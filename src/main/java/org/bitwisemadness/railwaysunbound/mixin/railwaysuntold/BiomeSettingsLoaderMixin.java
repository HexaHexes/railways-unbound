package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.datapack.BiomeSettingsLoader;
import java.util.List;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.bitwisemadness.railwaysunbound.worldgen.StructureTargetMerge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Merges this addon's extra structure tags into Railways Untold's village
 * targets.
 *
 * <p>Injecting here rather than in {@code resolveStructureSettings} is
 * deliberate: that method returns a cached record, and
 * {@code StructureAvoidanceFilter} compares its blacklist by reference to
 * decide whether to rebuild, so handing out a fresh record per call would
 * rebuild those caches on every query. {@code buildStructureSettings} runs
 * once per datapack reload and its result is what gets cached, so the
 * augmented tags survive with the original blacklist list - and its
 * reference - untouched.</p>
 *
 * <p>The config is read behind {@link RailwaysUnboundConfigs#isLoaded()}
 * because an unloaded SERVER spec throws rather than returning a default;
 * a skip here is corrected by the next reload, a throw would fail the
 * reload. Takes effect on world load or {@code /reload}.</p>
 */
@Mixin(BiomeSettingsLoader.class)
public abstract class BiomeSettingsLoaderMixin {

    @Inject(method = "buildStructureSettings", at = @At("RETURN"), cancellable = true)
    private void railwaysUnbound$appendExtraStructureTags(
            CallbackInfoReturnable<BiomeSettingsLoader.StructureSettings> callback) {
        if (!RailwaysUnboundConfigs.isLoaded()) {
            return;
        }
        List<String> extra = RailwaysUnboundConfigs.extraStructureTags();
        BiomeSettingsLoader.StructureSettings original = callback.getReturnValue();
        if (extra.isEmpty() || original == null) {
            return;
        }
        List<String> merged = StructureTargetMerge.mergeTags(original.targetTags(), extra);
        if (merged.size() == original.targetTags().size()) {
            return;
        }
        callback.setReturnValue(new BiomeSettingsLoader.StructureSettings(
                merged, original.avoidanceBlacklist(), original.undergroundList()));
    }
}
