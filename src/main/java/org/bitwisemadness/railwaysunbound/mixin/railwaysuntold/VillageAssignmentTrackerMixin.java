package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.worldgen.village.VillageAssignmentTracker;
import java.util.UUID;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Turns off Railways Untold's 25 degree angular claim cap.
 *
 * <p>Every village gate funnels through the three-argument
 * {@code isAngleTooClose}: the layout filter, the branch-claim rejection, the
 * finalize check (the five-argument overload only picks a threshold, then
 * calls this one), and the branch-direction spread penalty. Answering false
 * when the cap is off therefore disables the cap everywhere at once, with no
 * members shadowed and no code copied.</p>
 *
 * <p>Named side effect: {@code BranchDirectionSelector}'s +60 branch penalty
 * reads the same method, so branches stop being steered away from claimed
 * bearings too. That is consistent - the steering exists only to respect the
 * cap.</p>
 *
 * <p>Injected at HEAD with a cancel; gated by {@code requiredMods} in
 * {@code railways_unbound.railwaysuntold.mixins.json}.</p>
 */
@Mixin(VillageAssignmentTracker.class)
public abstract class VillageAssignmentTrackerMixin {

    @Inject(method = "isAngleTooClose(DLjava/util/UUID;D)Z", at = @At("HEAD"), cancellable = true)
    private void railwaysUnbound$dropClaimCap(double angle, UUID headId, double threshold,
            CallbackInfoReturnable<Boolean> callback) {
        if (RailwaysUnboundConfigs.isLoaded() && !RailwaysUnboundConfigs.angularClaimCap()) {
            callback.setReturnValue(false);
        }
    }
}
