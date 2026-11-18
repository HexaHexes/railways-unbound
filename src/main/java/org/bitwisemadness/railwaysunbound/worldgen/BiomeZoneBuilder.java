package org.bitwisemadness.railwaysunbound.worldgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Merges blocked chunk coordinates into axis-aligned boxes.
 *
 * <p>One box per horizontal run of adjacent blocked chunks keeps the zone
 * list short enough that Railways Untold's per-segment containment scan stays
 * cheap even when whole biome regions are blocked.</p>
 */
public final class BiomeZoneBuilder {

    /** A merged box in chunk coordinates; max corners are exclusive. */
    public record ChunkBox(int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
    }

    private BiomeZoneBuilder() {
    }

    /** Packs a chunk coordinate pair into one set-friendly key. */
    public static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Merges the blocked chunks into boxes, one per run of consecutive
     * chunk-x coordinates sharing a chunk-z row.
     *
     * @param blockedChunks packed chunk keys
     * @return the merged boxes
     */
    public static List<ChunkBox> mergeRuns(Set<Long> blockedChunks) {
        Map<Integer, TreeSet<Integer>> rows = new HashMap<>();
        for (long packed : blockedChunks) {
            int chunkX = (int) (packed >> 32);
            int chunkZ = (int) packed;
            rows.computeIfAbsent(chunkZ, key -> new TreeSet<>()).add(chunkX);
        }
        List<ChunkBox> boxes = new ArrayList<>();
        for (Map.Entry<Integer, TreeSet<Integer>> row : rows.entrySet()) {
            appendRuns(row.getKey(), row.getValue(), boxes);
        }
        return boxes;
    }

    /** Emits one box per maximal run of consecutive x coordinates. */
    private static void appendRuns(int chunkZ, TreeSet<Integer> xs, List<ChunkBox> boxes) {
        int runStart = 0;
        int runEndExclusive = 0;
        boolean open = false;
        for (int chunkX : xs) {
            if (!open || chunkX != runEndExclusive) {
                if (open) {
                    boxes.add(new ChunkBox(runStart, chunkZ, runEndExclusive, chunkZ + 1));
                }
                runStart = chunkX;
                open = true;
            }
            runEndExclusive = chunkX + 1;
        }
        if (open) {
            boxes.add(new ChunkBox(runStart, chunkZ, runEndExclusive, chunkZ + 1));
        }
    }
}
