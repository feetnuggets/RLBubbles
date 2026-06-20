package com.rlbubbles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rlbubbles.RLBubbles;
import com.rlbubbles.common.config.RLBubblesConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Draws floating TEXT ONLY above entities -- no bubble, no background, no border (unless the user
 * explicitly turns those on in config, which defaults to off per the design).
 *
 * Rendering approach:
 *  - Hooks RenderLevelStageEvent.AFTER_PARTICLES so we draw in world space with the camera
 *    transform available.
 *  - For each entity with active bubbles, translate to its head, billboard toward the camera,
 *    scale by a fixed factor (the text naturally shrinks with distance because it's in world
 *    space), and draw with computed fade alpha + slight upward drift.
 *  - Stacks multiple messages vertically if maxMessagesPerEntity > 1.
 */
@Mod.EventBusSubscriber(modid = RLBubbles.MOD_ID, value = Dist.CLIENT)
public final class BubbleRenderer {

    private static final float BASE_SCALE = 0.025f;
    private static final float LINE_GAP = 0.28f;     // world-space gap between stacked messages
    private static final float HEAD_PADDING = 0.55f; // above the entity's top

    private BubbleRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();
        float partial = event.getPartialTick();
        Font font = mc.font;
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        double maxDistSq = RLBubblesConfig.maxDistance() * (double) RLBubblesConfig.maxDistance();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            List<BubbleClientHandler.Bubble> bubbles = BubbleClientHandler.get(entity.getId());
            if (bubbles == null || bubbles.isEmpty()) continue;

            double dx = entity.getX() - camPos.x;
            double dy = entity.getY() - camPos.y;
            double dz = entity.getZ() - camPos.z;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;

            // Interpolated entity position for smooth motion.
            double ex = net.minecraft.util.Mth.lerp(partial, entity.xOld, entity.getX());
            double ey = net.minecraft.util.Mth.lerp(partial, entity.yOld, entity.getY());
            double ez = net.minecraft.util.Mth.lerp(partial, entity.zOld, entity.getZ());
            double headY = ey + living.getBbHeight() + HEAD_PADDING;

            // Render newest message on top; stack older ones beneath.
            synchronized (bubbles) {
                int n = bubbles.size();
                for (int i = 0; i < n; i++) {
                    BubbleClientHandler.Bubble bubble = bubbles.get(i);
                    float rise = bubble.rise(partial);
                    float yOff = (n - 1 - i) * LINE_GAP + rise;
                    drawText(pose, buffers, font, camera,
                            ex - camPos.x, headY - camPos.y + yOff, ez - camPos.z,
                            bubble.text, bubble.alpha(partial));
                }
            }
        }
        buffers.endBatch();
    }

    private static void drawText(PoseStack pose, MultiBufferSource buffers, Font font, Camera camera,
                                 double x, double y, double z, String raw, float alpha) {
        if (alpha <= 0.01f) return;
        Component text = BubbleTextFormatter.format(raw);

        pose.pushPose();
        pose.translate(x, y, z);
        // Billboard: face the camera using its rotation.
        pose.mulPose(camera.rotation());
        pose.scale(-BASE_SCALE, -BASE_SCALE, BASE_SCALE);

        float width = font.width(text);
        float xPos = -width / 2f;

        int alphaBits = (int) (alpha * 255.0f) << 24;
        boolean shadow = RLBubblesConfig.showShadow();

        // Optional background panel only if the user explicitly enabled it (default off).
        // 0 background color = no panel (text only).
        int bgColor = RLBubblesConfig.showBackground() ? (0x40 << 24) : 0;

        var matrix = pose.last().pose();
        font.drawInBatch(text, xPos, 0f, 0xFFFFFF | alphaBits, shadow, matrix, buffers,
                bgColor == 0 ? Font.DisplayMode.NORMAL : Font.DisplayMode.SEE_THROUGH,
                bgColor, 0xF000F0);

        pose.popPose();
    }
}
