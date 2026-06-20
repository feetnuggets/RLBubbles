package com.rlbubbles.api;

import com.rlbubbles.RLBubbles;
import com.rlbubbles.common.dialogue.DialogueContext;
import com.rlbubbles.common.dialogue.DialogueEntry;
import com.rlbubbles.common.network.RLBubblesNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Public, stable entry point for other mods. Lets them add dialogue, trigger a message on a
 * specific entity right now, and register custom conditions / entity providers.
 *
 * All methods are safe to call from common code; showMessage must be called server-side
 * (it broadcasts over the network to clients).
 */
public final class RLBubblesAPI {

    private RLBubblesAPI() {}

    /**
     * Register a dialogue line programmatically. Survives datapack /reload.
     *
     * @param entityId entity to attach to, or null for "any entity"
     * @param text     the line (supports the same formatting/gradient syntax as JSON dialogue)
     * @param weight   selection weight (>=1)
     * @param rare     whether this is a rare line
     */
    public static void registerDialogue(ResourceLocation entityId, String text, int weight, boolean rare) {
        List<String> texts = new ArrayList<>();
        texts.add(text);
        DialogueEntry entry = new DialogueEntry(entityId, weight, rare, texts,
                null, null, null, null, null);
        RLBubbles.getRegistry().addApiEntry(entry);
    }

    /** Register a fully-built dialogue entry (for advanced conditions). */
    public static void registerDialogue(DialogueEntry entry) {
        RLBubbles.getRegistry().addApiEntry(entry);
    }

    /**
     * Immediately show a message above a specific entity (server-side only). Bypasses condition
     * matching -- use this for scripted events.
     */
    public static void showMessage(LivingEntity entity, String text) {
        if (entity.level() instanceof ServerLevel server) {
            RLBubblesNetwork.broadcast(server, entity, text);
        }
    }

    /**
     * Register a named custom condition that dialogue packs can reference. Custom conditions get
     * the live DialogueContext and return whether they're satisfied.
     */
    public static void registerCondition(String name, Predicate<DialogueContext> condition) {
        CustomConditions.register(name, condition);
    }

    /**
     * Register an entity provider: a hook that can supply dialogue dynamically for entities at
     * selection time (e.g. an NPC mod that wants per-NPC lines). Providers run after static
     * dialogue and may return null to defer.
     */
    public static void registerEntityProvider(EntityDialogueProvider provider) {
        EntityProviders.register(provider);
    }

    /** Functional hook for dynamic per-entity dialogue. */
    @FunctionalInterface
    public interface EntityDialogueProvider {
        /** @return a line to show, or null to let the normal system handle it. */
        String provide(LivingEntity entity, DialogueContext context);
    }
}
