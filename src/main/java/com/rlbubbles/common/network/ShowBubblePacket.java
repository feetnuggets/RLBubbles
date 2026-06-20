package com.rlbubbles.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> client: "entity #id should show this text". Kept tiny (an int entity id + a short
 * string) so broadcasting to many players over many entities stays cheap on the wire. The text is
 * sent as a raw string and parsed client-side so formatting/gradient codes travel compactly.
 */
public class ShowBubblePacket {

    private final int entityId;
    private final String text;
    private final int displayTicks;

    public ShowBubblePacket(int entityId, String text, int displayTicks) {
        this.entityId = entityId;
        this.text = text;
        this.displayTicks = displayTicks;
    }

    public static void encode(ShowBubblePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.text, 512);
        buf.writeVarInt(msg.displayTicks);
    }

    public static ShowBubblePacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String text = buf.readUtf(512);
        int ticks = buf.readVarInt();
        return new ShowBubblePacket(id, text, ticks);
    }

    public static void handle(ShowBubblePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            // Only the physical client has the renderer/handler; guard with DistExecutor so the
            // dedicated server never classloads client code.
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.rlbubbles.client.ClientBubbleProxy.show(msg.entityId, msg.text, msg.displayTicks))
        );
        ctx.get().setPacketHandled(true);
    }

    public int entityId() { return entityId; }
    public String text() { return text; }
    public int displayTicks() { return displayTicks; }
}
