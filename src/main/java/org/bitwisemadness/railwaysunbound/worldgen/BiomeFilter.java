package org.bitwisemadness.railwaysunbound.worldgen;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Pure biome policy: must track placement route around a biome?
 *
 * <p>Entries are biome ids ({@code minecraft:deep_ocean}) or tag references
 * ({@code #minecraft:is_ocean}). Tag membership is injected as a predicate so
 * the class stays unit-testable; the accessor passes a probe backed by the
 * live biome Holder.</p>
 */
public record BiomeFilter(Mode mode, List<Entry> entries, int scanRadiusChunks) {

    /** How the configured list treats the biomes it matches. */
    public enum Mode {
        NONE,
        WHITELIST,
        BLACKLIST
    }

    /** One configured biome entry: a direct id, or a {@code #tag} reference. */
    public record Entry(boolean tag, String id) {
    }

    /**
     * Builds a filter from raw config strings. Invalid entries are skipped
     * silently because the config validator already refused them at load.
     *
     * @param mode how matched biomes are treated
     * @param raw configured entries
     * @param scanRadiusChunks chunk scan radius around each active head
     * @return the parsed filter
     */
    public static BiomeFilter of(Mode mode, List<? extends String> raw, int scanRadiusChunks) {
        return new BiomeFilter(mode, raw.stream()
                .map(BiomeFilter::parse)
                .filter(Objects::nonNull)
                .toList(), scanRadiusChunks);
    }

    /** True when placement must route around the biome. */
    public boolean blocks(String biomeId, Predicate<String> tagMembership) {
        boolean listed = entries.stream().anyMatch(entry -> entry.tag()
                ? tagMembership.test(entry.id())
                : entry.id().equals(biomeId));
        return switch (mode) {
            case NONE -> false;
            case BLACKLIST -> listed;
            case WHITELIST -> !listed;
        };
    }

    /** Parses one raw entry; null when the string names no id or tag. */
    static Entry parse(String raw) {
        boolean tag = raw.startsWith("#");
        String id = (tag ? raw.substring(1) : raw).toLowerCase(Locale.ROOT);
        return id.indexOf(':') >= 0 ? new Entry(tag, id) : null;
    }
}
