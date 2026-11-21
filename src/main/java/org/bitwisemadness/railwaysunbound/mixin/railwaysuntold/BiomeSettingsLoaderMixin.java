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
 * Merges this addon's target structure tags into Railways Untold's own
 * targeting tags, so stations can be aimed at any structure in any listed
 * tag - villages, monuments, mineshafts, or a pack's custom structures.
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
    private void railwaysUnbound$appendTargetStructureTags(
            CallbackInfoReturnable<BiomeSettingsLoader.StructureSettings> callback) {
        if (!RailwaysUnboundConfigs.isLoaded()) {
            return;
        }
        List<String> added = RailwaysUnboundConfigs.targetStructureTags();
        BiomeSettingsLoader.StructureSettings original = callback.getReturnValue();
        if (added.isEmpty() || original == null) {
            return;
        }
        List<String> merged = StructureTargetMerge.mergeTags(original.targetTags(), added);
        if (merged.size() == original.targetTags().size()) {
            return;
        }
        callback.setReturnValue(new BiomeSettingsLoader.StructureSettings(
                merged, original.avoidanceBlacklist(), original.undergroundList()));
    }
}
