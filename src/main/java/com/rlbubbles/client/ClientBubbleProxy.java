package com.rlbubbles.client;

/**
 * Thin client-only indirection used by the network packet handler. Keeping the actual call to
 * BubbleClientHandler behind this class (invoked via DistExecutor) ensures the dedicated server
 * never classloads client rendering code.
 */
public final class ClientBubbleProxy {
    private ClientBubbleProxy() {}

    public static void show(int entityId, String text, int displayTicks) {
        BubbleClientHandler.acceptBubble(entityId, text, displayTicks);
    }
}
