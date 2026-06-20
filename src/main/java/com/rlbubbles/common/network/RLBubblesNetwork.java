package com.rlbubbles.common.network;

import com.rlbubbles.RLBubbles;
import com.rlbubbles.common.config.RLBubblesConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Owns the SimpleChannel and provides the broadcast entry point used by the DialogueManager.
 * The server picks the line; clients render it. We send to all players tracking the speaking
 * entity's chunk so every nearby player sees the same message (singleplayer, LAN, dedicated).
 */
public final class RLBubblesNetwork {

    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    private RLBubblesNetwork() {}

    public static void register() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(RLBubbles.MOD_ID, "main"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();

        int id = 0;
        CHANNEL.registerMessage(id++, ShowBubblePacket.class,
                ShowBubblePacket::encode, ShowBubblePacket::decode, ShowBubblePacket::handle);
    }

    /** Broadcast a bubble for a speaking entity to all players tracking it. */
    public static void broadcast(ServerLevel level, LivingEntity speaker, String text) {
        ShowBubblePacket pkt = new ShowBubblePacket(
                speaker.getId(), text, RLBubblesConfig.displayTicks());
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> speaker), pkt);
    }
}
