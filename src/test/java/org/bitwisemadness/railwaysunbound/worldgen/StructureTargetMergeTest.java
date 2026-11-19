package org.bitwisemadness.railwaysunbound.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Structure target merge rules, pure JVM: dedup by id, tag union.
 */
class StructureTargetMergeTest {

    @Test
    void mergeByIdKeepsFirstSeenOrderAndDropsDuplicates() {
        List<String> merged = StructureTargetMerge.mergeById(
                List.of(List.of("minecraft:village", "terralith:fortified_village"),
                        List.of("minecraft:village", "tensura:villages/dwarf_village")),
                id -> id);
        assertEquals(List.of("minecraft:village", "terralith:fortified_village",
                "tensura:villages/dwarf_village"), merged);
    }

    @Test
    void mergeByIdNeverDedupesTargetsWithoutAnId() {
        List<String> merged = StructureTargetMerge.mergeById(
                List.of(List.of("a", "x"), List.of("x", "a", "x")),
                id -> "x".equals(id) ? null : id);
        assertEquals(List.of("a", "x", "x", "x"), merged);
    }

    @Test
    void mergeByIdSkipsNullGroupsAndTargets() {
        List<List<String>> groups = new ArrayList<>();
        groups.add(null);
        groups.add(Arrays.asList((String) null, "a"));
        List<String> merged = StructureTargetMerge.mergeById(groups, id -> id);
        assertEquals(List.of("a"), merged);
    }

    @Test
    void mergeByIdOnEmptyInputIsEmpty() {
        List<List<String>> empty = List.of();
        assertTrue(StructureTargetMerge.mergeById(empty, id -> id).isEmpty());
        List<List<String>> absent = null;
        assertTrue(StructureTargetMerge.mergeById(absent, id -> id).isEmpty());
    }

    @Test
    void mergeTagsAppendsUnseenExtrasInOrder() {
        List<String> merged = StructureTargetMerge.mergeTags(
                List.of("a", "b"), Arrays.asList("b", "c", null, "a", "d"));
        assertEquals(List.of("a", "b", "c", "d"), merged);
    }

    @Test
    void mergeTagsWithNoExtrasReturnsBaseUntouched() {
        List<String> base = List.of("a");
        assertEquals(base, StructureTargetMerge.mergeTags(base, null));
        assertEquals(base, StructureTargetMerge.mergeTags(base, List.of()));
    }
}
