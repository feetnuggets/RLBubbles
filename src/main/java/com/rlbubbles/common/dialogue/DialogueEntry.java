package com.rlbubbles.common.dialogue;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A single data-driven dialogue line, loaded from JSON in data/<namespace>/dialogue/*.json.
 *
 * JSON shape (all condition fields optional):
 * {
 *   "entity": "minecraft:villager",      // entity id, or "*"/"any" / a #tag-like wildcard, or omitted = any
 *   "weight": 10,                         // selection weight (default 1)
 *   "rare": false,                        // only eligible when rare dialogue is enabled & rolls through
 *   "conditions": {
 *     "time": "day|night|sunrise|sunset",
 *     "weather": "clear|rain|thunder",
 *     "health": "healthy|injured|critical",
 *     "biome": "minecraft:desert",        // exact biome id
 *     "nearby": ["dragon","creeper","boss","hostile"]
 *   },
 *   "text": "The crops look healthy today."   // or "texts": ["a","b"] to bundle several lines
 * }
 */
public final class DialogueEntry {

    public final ResourceLocation entityMatch;   // null = matches any entity
    public final int weight;
    public final boolean rare;
    public final List<String> texts;             // one entry may carry several interchangeable lines

    // Parsed conditions (null = unconstrained)
    public final DialogueContext.TimeOfDay time;
    public final DialogueContext.Weather weather;
    public final DialogueContext.HealthState health;
    public final ResourceLocation biome;
    public final List<String> nearby;            // tokens: dragon/creeper/boss/hostile

    public DialogueEntry(ResourceLocation entityMatch, int weight, boolean rare, List<String> texts,
                         DialogueContext.TimeOfDay time, DialogueContext.Weather weather,
                         DialogueContext.HealthState health, ResourceLocation biome, List<String> nearby) {
        this.entityMatch = entityMatch;
        this.weight = Math.max(1, weight);
        this.rare = rare;
        this.texts = texts;
        this.time = time;
        this.weather = weather;
        this.health = health;
        this.biome = biome;
        this.nearby = nearby;
    }

    /** True if this entry's conditions are all satisfied by the given context. */
    public boolean matches(DialogueContext ctx) {
        if (entityMatch != null && !entityMatch.equals(ctx.entityId)) return false;
        if (time != null && time != ctx.timeOfDay) return false;
        if (weather != null && weather != ctx.weather) return false;
        if (health != null && health != ctx.healthState) return false;
        if (biome != null && !biome.equals(ctx.biomeId)) return false;
        if (nearby != null) {
            for (String token : nearby) {
                switch (token) {
                    case "dragon":  if (!ctx.nearbyDragon)  return false; break;
                    case "creeper": if (!ctx.nearbyCreeper) return false; break;
                    case "boss":    if (!ctx.nearbyBoss)    return false; break;
                    case "hostile": if (!ctx.nearbyHostile) return false; break;
                    default: break;
                }
            }
        }
        return true;
    }

    /** Pick one of this entry's texts at random (entries usually have exactly one). */
    public String pickText(java.util.Random rng) {
        if (texts.isEmpty()) return "";
        return texts.get(rng.nextInt(texts.size()));
    }

    // ---- JSON parsing ----

    public static DialogueEntry fromJson(JsonObject o) {
        ResourceLocation entity = null;
        if (o.has("entity")) {
            String e = GsonHelper.getAsString(o, "entity");
            if (!e.equals("*") && !e.equalsIgnoreCase("any")) {
                entity = new ResourceLocation(e);
            }
        }
        int weight = GsonHelper.getAsInt(o, "weight", 1);
        boolean rare = GsonHelper.getAsBoolean(o, "rare", false);

        List<String> texts = new ArrayList<>();
        if (o.has("texts")) {
            o.getAsJsonArray("texts").forEach(el -> texts.add(el.getAsString()));
        } else if (o.has("text")) {
            texts.add(GsonHelper.getAsString(o, "text"));
        }

        DialogueContext.TimeOfDay time = null;
        DialogueContext.Weather weather = null;
        DialogueContext.HealthState health = null;
        ResourceLocation biome = null;
        List<String> nearby = null;

        if (o.has("conditions")) {
            JsonObject c = GsonHelper.getAsJsonObject(o, "conditions");
            if (c.has("time"))    time    = parseEnum(DialogueContext.TimeOfDay.class, GsonHelper.getAsString(c, "time"));
            if (c.has("weather")) weather = parseEnum(DialogueContext.Weather.class, GsonHelper.getAsString(c, "weather"));
            if (c.has("health"))  health  = parseEnum(DialogueContext.HealthState.class, GsonHelper.getAsString(c, "health"));
            if (c.has("biome"))   biome   = new ResourceLocation(GsonHelper.getAsString(c, "biome"));
            if (c.has("nearby")) {
                nearby = new ArrayList<>();
                final List<String> n = nearby;
                c.getAsJsonArray("nearby").forEach(el -> n.add(el.getAsString().toLowerCase(Locale.ROOT)));
            }
        }
        return new DialogueEntry(entity, weight, rare, texts, time, weather, health, biome, nearby);
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> cls, String raw) {
        try {
            return Enum.valueOf(cls, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null; // unknown value -> treat as unconstrained rather than crash the pack
        }
    }
}
