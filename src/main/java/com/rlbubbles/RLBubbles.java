package com.rlbubbles;

import com.rlbubbles.client.BubbleClientHandler;
import com.rlbubbles.common.config.RLBubblesConfig;
import com.rlbubbles.common.dialogue.DialogueLoader;
import com.rlbubbles.common.dialogue.DialogueManager;
import com.rlbubbles.common.dialogue.DialogueRegistry;
import com.rlbubbles.common.network.RLBubblesNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RLBubbles.MOD_ID)
public class RLBubbles {

    public static final String MOD_ID = "rlbubbles";
    public static final Logger LOGGER = LoggerFactory.getLogger("RLBubbles");

    private static final DialogueRegistry REGISTRY = new DialogueRegistry();
    private static DialogueManager MANAGER;

    public RLBubbles() {
        var modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RLBubblesConfig.SPEC);

        MANAGER = new DialogueManager(REGISTRY, RLBubblesNetwork::broadcast);

        // Forge event bus: reload listener + server tick.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(RLBubblesNetwork::register);
        LOGGER.info("[RLBubbles] Common setup complete.");
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        if (RLBubblesConfig.enableDatapacks()) {
            event.addListener(new DialogueLoader(REGISTRY));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.level instanceof ServerLevel server) {
            MANAGER.onServerTick(server);
        }
    }

    public static DialogueRegistry getRegistry() { return REGISTRY; }

    /** Client tick driver (advances bubble fade timers). Registered on the client event bus. */
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                BubbleClientHandler.tick();
            }
        }
    }
}
