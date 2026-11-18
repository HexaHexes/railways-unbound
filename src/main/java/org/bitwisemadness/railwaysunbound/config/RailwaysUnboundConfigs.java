package org.bitwisemadness.railwaysunbound.config;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.bitwisemadness.railwaysunbound.worldgen.BiomeFilter;
import org.bitwisemadness.railwaysunbound.worldgen.DimensionGate;
import org.slf4j.Logger;

/**
 * The {@code railways_unbound-server.toml} spec.
 *
 * <p>SERVER is the authority channel of the org config contract: values sync
 * to clients, accept per-world overrides in {@code <world>/serverconfig/},
 * and reload without a restart. Every reader calls the live accessors below
 * per query; nothing caches a snapshot.</p>
 */
public final class RailwaysUnboundConfigs {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<DimensionGate.Mode> DIMENSION_MODE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSIONS;
    private static final ModConfigSpec.EnumValue<BiomeFilter.Mode> BIOME_MODE;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> BIOMES;
    private static final ModConfigSpec.IntValue BIOME_SCAN_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("dimensions");
        DIMENSION_MODE = builder
                .comment("How the dimension list gates Railways Untold's bootstrap.")
                .comment("OVERWORLD_ONLY: Railways Untold's own behavior, unchanged.")
                .comment("WHITELIST: only the listed dimensions generate; the overworld")
                .comment("must be listed to stay enabled. BLACKLIST: the listed")
                .comment("dimensions are denied and the rest is unchanged.")
                .defineEnum("mode", DimensionGate.Mode.OVERWORLD_ONLY);
        DIMENSIONS = builder
                .comment("Dimension ids, e.g. 'minecraft:the_nether'. Whitelisted")
                .comment("dimensions beyond the overworld bootstrap their own track")
                .comment("network; the overworld keeps Railways Untold's own world")
                .comment("type check either way.")
                .defineListAllowEmpty("list", List.of(), () -> "",
                        RailwaysUnboundConfigs::validDimensionId);
        builder.pop();
        builder.push("biomes");
        BIOME_MODE = builder
                .comment("How the biome list gates track placement.")
                .comment("NONE: no gating. BLACKLIST: tracks route around the listed")
                .comment("biomes. WHITELIST: tracks route around every biome except")
                .comment("the listed ones.")
                .defineEnum("mode", BiomeFilter.Mode.NONE);
        BIOMES = builder
                .comment("Biome ids or '#tags', e.g. 'minecraft:deep_ocean' or")
                .comment("'#minecraft:is_ocean'. Gating samples the biome at each")
                .comment("head's elevation and blocks that whole chunk column, so")
                .comment("surface and cave biomes gate at the track's own height.")
                .defineListAllowEmpty("list", List.of(), () -> "",
                        RailwaysUnboundConfigs::validBiomeEntry);
        BIOME_SCAN_RADIUS = builder
                .comment("Chunk radius around each active head rescanned for blocked")
                .comment("biomes each server tick. Smaller is cheaper; blocked chunks")
                .comment("outside the ring still block once tracks reach them.")
                .defineInRange("scanRadiusChunks", 4, 1, 16);
        builder.pop();
        SPEC = builder.build();
    }

    private RailwaysUnboundConfigs() {
    }

    /** Registers the config file; called once from the mod constructor. */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC);
    }

    /** The dimension gate; read live per query, never cached. */
    public static DimensionGate dimensions() {
        return DimensionGate.of(DIMENSION_MODE.get(), DIMENSIONS.get());
    }

    /** The biome filter; read live per query, never cached. */
    public static BiomeFilter biomes() {
        return BiomeFilter.of(BIOME_MODE.get(), BIOMES.get(), BIOME_SCAN_RADIUS.get());
    }

    /**
     * Warns about configurations that silence all generation. An empty
     * whitelist is a valid way to disable everything, but it should be loud.
     */
    public static void warnOnDegenerateCombos() {
        if (DIMENSION_MODE.get() == DimensionGate.Mode.WHITELIST && DIMENSIONS.get().isEmpty()) {
            LOGGER.warn("Dimension mode WHITELIST lists no dimension: "
                    + "Railways Untold will not generate anywhere");
        }
        if (BIOME_MODE.get() == BiomeFilter.Mode.WHITELIST && BIOMES.get().isEmpty()) {
            LOGGER.warn("Biome mode WHITELIST lists no biome: tracks route around "
                    + "everything and heads will stall");
        }
    }

    private static boolean validDimensionId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }

    private static boolean validBiomeEntry(Object value) {
        if (!(value instanceof String entry) || entry.isEmpty()) {
            return false;
        }
        String id = entry.startsWith("#") ? entry.substring(1) : entry;
        return ResourceLocation.tryParse(id) != null;
    }
}
