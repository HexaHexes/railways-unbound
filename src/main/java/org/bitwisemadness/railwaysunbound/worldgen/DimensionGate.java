package org.bitwisemadness.railwaysunbound.worldgen;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Pure dimension policy: may Railways Untold bootstrap in a dimension?
 *
 * <p>Plain strings on purpose: the whole class is unit-testable without a
 * Minecraft runtime, and callers pass the level's already-resolved id.</p>
 */
public record DimensionGate(Mode mode, Set<String> dimensions) {

    /** How the configured list treats the dimensions it names. */
    public enum Mode {
        OVERWORLD_ONLY,
        WHITELIST,
        BLACKLIST
    }

    /** The one dimension Railways Untold bootstraps in without any addon. */
    public static final String OVERWORLD = "minecraft:overworld";

    public DimensionGate {
        Set<String> normalized = new HashSet<>();
        for (String id : dimensions) {
            normalized.add(id.toLowerCase(Locale.ROOT));
        }
        dimensions = Set.copyOf(normalized);
    }

    /**
     * Builds a gate from raw config strings.
     *
     * @param mode how the list is interpreted
     * @param raw configured dimension ids
     * @return the normalized gate
     */
    public static DimensionGate of(Mode mode, List<? extends String> raw) {
        return new DimensionGate(mode, new HashSet<>(raw));
    }

    /** True when generation is allowed in the dimension. */
    public boolean allows(String dimensionId) {
        return switch (mode) {
            case OVERWORLD_ONLY -> OVERWORLD.equals(dimensionId);
            case WHITELIST -> dimensions.contains(dimensionId);
            case BLACKLIST -> !dimensions.contains(dimensionId);
        };
    }
}
