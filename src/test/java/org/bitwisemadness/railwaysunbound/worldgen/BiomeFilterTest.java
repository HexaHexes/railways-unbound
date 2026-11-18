package org.bitwisemadness.railwaysunbound.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Biome filter policy, pure JVM: entry parsing and every mode's blocking
 * rule, with tag membership injected as a fake probe.
 */
class BiomeFilterTest {

    @Test
    void parseSplitsTagsFromDirectIds() {
        BiomeFilter.Entry tagged = BiomeFilter.parse("#minecraft:is_ocean");
        assertTrue(tagged.tag());
        assertEquals("minecraft:is_ocean", tagged.id());

        BiomeFilter.Entry direct = BiomeFilter.parse("Minecraft:Deep_Ocean");
        assertFalse(direct.tag());
        assertEquals("minecraft:deep_ocean", direct.id());
    }

    @Test
    void entriesWithoutNamespaceParseToNull() {
        assertNull(BiomeFilter.parse("deep_ocean"));
        assertNull(BiomeFilter.parse("#"));
        assertNull(BiomeFilter.parse(""));
    }

    @Test
    void blacklistBlocksListedBiomesOnly() {
        BiomeFilter filter = BiomeFilter.of(BiomeFilter.Mode.BLACKLIST,
                List.of("minecraft:deep_ocean", "#minecraft:is_ocean"), 4);
        assertTrue(filter.blocks("minecraft:deep_ocean", tag -> false));
        assertTrue(filter.blocks("minecraft:frozen_ocean", "minecraft:is_ocean"::equals));
        assertFalse(filter.blocks("minecraft:plains", tag -> false));
    }

    @Test
    void whitelistBlocksEverythingUnlisted() {
        BiomeFilter filter = BiomeFilter.of(BiomeFilter.Mode.WHITELIST,
                List.of("#minecraft:is_forest"), 2);
        assertFalse(filter.blocks("minecraft:forest", "minecraft:is_forest"::equals));
        assertTrue(filter.blocks("minecraft:forest", tag -> false));
        assertTrue(filter.blocks("minecraft:plains", tag -> false));
    }

    @Test
    void noneBlocksNothingEvenWhenListed() {
        BiomeFilter filter = BiomeFilter.of(BiomeFilter.Mode.NONE,
                List.of("minecraft:plains"), 4);
        assertFalse(filter.blocks("minecraft:plains", tag -> true));
    }

    @Test
    void invalidEntriesAreSkippedAtBuildTime() {
        BiomeFilter filter = BiomeFilter.of(BiomeFilter.Mode.BLACKLIST,
                List.of("minecraft:plains", "no_namespace"), 4);
        assertTrue(filter.blocks("minecraft:plains", tag -> false));
        assertFalse(filter.blocks("no_namespace", tag -> false));
    }
}
