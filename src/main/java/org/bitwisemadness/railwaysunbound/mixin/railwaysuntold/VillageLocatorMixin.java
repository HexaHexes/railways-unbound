package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.worldgen.village.VillageLocator;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Opens the second, quieter spawn gate: the {@code 100} block exclusion
 * inside {@code findNearestUnassignedVillage}.
 *
 * <p>{@code tryAssignVillage}'s own spawn gate writes a permanent
 * {@code TOO_CLOSE_TO_SPAWN} attempt to the network record, so
 * {@link VillageHeadAssignerMixin} is the one that matters for permanence.
 * This gate is different: it is a bare {@code if (dist < 100) continue} inside
 * the unassigned-village fallback loop, so a village skipped here is merely
 * not considered this time - it is not recorded as rejected. Leaving it at
 * {@code 100} while {@code minDistanceFromSpawn = 0} would therefore keep
 * silently dropping villages near spawn on every fallback search, and the
 * config would appear not to work.</p>
 *
 * <p>The five-argument overload is a pure delegate to the six-argument one
 * (it passes a {@code null} predicate), so patching the six-argument method
 * covers both entry points. The selector carries the full descriptor to name
 * that overload alone - the same reason
 * {@code VillageAssignmentTrackerMixin} spells out its descriptor.</p>
 *
 * <p>The surrounding {@code Objects.requireNonNull(config)} reads the config
 * but discards it; the distance is the inline {@code bipush 100} immediately
 * after, which is the only int {@code 100} in the method (the other constant
 * is {@code Integer.MAX_VALUE}). Replacing it is the whole injection - no
 * shadowed members, no copied control flow.</p>
 */
@Mixin(VillageLocator.class)
public abstract class VillageLocatorMixin {

    @ModifyConstant(
            method = "findNearestUnassignedVillage(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/core/BlockPos;I"
                    + "Lcom/vodmordia/railwaysuntold/worldgen/network/RailNetwork;"
                    + "Lcom/vodmordia/railwaysuntold/config/RailwaysUntoldConfig;"
                    + "Ljava/util/function/Predicate;)"
                    + "Lcom/vodmordia/railwaysuntold/worldgen/village/VillageInfo;",
            constant = @Constant(intValue = 100))
    private static int railwaysUnbound$minDistanceFromSpawn(int original) {
        return RailwaysUnboundConfigs.isLoaded()
                ? RailwaysUnboundConfigs.minDistanceFromSpawn() : original;
    }
}
