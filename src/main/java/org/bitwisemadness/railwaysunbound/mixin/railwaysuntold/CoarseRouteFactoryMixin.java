package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.config.RailwaysUntoldConfig;
import com.vodmordia.railwaysuntold.worldgen.planner.noise.CoarseRouteFactory;
import com.vodmordia.railwaysuntold.worldgen.terrain.NoiseStructureScanner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes structure avoidance unconditional while leaving terrain avoidance
 * under Railways Untold's own {@code allAvoidance}.
 *
 * <p>Railways Untold has four ways a rail can still end up crossing an
 * existing structure even with {@code allAvoidance = true}, and this mixin
 * closes all of them behind {@code alwaysAvoidStructures} (default on):</p>
 *
 * <ol>
 * <li><b>The defer overflow.</b> Structure candidates whose chunks have not
 * generated yet are deferred ("ROUTE-DEFER") for up to six cycles while
 * prewarm runs, then the route commits with {@code ScanResult.predictions()}
 * only - the still-unverified candidates are silently dropped, so neither
 * planning nor validation ever sees them. The two {@code predictions()}
 * redirects below append a conservative obstacle per remaining
 * {@link NoiseStructureScanner.DeferredCandidate} instead: the candidate's
 * structure-set name, its chunk as the center, and no footprint, which makes
 * {@code getStructureRadius} fall back to its 48 block default (or the
 * surveyed layout for village-like sets). Duplicates collapse later through
 * {@code structureKey} (chunk + set name) and
 * {@code mergeStructuresDedupByChunk}.</li>
 * <li><b>The validation gate.</b> {@code lambda$createAndAttachInternal$9}
 * skips {@code validateRouteAgainstStructures} entirely when
 * {@code allAvoidance} is off. The redirect forces it on.</li>
 * <li><b>The defer gate.</b> {@code createAndAttachInternal} skips the
 * unverified-candidate wait when {@code allAvoidance} is off, committing
 * immediately with whatever is unverified. The redirect forces the wait on -
 * though with (1) in place the wait is now an optimization, not a
 * correctness gate.</li>
 * <li><b>The blacklist</b> ({@code structure_avoidance_blacklist}) is
 * neutralized in {@link StructureAvoidanceFilterMixin}.</li>
 * </ol>
 *
 * <p>Deliberately untouched: the underground/buried exemptions
 * ({@code structure_underground_list}, fully buried structures), the lostcity
 * passthrough exemptions, and {@code AvoidanceConstraintService} steering -
 * steering and terrain avoidance stay under Railways Untold's own
 * {@code allAvoidance}, per this addon's scope.</p>
 *
 * <p>Every redirect handler re-enters the original static call from the
 * handler body, which is a different call site and is never re-redirected.
 * The config is read behind {@link RailwaysUnboundConfigs#isLoaded()} so an
 * unloaded SERVER spec falls back to upstream behavior instead of throwing.</p>
 */
@Mixin(CoarseRouteFactory.class)
public abstract class CoarseRouteFactoryMixin {

    @Redirect(method = "createAndAttachInternal", at = @At(value = "INVOKE",
            target = "Lcom/vodmordia/railwaysuntold/config/RailwaysUntoldConfig;"
                    + "->isAllAvoidanceEnabled()Z"))
    private static boolean railwaysUnbound$forceDeferGate() {
        return railwaysUnbound$alwaysAvoid() || RailwaysUntoldConfig.isAllAvoidanceEnabled();
    }

    @Redirect(method = "lambda$createAndAttachInternal$9", at = @At(value = "INVOKE",
            target = "Lcom/vodmordia/railwaysuntold/config/RailwaysUntoldConfig;"
                    + "->isAllAvoidanceEnabled()Z"))
    private static boolean railwaysUnbound$forceValidationGate() {
        return railwaysUnbound$alwaysAvoid() || RailwaysUntoldConfig.isAllAvoidanceEnabled();
    }

    @Redirect(method = "createAndAttachInternal", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/vodmordia/railwaysuntold/worldgen/terrain/NoiseStructureScanner"
                    + "$ScanResult;->predictions()Ljava/util/List;"))
    private static List<NoiseStructureScanner.PredictedStructure> railwaysUnbound$commitWithDeferred(
            NoiseStructureScanner.ScanResult scan) {
        if (!railwaysUnbound$alwaysAvoid()) {
            return scan.predictions();
        }
        return railwaysUnbound$withDeferred(scan);
    }

    @Redirect(method = "validateRouteAgainstStructures", at = @At(value = "INVOKE",
            target = "Lcom/vodmordia/railwaysuntold/worldgen/terrain/NoiseStructureScanner"
                    + "$ScanResult;->predictions()Ljava/util/List;"))
    private static List<NoiseStructureScanner.PredictedStructure> railwaysUnbound$validateWithDeferred(
            NoiseStructureScanner.ScanResult scan) {
        if (!railwaysUnbound$alwaysAvoid()) {
            return scan.predictions();
        }
        return railwaysUnbound$withDeferred(scan);
    }

    private static boolean railwaysUnbound$alwaysAvoid() {
        return RailwaysUnboundConfigs.isLoaded() && RailwaysUnboundConfigs.alwaysAvoidStructures();
    }

    /**
     * The scan's verified predictions plus one conservative obstacle per
     * still-unverified candidate. Empty-deferred scans return the original
     * list untouched, so the common path allocates nothing.
     */
    private static List<NoiseStructureScanner.PredictedStructure> railwaysUnbound$withDeferred(
            NoiseStructureScanner.ScanResult scan) {
        List<NoiseStructureScanner.DeferredCandidate> deferred = scan.deferredCandidates();
        if (deferred.isEmpty()) {
            return scan.predictions();
        }
        List<NoiseStructureScanner.PredictedStructure> merged =
                new ArrayList<>(scan.predictions());
        for (NoiseStructureScanner.DeferredCandidate candidate : deferred) {
            merged.add(railwaysUnbound$asObstacle(candidate));
        }
        return merged;
    }

    /**
     * A deferred candidate as a footprint-less obstacle: named after its
     * structure set (so {@code structureKey} dedups it against the real
     * prediction if one shows up later), centered on the candidate chunk at a
     * neutral height, and flagged village-like only when Railways Untold's
     * own name heuristic would have flagged it - which keeps the surveyed
     * layout radius fallback available for village-like sets.
     */
    private static NoiseStructureScanner.PredictedStructure railwaysUnbound$asObstacle(
            NoiseStructureScanner.DeferredCandidate candidate) {
        ChunkPos chunk = candidate.chunkPos();
        String setName = candidate.setHolder().unwrapKey()
                .map(key -> key.location().toString())
                .orElse("unknown");
        List<String> structureNames = candidate.setHolder().value().structures().stream()
                .map(entry -> entry.structure().unwrapKey()
                        .map(key -> key.location().toString())
                        .orElse("unknown"))
                .toList();
        boolean villageLike = setName.contains("village")
                || structureNames.stream().anyMatch(name -> name.contains("village"));
        BlockPos center = new BlockPos(chunk.getMiddleBlockX(), 64, chunk.getMiddleBlockZ());
        return new NoiseStructureScanner.PredictedStructure(
                chunk, center, setName, structureNames, villageLike);
    }
}
