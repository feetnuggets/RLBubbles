package com.rlbubbles.api;

import com.rlbubbles.common.dialogue.DialogueContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Registry of named custom conditions contributed by other mods via the API. */
public final class CustomConditions {
    private static final Map<String, Predicate<DialogueContext>> CONDITIONS = new ConcurrentHashMap<>();
    private CustomConditions() {}

    public static void register(String name, Predicate<DialogueContext> condition) {
        CONDITIONS.put(name, condition);
    }

    public static boolean test(String name, DialogueContext ctx) {
        Predicate<DialogueContext> c = CONDITIONS.get(name);
        return c == null || c.test(ctx); // unknown condition -> treated as satisfied
    }

    public static boolean has(String name) { return CONDITIONS.containsKey(name); }
}
