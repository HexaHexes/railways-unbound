package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.config.StructureAvoidanceFilter;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Neutralizes Railways Untold's structure avoidance blacklist.
 *
 * <p>{@code StructureAvoidanceFilter.isBlacklisted} is the blacklist's only
 * consumer: {@code tryPredictStructure} early-returns on it, which drops the
 * structure from the obstacle set entirely and lets rails pass straight
 * through. The datapack key is {@code structure_avoidance_blacklist}
 * (default {@code ["wythers:"]}) and it means "exempt from avoidance", not
 * "avoid this". With {@code alwaysAvoidStructures} on, no structure is
 * exempt on that route: the handler returns {@code false} at HEAD before the
 * cache logic runs, so the cache fields are simply never consulted.</p>
 *
 * <p>The handler declares only the {@link CallbackInfoReturnable} - no
 * {@code structureId} parameter - because Mixin's {@code checkDescriptor}
 * accepts that simple descriptor with {@code captureArgs = false}, and an
 * unused target parameter would only earn a CodeQL {@code Useless parameter}
 * finding. The config is read behind
 * {@link RailwaysUnboundConfigs#isLoaded()} so an unloaded SERVER spec falls
 * back to upstream behavior instead of throwing.</p>
 */
@Mixin(StructureAvoidanceFilter.class)
public abstract class StructureAvoidanceFilterMixin {

    @Inject(method = "isBlacklisted", at = @At("HEAD"), cancellable = true)
    private static void railwaysUnbound$neverBlacklist(
            CallbackInfoReturnable<Boolean> callback) {
        if (RailwaysUnboundConfigs.isLoaded() && RailwaysUnboundConfigs.alwaysAvoidStructures()) {
            callback.setReturnValue(false);
        }
    }
}
