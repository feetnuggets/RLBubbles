package com.rlbubbles.common.dialogue;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * An immutable snapshot of an entity's situation, computed server-side once when we decide whether
 * an entity should speak. DialogueCondition implementations read from this rather than touching the
 * world directly, which keeps condition checks cheap and side-effect free.
 */
public final class DialogueContext {

    public enum TimeOfDay { DAY, NIGHT, SUNRISE, SUNSET }
    public enum Weather { CLEAR, RAIN, THUNDER }
    public enum HealthState { HEALTHY, INJURED, CRITICAL }

    public final LivingEntity entity;
    public final ResourceLocation entityId;
    public final Level level;

    public final TimeOfDay timeOfDay;
    public final Weather weather;
    public final HealthState healthState;
    public final ResourceLocation biomeId;

    /** Threat flags computed from a nearby-entity scan (see DialogueManager). */
    public final boolean nearbyDragon;
    public final boolean nearbyCreeper;
    public final boolean nearbyBoss;
    public final boolean nearbyHostile;

    private DialogueContext(Builder b) {
        this.entity = b.entity;
        this.entityId = b.entityId;
        this.level = b.level;
        this.timeOfDay = b.timeOfDay;
        this.weather = b.weather;
        this.healthState = b.healthState;
        this.biomeId = b.biomeId;
        this.nearbyDragon = b.nearbyDragon;
        this.nearbyCreeper = b.nearbyCreeper;
        this.nearbyBoss = b.nearbyBoss;
        this.nearbyHostile = b.nearbyHostile;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private LivingEntity entity;
        private ResourceLocation entityId;
        private Level level;
        private TimeOfDay timeOfDay = TimeOfDay.DAY;
        private Weather weather = Weather.CLEAR;
        private HealthState healthState = HealthState.HEALTHY;
        private ResourceLocation biomeId;
        private boolean nearbyDragon, nearbyCreeper, nearbyBoss, nearbyHostile;

        public Builder entity(LivingEntity e) { this.entity = e; return this; }
        public Builder entityId(ResourceLocation id) { this.entityId = id; return this; }
        public Builder level(Level l) { this.level = l; return this; }
        public Builder timeOfDay(TimeOfDay t) { this.timeOfDay = t; return this; }
        public Builder weather(Weather w) { this.weather = w; return this; }
        public Builder healthState(HealthState h) { this.healthState = h; return this; }
        public Builder biomeId(ResourceLocation b) { this.biomeId = b; return this; }
        public Builder nearbyDragon(boolean v) { this.nearbyDragon = v; return this; }
        public Builder nearbyCreeper(boolean v) { this.nearbyCreeper = v; return this; }
        public Builder nearbyBoss(boolean v) { this.nearbyBoss = v; return this; }
        public Builder nearbyHostile(boolean v) { this.nearbyHostile = v; return this; }

        public DialogueContext build() { return new DialogueContext(this); }
    }
}
