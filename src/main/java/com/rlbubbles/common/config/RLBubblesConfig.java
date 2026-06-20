package com.rlbubbles.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * All tunables from the design spec. Exposed via static accessors so the rest of the codebase
 * (which is loader-agnostic) doesn't depend on the ForgeConfigSpec types directly -- the
 * NeoForge port swaps only this class's backing implementation.
 */
public final class RLBubblesConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE_VILLAGERS;
    private static final ForgeConfigSpec.BooleanValue ENABLE_PASSIVE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_HOSTILE;
    private static final ForgeConfigSpec.BooleanValue ENABLE_BOSSES;
    private static final ForgeConfigSpec.BooleanValue ENABLE_MODDED;

    private static final ForgeConfigSpec.DoubleValue SPEECH_FREQUENCY;
    private static final ForgeConfigSpec.IntValue MAX_DISTANCE;

    private static final ForgeConfigSpec.IntValue FADE_IN_TICKS;
    private static final ForgeConfigSpec.IntValue DISPLAY_TICKS;
    private static final ForgeConfigSpec.IntValue FADE_OUT_TICKS;

    private static final ForgeConfigSpec.BooleanValue SHOW_SHADOW;
    private static final ForgeConfigSpec.BooleanValue ENABLE_CONTEXTUAL;
    private static final ForgeConfigSpec.BooleanValue ENABLE_RARE;

    private static final ForgeConfigSpec.BooleanValue SHOW_BUBBLE;
    private static final ForgeConfigSpec.BooleanValue SHOW_BACKGROUND;

    private static final ForgeConfigSpec.BooleanValue ALLOW_GRADIENTS;
    private static final ForgeConfigSpec.BooleanValue ALLOW_FORMATTING;

    private static final ForgeConfigSpec.IntValue MAX_MESSAGES_PER_ENTITY;
    private static final ForgeConfigSpec.BooleanValue ENABLE_DATAPACKS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("entities");
        ENABLE_VILLAGERS = b.comment("Allow villagers to speak").define("enableVillagers", true);
        ENABLE_PASSIVE   = b.comment("Allow passive mobs to speak").define("enablePassiveMobs", true);
        ENABLE_HOSTILE   = b.comment("Allow hostile mobs to speak").define("enableHostileMobs", true);
        ENABLE_BOSSES    = b.comment("Allow bosses to speak").define("enableBosses", true);
        ENABLE_MODDED    = b.comment("Allow modded / uncategorized living entities to speak").define("enableModdedMobs", true);
        b.pop();

        b.push("frequency");
        SPEECH_FREQUENCY = b.comment("Global speech frequency multiplier (0 disables)").defineInRange("speechFrequency", 1.0, 0.0, 10.0);
        MAX_DISTANCE     = b.comment("Max distance (blocks) from a player for an entity to be considered").defineInRange("maxDistance", 48, 8, 128);
        b.pop();

        b.push("timing");
        FADE_IN_TICKS  = b.comment("Fade-in duration (ticks)").defineInRange("fadeInTicks", 10, 0, 200);
        DISPLAY_TICKS  = b.comment("Hold duration (ticks)").defineInRange("displayTicks", 80, 1, 600);
        FADE_OUT_TICKS = b.comment("Fade-out duration (ticks)").defineInRange("fadeOutTicks", 20, 0, 200);
        b.pop();

        b.push("rendering");
        SHOW_SHADOW      = b.comment("Render text drop shadow").define("showShadow", true);
        ENABLE_CONTEXTUAL= b.comment("Use contextual dialogue (time/weather/threats/biome)").define("enableContextualDialogue", true);
        ENABLE_RARE      = b.comment("Allow rare dialogue lines").define("enableRareDialogue", true);
        SHOW_BUBBLE      = b.comment("Render a speech bubble shape (spec default: false -- text only)").define("showBubble", false);
        SHOW_BACKGROUND  = b.comment("Render a text background panel (spec default: false -- text only)").define("showBackground", false);
        ALLOW_GRADIENTS  = b.comment("Allow gradient color codes in dialogue").define("allowGradients", true);
        ALLOW_FORMATTING = b.comment("Allow vanilla formatting codes in dialogue").define("allowFormatting", true);
        b.pop();

        b.push("behavior");
        MAX_MESSAGES_PER_ENTITY = b.comment("Max simultaneous messages per entity").defineInRange("maxMessagesPerEntity", 1, 1, 5);
        ENABLE_DATAPACKS        = b.comment("Load dialogue from datapacks").define("enableDatapacks", true);
        b.pop();

        SPEC = b.build();
    }

    private RLBubblesConfig() {}

    public static boolean enableVillagers()   { return ENABLE_VILLAGERS.get(); }
    public static boolean enablePassiveMobs()  { return ENABLE_PASSIVE.get(); }
    public static boolean enableHostileMobs()  { return ENABLE_HOSTILE.get(); }
    public static boolean enableBosses()       { return ENABLE_BOSSES.get(); }
    public static boolean enableModdedMobs()   { return ENABLE_MODDED.get(); }
    public static double  speechFrequency()    { return SPEECH_FREQUENCY.get(); }
    public static int     maxDistance()        { return MAX_DISTANCE.get(); }
    public static int     fadeInTicks()        { return FADE_IN_TICKS.get(); }
    public static int     displayTicks()       { return DISPLAY_TICKS.get(); }
    public static int     fadeOutTicks()       { return FADE_OUT_TICKS.get(); }
    public static boolean showShadow()         { return SHOW_SHADOW.get(); }
    public static boolean enableContextualDialogue() { return ENABLE_CONTEXTUAL.get(); }
    public static boolean enableRareDialogue() { return ENABLE_RARE.get(); }
    public static boolean showBubble()         { return SHOW_BUBBLE.get(); }
    public static boolean showBackground()     { return SHOW_BACKGROUND.get(); }
    public static boolean allowGradients()     { return ALLOW_GRADIENTS.get(); }
    public static boolean allowFormatting()    { return ALLOW_FORMATTING.get(); }
    public static int     maxMessagesPerEntity(){ return MAX_MESSAGES_PER_ENTITY.get(); }
    public static boolean enableDatapacks()    { return ENABLE_DATAPACKS.get(); }
}
