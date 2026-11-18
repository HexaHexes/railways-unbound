package org.bitwisemadness.railwaysunbound.railways;

import com.vodmordia.railwaysuntold.config.RailwaysUntoldConfig;
import com.vodmordia.railwaysuntold.worldgen.api.AvoidanceZone;
import com.vodmordia.railwaysuntold.worldgen.api.AvoidanceZoneProvider;
import com.vodmordia.railwaysuntold.worldgen.api.TrackAvoidanceApi;
import com.vodmordia.railwaysuntold.worldgen.head.TrackExpansionHead;
import com.vodmordia.railwaysuntold.worldgen.placement.ChunkLoadTrackExpander;
import com.vodmordia.railwaysuntold.worldgen.placement.TrackExpansionOrchestrator;
import com.vodmordia.railwaysuntold.worldgen.placement.TrackPlacementInitializer;
import com.vodmordia.railwaysuntold.worldgen.placement.TrackPlacerRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.bitwisemadness.railwaysunbound.worldgen.BiomeFilter;
import org.bitwisemadness.railwaysunbound.worldgen.BiomeZoneBuilder;
import org.bitwisemadness.railwaysunbound.worldgen.DimensionGate;

/**
 * The only class importing Railways Untold types.
 *
 * <p>Three registrations happen here, all against public static registries
 * verified against the pinned jar (see docs/RAILWAYS-UNTOLD.md): the biome
 * avoidance provider that routes tracks around blocked biomes, the generator
 * claim that bootstraps whitelisted non-overworld dimensions, and the
 * chunk-load extension that lets Railways Untold process chunks in those
 * dimensions. All three read the live server config on every query.</p>
 */
public final class RailwaysUntoldAccess {

    private static final Map<String, TagKey<Biome>> TAG_KEYS = new ConcurrentHashMap<>();

    private RailwaysUntoldAccess() {
    }

    /** Registers every Railways Untold hook this addon owns. */
    public static void registerHooks() {
        TrackAvoidanceApi.registerProvider(new BiomeAvoidanceProvider());
        TrackPlacementInitializer.registerGeneratorClaim(
                RailwaysUntoldAccess::claimMatches, RailwaysUntoldConfig::getDefault);
        ChunkLoadTrackExpander.registerDimensionExtension(RailwaysUntoldAccess::extensionMatches);
    }

    /**
     * Claims whitelisted non-overworld dimensions. WHITELIST only: a
     * blacklist never widens where generation bootstraps, and the overworld
     * keeps Railways Untold's own world-type check.
     */
    private static boolean claimMatches(ServerLevel level) {
        DimensionGate gate = RailwaysUnboundConfigs.dimensions();
        String id = dimensionId(level);
        return gate.mode() == DimensionGate.Mode.WHITELIST
                && gate.allows(id)
                && !DimensionGate.OVERWORLD.equals(id);
    }

    /** Lets chunk-load expansion run wherever generation is allowed. */
    private static boolean extensionMatches(ServerLevel level) {
        return RailwaysUnboundConfigs.dimensions().allows(dimensionId(level));
    }

    private static String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    /** Resolves a validated '#namespace:path' entry to its TagKey. */
    private static TagKey<Biome> tagKey(String id) {
        return TAG_KEYS.computeIfAbsent(id,
                key -> TagKey.create(Registries.BIOME, ResourceLocation.parse(key)));
    }

    /**
     * Serves avoidance zones for the biome filter.
     *
     * <p>Railways Untold queries providers on every placement decision and
     * consults no cache of its own, so the zone set is rebuilt at most once
     * per server tick per level and served from that cache in between. The
     * config itself is still read live per query.</p>
     */
    private static final class BiomeAvoidanceProvider implements AvoidanceZoneProvider {

        private final Map<ResourceKey<Level>, CacheEntry> cache = new HashMap<>();

        private record CacheEntry(long tick, List<AvoidanceZone> zones) {
        }

        @Override
        public Collection<AvoidanceZone> getZones(ServerLevel level) {
            BiomeFilter filter = RailwaysUnboundConfigs.biomes();
            if (filter.mode() == BiomeFilter.Mode.NONE) {
                return List.of();
            }
            long tick = level.getGameTime();
            CacheEntry entry = cache.get(level.dimension());
            if (entry == null || entry.tick() != tick) {
                entry = new CacheEntry(tick, scan(level, filter));
                cache.put(level.dimension(), entry);
            }
            return entry.zones();
        }

        /** Boxes the blocked chunks around every active head. */
        private List<AvoidanceZone> scan(ServerLevel level, BiomeFilter filter) {
            TrackExpansionOrchestrator orchestrator = TrackPlacerRegistry.get(level);
            if (orchestrator == null) {
                return List.of();
            }
            Set<Long> blocked = new HashSet<>();
            for (TrackExpansionHead head : orchestrator.getHeadManager().getActiveHeads()) {
                collectBlocked(level, filter, head.getPosition(), blocked);
            }
            int minY = level.getMinBuildHeight();
            int maxYExclusive = minY + level.getHeight();
            List<AvoidanceZone> zones = new ArrayList<>();
            for (BiomeZoneBuilder.ChunkBox box : BiomeZoneBuilder.mergeRuns(blocked)) {
                zones.add(new AvoidanceZone(
                        new BlockPos(box.minChunkX() << 4, minY, box.minChunkZ() << 4),
                        new BlockPos((box.maxChunkX() << 4) - 1, maxYExclusive - 1,
                                (box.maxChunkZ() << 4) - 1)));
            }
            return zones;
        }

        /**
         * Adds every blocked chunk within the scan radius of one head. The
         * biome read is the same noise-blended one Railways Untold's own
         * filters use, so it answers for unloaded chunks too and never
         * forces a chunk load.
         */
        private void collectBlocked(ServerLevel level, BiomeFilter filter, BlockPos headPos,
                Set<Long> blocked) {
            int minY = level.getMinBuildHeight();
            int headY = Mth.clamp(headPos.getY(), minY, minY + level.getHeight() - 1);
            int centerChunkX = headPos.getX() >> 4;
            int centerChunkZ = headPos.getZ() >> 4;
            int radius = filter.scanRadiusChunks();
            for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
                for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius;
                        chunkX++) {
                    if (isBlocked(level, filter, chunkX, chunkZ, headY)) {
                        blocked.add(BiomeZoneBuilder.pack(chunkX, chunkZ));
                    }
                }
            }
        }

        /** Samples the biome at the head's elevation. */
        private boolean isBlocked(ServerLevel level, BiomeFilter filter, int chunkX, int chunkZ,
                int headY) {
            Holder<Biome> biome = level.getBiome(
                    new BlockPos((chunkX << 4) + 8, headY, (chunkZ << 4) + 8));
            String id = biome.unwrapKey().map(key -> key.location().toString()).orElse("");
            return filter.blocks(id, tag -> biome.is(tagKey(tag)));
        }
    }
}
