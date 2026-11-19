package org.bitwisemadness.railwaysunbound.mixin.railwaysuntold;

import com.vodmordia.railwaysuntold.worldgen.village.VillageLayoutPredictor;
import com.vodmordia.railwaysuntold.worldgen.village.util.StructureTagResolver;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry;
import org.bitwisemadness.railwaysunbound.config.RailwaysUnboundConfigs;
import org.bitwisemadness.railwaysunbound.worldgen.StructureTargetMerge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Predicts village layouts from every structure set the village tag
 * resolves to, not just the first.
 *
 * <p>Railways Untold pins layout prediction to
 * {@code StructureTagResolver.resolve(level).get(0)}: whichever set happens
 * to sort first is the only set whose villages ever confirm, and every other
 * village fails with {@code No village variant matched biome}. Merging the
 * structures of all resolved sets into one synthetic set fixes that without
 * touching the resolver's own cache or its spacing/salt bookkeeping.</p>
 *
 * <p>The synthetic set reuses the first target's placement, which is safe:
 * the merged holder is read only by {@code computeLayout}, and that reads
 * {@link StructureSet#structures()} and never {@link StructureSet#placement()},
 * so nothing about actual generation changes - only what predicts as
 * confirmable.</p>
 *
 * <p>Skipped when fewer than two sets resolve (there is nothing to merge) and
 * when the config opts back into upstream's single-set pin.</p>
 */
@Mixin(VillageLayoutPredictor.class)
public abstract class VillageLayoutPredictorMixin {

    @Inject(method = "findTargetStructureSet", at = @At("HEAD"), cancellable = true)
    private static void railwaysUnbound$mergeEveryTargetSet(ServerLevel level,
            CallbackInfoReturnable<Holder<StructureSet>> callback) {
        if (!RailwaysUnboundConfigs.isLoaded() || !RailwaysUnboundConfigs.validateAllStructureSets()) {
            return;
        }
        List<StructureTagResolver.ResolvedStructureTarget> targets = StructureTagResolver.resolve(level);
        if (targets.size() < 2) {
            return;
        }
        List<StructureSelectionEntry> merged = StructureTargetMerge.mergeById(
                targets.stream().map(target -> target.structureSet().value().structures()).toList(),
                VillageLayoutPredictorMixin::railwaysUnbound$structureId);
        if (merged.isEmpty()) {
            return;
        }
        StructureSet first = targets.get(0).structureSet().value();
        callback.setReturnValue(Holder.direct(new StructureSet(merged, first.placement())));
    }

    private static String railwaysUnbound$structureId(StructureSelectionEntry entry) {
        return entry.structure().unwrapKey()
                .map(key -> key.location().toString())
                .orElse(null);
    }
}
