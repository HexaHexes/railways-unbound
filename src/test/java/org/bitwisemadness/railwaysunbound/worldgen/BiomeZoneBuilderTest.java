package org.bitwisemadness.railwaysunbound.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Zone run-merging, pure JVM: adjacent chunks collapse, separated chunks do
 * not, and rows never merge across chunk-z.
 */
class BiomeZoneBuilderTest {

    @Test
    void adjacentChunksMergeIntoOneRun() {
        Set<Long> blocked = new HashSet<>(List.of(
                BiomeZoneBuilder.pack(2, 5), BiomeZoneBuilder.pack(3, 5),
                BiomeZoneBuilder.pack(4, 5)));
        List<BiomeZoneBuilder.ChunkBox> boxes = BiomeZoneBuilder.mergeRuns(blocked);
        assertEquals(1, boxes.size());
        assertEquals(new BiomeZoneBuilder.ChunkBox(2, 5, 5, 6), boxes.get(0));
    }

    @Test
    void separatedChunksStaySeparate() {
        Set<Long> blocked = new HashSet<>(List.of(
                BiomeZoneBuilder.pack(2, 5), BiomeZoneBuilder.pack(7, 5)));
        List<BiomeZoneBuilder.ChunkBox> boxes = BiomeZoneBuilder.mergeRuns(blocked);
        assertEquals(List.of(new BiomeZoneBuilder.ChunkBox(2, 5, 3, 6),
                new BiomeZoneBuilder.ChunkBox(7, 5, 8, 6)), boxes);
    }

    @Test
    void rowsNeverMergeAcrossChunkZ() {
        Set<Long> blocked = new HashSet<>(List.of(
                BiomeZoneBuilder.pack(2, 5), BiomeZoneBuilder.pack(3, 6)));
        List<BiomeZoneBuilder.ChunkBox> boxes = BiomeZoneBuilder.mergeRuns(blocked);
        assertEquals(2, boxes.size());
    }

    @Test
    void singleChunkCoversItsOwnBox() {
        Set<Long> blocked = Set.of(BiomeZoneBuilder.pack(-3, -2));
        List<BiomeZoneBuilder.ChunkBox> boxes = BiomeZoneBuilder.mergeRuns(blocked);
        assertEquals(List.of(new BiomeZoneBuilder.ChunkBox(-3, -2, -2, -1)), boxes);
    }

    @Test
    void emptyInputYieldsNoBoxes() {
        assertEquals(List.of(), BiomeZoneBuilder.mergeRuns(Set.of()));
    }
}
