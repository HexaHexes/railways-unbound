package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.worldgen.placement.TrackPlacementInitializer;
import net.minecraft.server.level.ServerPlayer;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels Railways Untold's login bootstrap in dimensions this addon's
 * config denies.
 *
 * <p>The only mixin in this repo: Railways Untold hardcodes its overworld
 * and world-type checks with no public veto, so WHITELIST and BLACKLIST
 * modes have no other way to say no. Injected at HEAD with a cancel, so no
 * members are shadowed and no code is copied; gated by {@code requiredMods}
 * in {@code railways_unbound.railwaysuntold.mixins.json}.</p>
 */
@Mixin(TrackPlacementInitializer.class)
public abstract class TrackPlacementInitializerMixin {

    @Inject(method = "onPlayerLoginImpl", at = @At("HEAD"), cancellable = true)
    private static void railwaysUnbound$denyDeniedDimensions(ServerPlayer player, CallbackInfo callback) {
        if (!RailwaysUnboundConfigs.dimensions().allows(
                player.level().dimension().location().toString())) {
            callback.cancel();
        }
    }
}
