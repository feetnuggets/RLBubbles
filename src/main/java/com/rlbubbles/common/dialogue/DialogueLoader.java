package com.rlbubbles.common.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rlbubbles.RLBubbles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads dialogue entries from data/<namespace>/dialogue/*.json across all datapacks. Because it
 * extends SimpleJsonResourceReloadListener, dialogue is fully datapack-driven and reloads with
 * /reload -- no code changes needed to add or override dialogue.
 *
 * A single JSON file may contain either one entry object, or an array of entry objects, or an
 * object with an "entries" array, so packs can organize lines however they like.
 */
public class DialogueLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOLDER = "dialogue";

    private final DialogueRegistry registry;

    public DialogueLoader(DialogueRegistry registry) {
        super(GSON, FOLDER);
        this.registry = registry;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager mgr, ProfilerFiller profiler) {
        List<DialogueEntry> parsed = new ArrayList<>();
        int fileCount = 0, errorCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            fileCount++;
            try {
                JsonElement root = e.getValue();
                if (root.isJsonArray()) {
                    for (JsonElement el : root.getAsJsonArray()) {
                        parsed.add(DialogueEntry.fromJson(el.getAsJsonObject()));
                    }
                } else if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    if (obj.has("entries") && obj.get("entries").isJsonArray()) {
                        for (JsonElement el : obj.getAsJsonArray("entries")) {
                            parsed.add(DialogueEntry.fromJson(el.getAsJsonObject()));
                        }
                    } else {
                        parsed.add(DialogueEntry.fromJson(obj));
                    }
                }
            } catch (Exception ex) {
                errorCount++;
                RLBubbles.LOGGER.warn("[RLBubbles] Skipping malformed dialogue file {}: {}", e.getKey(), ex.getMessage());
            }
        }

        registry.rebuild(parsed);
        RLBubbles.LOGGER.info("[RLBubbles] Loaded {} dialogue entries from {} files ({} skipped).",
                parsed.size(), fileCount, errorCount);
    }
}
