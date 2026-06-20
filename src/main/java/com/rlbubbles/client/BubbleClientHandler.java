package com.rlbubbles.client;

import com.rlbubbles.common.config.RLBubblesConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only store of bubbles currently being shown, keyed by entity id. Each bubble tracks its
 * own age so the renderer can compute fade-in / hold / fade-out alpha and a slight upward drift.
 *
 * This is intentionally tiny and allocation-light: a concurrent map of entity id -> small list,
 * advanced once per client tick.
 */
public final class BubbleClientHandler {

    public static final class Bubble {
        public final String text;
        public final int fadeIn;
        public final int hold;
        public final int fadeOut;
        public int age;

        Bubble(String text, int fadeIn, int hold, int fadeOut) {
            this.text = text;
            this.fadeIn = fadeIn;
            this.hold = hold;
            this.fadeOut = fadeOut;
        }

        public int total() { return fadeIn + hold + fadeOut; }
        public boolean expired() { return age >= total(); }

        /** 0..1 alpha for the current age (with partialTick for smoothness). */
        public float alpha(float partial) {
            float a = age + partial;
            if (a < fadeIn) return fadeIn <= 0 ? 1f : a / fadeIn;
            if (a < fadeIn + hold) return 1f;
            float fo = a - (fadeIn + hold);
            return fadeOut <= 0 ? 0f : Math.max(0f, 1f - fo / fadeOut);
        }

        /** Upward drift in blocks for the current age. */
        public float rise(float partial) {
            float a = age + partial;
            return 0.25f * (a / Math.max(1, total())); // up to a quarter-block over its life
        }
    }

    private static final Map<Integer, List<Bubble>> ACTIVE = new ConcurrentHashMap<>();

    private BubbleClientHandler() {}

    /** Called from the network handler when a ShowBubblePacket arrives. */
    public static void acceptBubble(int entityId, String text, int displayTicks) {
        int fadeIn = RLBubblesConfig.fadeInTicks();
        int fadeOut = RLBubblesConfig.fadeOutTicks();
        int max = RLBubblesConfig.maxMessagesPerEntity();

        List<Bubble> list = ACTIVE.computeIfAbsent(entityId, k -> new ArrayList<>());
        synchronized (list) {
            list.add(new Bubble(text, fadeIn, displayTicks, fadeOut));
            // Keep only the newest `max` messages for this entity.
            while (list.size() > max) list.remove(0);
        }
    }

    /** Advance all bubble ages once per client tick; drop expired ones. */
    public static void tick() {
        for (Iterator<Map.Entry<Integer, List<Bubble>>> it = ACTIVE.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, List<Bubble>> e = it.next();
            List<Bubble> list = e.getValue();
            synchronized (list) {
                list.removeIf(bubble -> { bubble.age++; return bubble.expired(); });
            }
            if (list.isEmpty()) it.remove();
        }
    }

    /** Snapshot the active bubbles for an entity (renderer reads this). */
    public static List<Bubble> get(int entityId) {
        return ACTIVE.get(entityId);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }
}
