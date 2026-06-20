package com.rlbubbles.common.dialogue;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Holds all loaded dialogue, indexed by entity id for fast lookup, plus a "wildcard" pool of
 * entries that match any entity. Selection is weighted and condition-filtered. Programmatic
 * entries added via the API are kept separately so a datapack /reload doesn't wipe them.
 *
 * Performance: the per-entity index means scanning is O(entries-for-this-entity), not O(all),
 * and the pools are plain ArrayLists cached until the next reload.
 */
public class DialogueRegistry {

    private final Map<ResourceLocation, List<DialogueEntry>> byEntity = new HashMap<>();
    private final List<DialogueEntry> wildcard = new ArrayList<>();

    // API-registered entries survive datapack reloads.
    private final List<DialogueEntry> apiEntries = new ArrayList<>();

    /** Replace all datapack-loaded entries (called by the loader on /reload). */
    public synchronized void rebuild(List<DialogueEntry> entries) {
        byEntity.clear();
        wildcard.clear();
        for (DialogueEntry e : entries) index(e);
        for (DialogueEntry e : apiEntries) index(e);
    }

    /** Add a programmatic entry (API). Persists across reloads. */
    public synchronized void addApiEntry(DialogueEntry e) {
        apiEntries.add(e);
        index(e);
    }

    private void index(DialogueEntry e) {
        if (e.entityMatch == null) {
            wildcard.add(e);
        } else {
            byEntity.computeIfAbsent(e.entityMatch, k -> new ArrayList<>()).add(e);
        }
    }

    /**
     * Choose a line for the given context, or null if nothing matches.
     * @param allowRare whether rare entries are eligible (caller gates this on config + a roll)
     */
    public synchronized String pick(DialogueContext ctx, boolean allowRare, Random rng) {
        List<DialogueEntry> candidates = new ArrayList<>();
        List<DialogueEntry> specific = byEntity.get(ctx.entityId);
        if (specific != null) collectMatching(specific, ctx, allowRare, candidates);
        collectMatching(wildcard, ctx, allowRare, candidates);

        if (candidates.isEmpty()) return null;

        int total = 0;
        for (DialogueEntry e : candidates) total += e.weight;
        int roll = rng.nextInt(total);
        for (DialogueEntry e : candidates) {
            roll -= e.weight;
            if (roll < 0) return e.pickText(rng);
        }
        return candidates.get(candidates.size() - 1).pickText(rng);
    }

    private void collectMatching(List<DialogueEntry> src, DialogueContext ctx, boolean allowRare,
                                 List<DialogueEntry> out) {
        for (DialogueEntry e : src) {
            if (e.rare && !allowRare) continue;
            if (e.matches(ctx)) out.add(e);
        }
    }

    /** True if any entry (specific or wildcard) could ever apply to this entity id. */
    public synchronized boolean hasAnyFor(ResourceLocation entityId) {
        return !wildcard.isEmpty() || byEntity.containsKey(entityId);
    }

    public synchronized int size() {
        int n = wildcard.size();
        for (List<DialogueEntry> l : byEntity.values()) n += l.size();
        return n;
    }
}
