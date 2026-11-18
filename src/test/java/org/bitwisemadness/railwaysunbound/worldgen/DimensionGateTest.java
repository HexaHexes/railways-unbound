package org.bitwisemadness.railwaysunbound.worldgen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Dimension gate policy, pure JVM: every mode's membership rules.
 */
class DimensionGateTest {

    @Test
    void overworldOnlyAllowsJustTheOverworld() {
        DimensionGate gate = DimensionGate.of(DimensionGate.Mode.OVERWORLD_ONLY, List.of());
        assertTrue(gate.allows("minecraft:overworld"));
        assertFalse(gate.allows("minecraft:the_nether"));
    }

    @Test
    void whitelistAllowsOnlyListedDimensions() {
        DimensionGate gate = DimensionGate.of(DimensionGate.Mode.WHITELIST,
                List.of("minecraft:overworld", "minecraft:the_moon"));
        assertTrue(gate.allows("minecraft:overworld"));
        assertTrue(gate.allows("minecraft:the_moon"));
        assertFalse(gate.allows("minecraft:the_nether"));
    }

    @Test
    void emptyWhitelistDeniesEverything() {
        DimensionGate gate = DimensionGate.of(DimensionGate.Mode.WHITELIST, List.of());
        assertFalse(gate.allows("minecraft:overworld"));
        assertFalse(gate.allows("minecraft:the_nether"));
    }

    @Test
    void blacklistDeniesOnlyListedDimensions() {
        DimensionGate gate = DimensionGate.of(DimensionGate.Mode.BLACKLIST,
                List.of("minecraft:overworld"));
        assertFalse(gate.allows("minecraft:overworld"));
        assertTrue(gate.allows("minecraft:the_nether"));
    }

    @Test
    void entriesMatchCaseInsensitively() {
        DimensionGate gate = DimensionGate.of(DimensionGate.Mode.WHITELIST,
                List.of("Example:Dim"));
        assertTrue(gate.allows("example:dim"));
        assertFalse(gate.allows("example:other"));
    }
}
