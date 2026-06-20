package com.rlbubbles.common.dialogue;

import com.rlbubbles.common.config.RLBubblesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

/**
 * Server-side brain: every so often, walks a slice of living entities, decides whether each
 * should speak, builds a DialogueContext, picks a line, and hands it to the networking layer to
 * broadcast to nearby players.
 *
 * Performance design:
 *  - Runs on an interval (BASE_INTERVAL ticks), not every tick.
 *  - Per tick it only considers entities within maxDistance of some player (it iterates players,
 *    not the whole entity list), so cost scales with players * local entities, not world size.
 *  - Each candidate entity passes a cheap random gate before any context/scan work happens.
 *  - The threat scan uses a single getEntitiesOfClass call with a small AABB.
 *  - No AI, pathfinding, or entity data is modified -- this is purely observational.
 */
public final class DialogueManager {

    private static final int BASE_INTERVAL = 40; // ticks between speak passes (2s)
    private final DialogueRegistry registry;
    private final Random rng = new Random();
    private final DialogueBroadcaster broadcaster;
    private int tickCounter = 0;

    public interface DialogueBroadcaster {
        void broadcast(ServerLevel level, LivingEntity speaker, String text);
    }

    public DialogueManager(DialogueRegistry registry, DialogueBroadcaster broadcaster) {
        this.registry = registry;
        this.broadcaster = broadcaster;
    }

    public void onServerTick(ServerLevel level) {
        if (++tickCounter < BASE_INTERVAL) return;
        tickCounter = 0;

        double maxDist = RLBubblesConfig.maxDistance();
        double freq = RLBubblesConfig.speechFrequency();
        if (freq <= 0) return;

        // Iterate near players so we never touch entities no one can see.
        for (var player : level.players()) {
            AABB area = player.getBoundingBox().inflate(maxDist);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player));
            for (LivingEntity entity : nearby) {
                // Cheap gate first: probability per pass, scaled by config frequency.
                // ~ chance that a given entity speaks on a given pass.
                if (rng.nextDouble() > 0.02 * freq) continue;
                trySpeak(level, entity);
            }
        }
    }

    private void trySpeak(ServerLevel level, LivingEntity entity) {
        if (!categoryEnabled(entity)) return;

        ResourceLocation id = EntityType.getKey(entity.getType());
        if (!registry.hasAnyFor(id)) {
            // No specific or wildcard dialogue applies; with fallback enabled the wildcard pool
            // (generic "*looks around*" lines) handles this, so this only short-circuits when the
            // registry is truly empty for this entity.
            return;
        }

        DialogueContext ctx = buildContext(level, entity, id);

        // Let API-registered providers offer a line first (dynamic per-entity dialogue).
        String line = com.rlbubbles.api.EntityProviders.any()
                ? com.rlbubbles.api.EntityProviders.query(entity, ctx) : null;
        if (line == null) {
            boolean allowRare = RLBubblesConfig.enableRareDialogue() && rng.nextFloat() < 0.10f;
            line = registry.pick(ctx, allowRare, rng);
        }
        if (line == null || line.isEmpty()) return;

        broadcaster.broadcast(level, entity, line);
    }

    private boolean categoryEnabled(LivingEntity e) {
        if (e instanceof Villager) return RLBubblesConfig.enableVillagers();
        if (isBoss(e))             return RLBubblesConfig.enableBosses();
        if (e instanceof Enemy)    return RLBubblesConfig.enableHostileMobs();
        if (e instanceof Animal)   return RLBubblesConfig.enablePassiveMobs();
        // Modded / uncategorized living entities ride the modded toggle.
        return RLBubblesConfig.enableModdedMobs();
    }

    private boolean isBoss(LivingEntity e) {
        return e instanceof EnderDragon || e instanceof WitherBoss
                || e.getType().getCategory() == MobCategory.MONSTER && e.getMaxHealth() >= 150.0f;
    }

    private DialogueContext buildContext(ServerLevel level, LivingEntity entity, ResourceLocation id) {
        DialogueContext.Builder b = DialogueContext.builder()
                .entity(entity).entityId(id).level(level);

        // Time of day
        long dayTime = level.getDayTime() % 24000L;
        DialogueContext.TimeOfDay tod;
        if (dayTime >= 23000 || dayTime < 1000) tod = DialogueContext.TimeOfDay.SUNRISE;
        else if (dayTime >= 12000 && dayTime < 13000) tod = DialogueContext.TimeOfDay.SUNSET;
        else if (dayTime >= 13000) tod = DialogueContext.TimeOfDay.NIGHT;
        else tod = DialogueContext.TimeOfDay.DAY;
        b.timeOfDay(tod);

        // Weather
        if (level.isThundering()) b.weather(DialogueContext.Weather.THUNDER);
        else if (level.isRaining()) b.weather(DialogueContext.Weather.RAIN);
        else b.weather(DialogueContext.Weather.CLEAR);

        // Health
        float frac = entity.getHealth() / Math.max(1.0f, entity.getMaxHealth());
        if (frac <= 0.25f) b.healthState(DialogueContext.HealthState.CRITICAL);
        else if (frac < 0.9f) b.healthState(DialogueContext.HealthState.INJURED);
        else b.healthState(DialogueContext.HealthState.HEALTHY);

        // Biome
        var biomeHolder = level.getBiome(entity.blockPosition());
        biomeHolder.unwrapKey().ifPresent(key -> b.biomeId(key.location()));

        // Nearby-threat scan (one small query, only if contextual dialogue is on)
        if (RLBubblesConfig.enableContextualDialogue()) {
            AABB scan = entity.getBoundingBox().inflate(12.0);
            List<LivingEntity> threats = level.getEntitiesOfClass(LivingEntity.class, scan,
                    t -> t != entity && t.isAlive());
            boolean dragon = false, creeper = false, boss = false, hostile = false;
            for (LivingEntity t : threats) {
                if (t instanceof EnderDragon) dragon = true;
                if (t instanceof Creeper) creeper = true;
                if (isBoss(t)) boss = true;
                if (t instanceof Enemy) hostile = true;
                if (dragon && creeper && boss && hostile) break;
            }
            b.nearbyDragon(dragon).nearbyCreeper(creeper).nearbyBoss(boss).nearbyHostile(hostile);
        }

        return b.build();
    }
}
