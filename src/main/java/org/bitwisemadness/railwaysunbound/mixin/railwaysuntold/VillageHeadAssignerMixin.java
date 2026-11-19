package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.worldgen.head.VillageHeadAssigner;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Exposes the three hardcoded gates inside {@code tryAssignVillage}.
 *
 * <p>Each is an inline constant with no config behind it upstream: the
 * {@code TOO_CLOSE_TO_SPAWN} distance (a failed attempt is written to the
 * network's permanent record, so a village rejected once is rejected
 * forever), the search radius used twice - the forward search and the
 * unassigned-village fallback - and the forward search limit. Replacing the
 * constants is the only injection point: none of them is a field read, and
 * the surrounding control flow is not worth copying.</p>
 *
 * <p>{@link Constant#intValue()} matches every occurrence, so both
 * {@code 1000}s move together; a constant that disappears upstream fails the
 * load loudly through {@code defaultRequire} rather than silently reverting.
 * Defaults equal upstream's own numbers, so the shipped behavior differs
 * only in the spawn distance, which defaults to off.</p>
 */
@Mixin(VillageHeadAssigner.class)
public abstract class VillageHeadAssignerMixin {

    @ModifyConstant(method = "tryAssignVillage", constant = @Constant(doubleValue = 100.0))
    private static double railwaysUnbound$minDistanceFromSpawn(double original) {
        return RailwaysUnboundConfigs.isLoaded()
                ? RailwaysUnboundConfigs.minDistanceFromSpawn() : original;
    }

    @ModifyConstant(method = "tryAssignVillage", constant = @Constant(intValue = 1000))
    private static int railwaysUnbound$searchRadius(int original) {
        return RailwaysUnboundConfigs.isLoaded()
                ? RailwaysUnboundConfigs.searchRadius() : original;
    }

    @ModifyConstant(method = "tryAssignVillage", constant = @Constant(intValue = 4000))
    private static int railwaysUnbound$forwardSearchLimit(int original) {
        return RailwaysUnboundConfigs.isLoaded()
                ? RailwaysUnboundConfigs.forwardSearchLimit() : original;
    }
}
