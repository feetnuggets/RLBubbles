package com.rlbubbles.api;

import com.rlbubbles.common.dialogue.DialogueContext;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry of dynamic entity dialogue providers contributed via the API. */
public final class EntityProviders {
    private static final List<RLBubblesAPI.EntityDialogueProvider> PROVIDERS = new CopyOnWriteArrayList<>();
    private EntityProviders() {}

    public static void register(RLBubblesAPI.EntityDialogueProvider provider) {
        PROVIDERS.add(provider);
    }

    /** First non-null provider result wins, or null if none supply a line. */
    public static String query(LivingEntity entity, DialogueContext ctx) {
        for (RLBubblesAPI.EntityDialogueProvider p : PROVIDERS) {
            String line = p.provide(entity, ctx);
            if (line != null && !line.isEmpty()) return line;
        }
        return null;
    }

    public static boolean any() { return !PROVIDERS.isEmpty(); }
}
