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
    private static final ModConfigSpec.BooleanValue ANGULAR_CLAIM_CAP;
    private static final ModConfigSpec.BooleanValue VALIDATE_ALL_STRUCTURE_SETS;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_STRUCTURE_TAGS;
    private static final ModConfigSpec.IntValue MIN_DISTANCE_FROM_SPAWN;
    private static final ModConfigSpec.IntValue SEARCH_RADIUS;
    private static final ModConfigSpec.IntValue FORWARD_SEARCH_LIMIT;

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
        builder.push("villages");
        ANGULAR_CLAIM_CAP = builder
                .comment("false: villages may be targeted from any bearing, so stations")
                .comment("can generate at every village. true: Railways Untold's own")
                .comment("25 degree claim cap, unchanged.")
                .define("angularClaimCap", false);
        VALIDATE_ALL_STRUCTURE_SETS = builder
                .comment("false: layout prediction pins to the first structure set the")
                .comment("village tag resolves to, so only that set's villages confirm.")
                .comment("true: prediction considers every structure set the tag")
                .comment("resolves to, so mixed-set packs confirm all their villages.")
                .define("validateAllStructureSets", true);
        EXTRA_STRUCTURE_TAGS = builder
                .comment("Extra structure tags merged into Railways Untold's village")
                .comment("targets, e.g. 'railways_unbound:villages'. Tags only - a bare")
                .comment("structure id is rejected. Takes effect on world load or")
                .comment("/reload.")
                .defineListAllowEmpty("extraStructureTags",
                        List.of("railways_unbound:villages"), () -> "",
                        RailwaysUnboundConfigs::validBiomeEntry);
        MIN_DISTANCE_FROM_SPAWN = builder
                .comment("Blocks a village must be from world spawn to be targeted.")
                .comment("0 disables Railways Untold's permanent TOO_CLOSE_TO_SPAWN")
                .comment("gate, which otherwise rejects the village forever.")
                .defineInRange("minDistanceFromSpawn", 0, 0, 30000000);
        SEARCH_RADIUS = builder
                .comment("Block radius in which a head searches for an unassigned")
                .comment("village. Railways Untold's own value, exposed.")
                .defineInRange("searchRadius", 1000, 16, 30000000);
        FORWARD_SEARCH_LIMIT = builder
                .comment("Block limit on the forward search for a village the head")
                .comment("is already heading toward. Railways Untold's own value,")
                .comment("exposed.")
                .defineInRange("forwardSearchLimit", 4000, 16, 30000000);
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
     * Whether the 25 degree angular claim cap applies. Read live per query;
     * the hot village-assignment path reads this single boolean.
     */
    public static boolean angularClaimCap() {
        return ANGULAR_CLAIM_CAP.get();
    }

    /**
     * Whether layout prediction validates every structure set the village tag
     * resolves to, rather than the first only.
     */
    public static boolean validateAllStructureSets() {
        return VALIDATE_ALL_STRUCTURE_SETS.get();
    }

    /**
     * Extra structure tags merged into Railways Untold's village targets.
     * The leading {@code #} is stripped: Railways Untold parses bare ids only.
     */
    public static List<String> extraStructureTags() {
        return EXTRA_STRUCTURE_TAGS.get().stream()
                .map(tag -> tag.startsWith("#") ? tag.substring(1) : tag)
                .toList();
    }

    /** Blocks a village must be from world spawn to be targeted. */
    public static int minDistanceFromSpawn() {
        return MIN_DISTANCE_FROM_SPAWN.get();
    }

    /** Block radius in which a head searches for an unassigned village. */
    public static int searchRadius() {
        return SEARCH_RADIUS.get();
    }

    /** Block limit on the forward search for a village ahead of the head. */
    public static int forwardSearchLimit() {
        return FORWARD_SEARCH_LIMIT.get();
    }

    /**
     * Whether the SERVER spec has been loaded yet. Mixins that run before the
     * config exists (a client resource pass, an early worldgen read) must skip
     * rather than trip over an unloaded value.
     */
    public static boolean isLoaded() {
        return SPEC.isLoaded();
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
