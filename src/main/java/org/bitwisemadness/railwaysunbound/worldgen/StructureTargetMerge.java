package org.bitwisemadness.railwaysunbound.worldgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Pure merge rules for Railways Untold's structure targets.
 *
 * <p>Plain strings on purpose: the whole class is unit-testable without a
 * Minecraft runtime. The mixin layer converts between these and the
 * target's own types; nothing here names a Minecraft class.</p>
 */
public final class StructureTargetMerge {

    private StructureTargetMerge() {
    }

    /**
     * Flattens every group of targets into one list, keeping first-seen order
     * and dropping later duplicates of the same id.
     *
     * <p>A {@code null} id never dedupes: callers use it for targets whose id
     * cannot be derived, and silently dropping those would be worse than
     * keeping them all. {@code null} groups and {@code null} targets are
     * skipped outright.</p>
     *
     * @param groups  groups to flatten, in order; {@code null} groups skipped
     * @param idOf    extracts the dedup key from one target
     * @return the merged list, first-seen order preserved, never contains null
     */
    public static <T> List<T> mergeById(List<? extends List<? extends T>> groups,
            Function<? super T, String> idOf) {
        Map<String, T> seen = new LinkedHashMap<>();
        List<T> merged = new ArrayList<>();
        if (groups == null) {
            return List.of();
        }
        for (List<? extends T> group : groups) {
            if (group == null) {
                continue;
            }
            for (T target : group) {
                if (target == null) {
                    continue;
                }
                String id = idOf.apply(target);
                if (id == null) {
                    merged.add(target);
                    continue;
                }
                if (seen.putIfAbsent(id, target) == null) {
                    merged.add(target);
                }
            }
        }
        return List.copyOf(merged);
    }

    /**
     * Appends every extra tag not already present, preserving order.
     *
     * @param base  already-resolved tags, never {@code null}
     * @param extra tags to append; {@code null} entries ignored
     * @return the union, base order then first-seen extras
     */
    public static List<String> mergeTags(List<String> base, List<String> extra) {
        if (extra == null || extra.isEmpty()) {
            return base;
        }
        List<String> merged = new ArrayList<>(base);
        for (String tag : extra) {
            if (tag != null && !merged.contains(tag)) {
                merged.add(tag);
            }
        }
        return List.copyOf(merged);
    }
}
