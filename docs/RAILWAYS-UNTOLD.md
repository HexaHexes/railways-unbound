# Railways Untold interop contract

Everything this repo assumes about Railways Untold, verified with `javap`
against the pinned jar (CurseMaven `curse.maven:railways-untold-1470432:8755353`,
"Railways Untold" 1.2.2, NeoForge 1.21.1). Re-verify each section against a
new jar before merging a pin bump.

## Identity

- Mod id `railwaysuntold`, package `com.vodmordia.railwaysuntold`, author
  vodmordia. Closed beta; the shipped jar says All Rights Reserved (a GPL
  relicensing is announced but no source is published). Distribute nothing of
  it; compile against it, gate on it, never bundle it.

## Generation model

There is no vanilla worldgen JSON anywhere in the jar: no placed features, no
biome modifiers, no structures. Generation is entirely code-driven:

1. A player-login bootstrap (`TrackPlacementInitializer.onPlayerLoginImpl`)
   checks an active track backend (Create or Immersive Railroading), then any
   registered `GeneratorClaim`. Only the unclaimed default path runs the
   hardcoded `minecraft:overworld` string check and the
   `generateInWorldTypes` world-type check.
2. `TrackExpansionOrchestrator` then drives expansion heads
   (`ExpansionHeadManager.getActiveHeads()`), planned and placed segment by
   segment.
3. Every segment endpoint passes
   `TrackAvoidanceApi.isInsideAnyZone(pos, level)` in
   `PathExecutor.executeNextSegment`. A hit clears the remaining planned
   path, so the head re-plans around the zone; boxed-in heads stall rather
   than place inside it.

## Hooks this addon uses

| Hook | Verified signature | Used for |
| --- | --- | --- |
| `TrackAvoidanceApi.registerProvider` | `static void (AvoidanceZoneProvider)` | biome gating zones |
| `AvoidanceZoneProvider.getZones` | `Collection<AvoidanceZone> (ServerLevel)` | per-query zone supply |
| `AvoidanceZone` | record `(BlockPos minCorner, BlockPos maxCorner)`; `contains` is inclusive on both corners | zone boxes |
| `TrackPlacementInitializer.registerGeneratorClaim` | `static void (Predicate<ServerLevel>, Supplier<RailwaysUntoldConfig>)` | whitelisted non-overworld bootstrap |
| `ChunkLoadTrackExpander.registerDimensionExtension` | `static void (Predicate<ServerLevel>)` | chunk-load expansion in allowed dimensions |
| `TrackPlacerRegistry.get` | `static TrackExpansionOrchestrator (ServerLevel)`, null-safe read | head lookup for zone scans |
| `TrackExpansionOrchestrator.getHeadManager` → `ExpansionHeadManager.getActiveHeads` → `TrackExpansionHead.getPosition` | instance accessors | head positions |

Both avoidance queries call every provider on every check and consult no
cache of their own, hence this repo's one-tick zone cache in
`RailwaysUntoldAccess`. Mixin targets are not hooks; they are listed in the
next section.

## Mixins

All eight live in `org.bitwisemadness.railwaysunbound.mixin.railwaysuntold`,
are registered in `railways_unbound.railwaysuntold.mixins.json` with
`defaultRequire: 1` and no refmap, and are gated by
`requiredMods = ["railwaysuntold"]` in
`src/main/templates/META-INF/neoforge.mods.toml`. Every target method was
verified with `javap -p` to be declared on the target class itself.

### `TrackPlacementInitializerMixin`

- Target: `...worldgen.placement.TrackPlacementInitializer.onPlayerLoginImpl(Lnet/minecraft/server/level/ServerPlayer;)V`
  (private static, single, no overloads).
- Shape: `@Inject` at `HEAD`, `cancellable = true`; cancels when the live
  dimension gate denies the level. No `@Shadow`.
- Why a mixin at all: the default path's overworld and world-type checks are
  hardcoded with no public veto. A `GeneratorClaim` can only widen
  generation, never narrow it, so deny semantics for the overworld have no
  API route.

### `VillageAssignmentTrackerMixin`

- Target: `...worldgen.village.VillageAssignmentTracker.isAngleTooClose(DLjava/util/UUID;D)Z`
  (instance; the full descriptor is required, the class also declares
  `isAngleTooClose(DLjava/util/UUID;DDZ)Z`).
- Shape: `@Inject` at `HEAD`, `cancellable = true`; returns `false` when
  `angularClaimCap = false`. The handler declares only the
  `CallbackInfoReturnable` - no `angle`/`headId`/`threshold` parameters -
  because Mixin's `Callback.checkDescriptor` accepts that "simple" descriptor
  and sets `captureArgs = false`, so the target arguments are never loaded.
  Declaring them just to ignore them would only earn a CodeQL
  `Useless parameter` finding.
- Why the 3-arg overload: the 5-arg overload only picks one of its two
  thresholds from the flag and delegates here, so every claim check in the
  jar funnels through this one method - the layout filter, the branch-claim
  rejection, `finalizeVillageAssignment`, and the branch-direction spread
  penalty.

### `VillageLayoutPredictorMixin`

- Target: `...worldgen.village.VillageLayoutPredictor.findTargetStructureSet(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/core/Holder;`
  (private static, unique name).
- Shape: `@Inject` at `HEAD`, `cancellable = true`; merges every
  `StructureTagResolver.resolve(level)` target into one synthetic
  `Holder.direct(new StructureSet(merged, first.placement()))`, deduplicated
  by structure id. Skipped below two targets, and when
  `validateAllStructureSets = false`.
- Why it is safe for generation: `findTargetStructureSet` has exactly one
  caller, `computeLayout`, and that reads `set.structures()` only - never
  `set.placement()`. Nothing about how structures actually generate changes;
  only what predicts as confirmable.

### `BiomeSettingsLoaderMixin`

- Target: `...datapack.BiomeSettingsLoader.buildStructureSettings()Lcom/vodmordia/railwaysuntold/datapack/BiomeSettingsLoader$StructureSettings;`
  (private, unique name).
- Shape: `@Inject` at `RETURN`, `cancellable = true`; rebuilds the record
  with `targetStructureTags` appended to `targetTags`, reusing the original
  `avoidanceBlacklist` and `undergroundList` lists by reference.
- Why here and not `resolveStructureSettings`: that method returns
  `cachedStructureSettings`, and `StructureAvoidanceFilter.isBlacklisted`
  feeds its blacklist into `rebuildCacheIfNeeded`, which compares the list by
  reference. A fresh record per call would rebuild those caches every query.
  `buildStructureSettings` runs once per datapack reload (`apply()` nulls the
  cache), so the augmented value is built once and cached like any other.

### `VillageHeadAssignerMixin`

- Target: `...worldgen.head.VillageHeadAssigner.tryAssignVillage(...)Z`
  (public static, unique name). Three `@ModifyConstant` handlers:
  `doubleValue = 100.0`, `intValue = 1000` (two occurrences, both replaced),
  `intValue = 4000`.
- Shape: constants only, no `@At`, no copied control flow. `@Constant`
  matches by value through `Bytecode.isConstant`, so the `sipush` encodings
  are matched as well as `ldc`.
- The `4000` inside `logSyntheticFrontierOnce` is a different method and is
  deliberately untouched.

### `VillageLocatorMixin`

- Target: `...worldgen.village.VillageLocator.findNearestUnassignedVillage(...)L...VillageInfo;`
  (public static, **five overloads**, so the selector carries the full
  descriptor and names only the six-argument one - the one with the
  `Predicate`). Verified to resolve to exactly 1 of the 5 overloads.
- Shape: a single `@ModifyConstant`, `intValue = 100`; the only int `100`
  in that method (the other constant is `Integer.MAX_VALUE`). The handler is
  `private static` because the target is static.
- Why a second spawn gate exists: `tryAssignVillage`'s gate writes a
  permanent `TOO_CLOSE_TO_SPAWN` attempt to the network record, but this one
  is a bare `if (distManhattan < 100) continue` inside the fallback search
  loop - it skips the village for that search only. Leaving it at `100`
  while `minDistanceFromSpawn = 0` would keep silently dropping villages
  near spawn on every fallback, and the config would look broken.
- The five-argument overload is a pure delegate (it passes a `null`
  predicate), so patching the six-argument method covers both entry points.
- The `Objects.requireNonNull(config)` just above the constant reads the
  config and discards it; the distance is the inline `bipush 100`.

### `CoarseRouteFactoryMixin`

- Target: `...worldgen.planner.noise.CoarseRouteFactory` - four redirects,
  each selector verified unique with `javap -p`:
  - `createAndAttachInternal` (private static, single declaration),
    `@Redirect` on `RailwaysUntoldConfig.isAllAvoidanceEnabled()` (the defer
    gate at bytecode offset 667) and on `ScanResult.predictions()` at
    `ordinal = 1` (the defer-overflow commit).
  - `lambda$createAndAttachInternal$9` (private static, single
    declaration), `@Redirect` on `RailwaysUntoldConfig.isAllAvoidanceEnabled()`
    (the route-validation gate at offset 16).
  - `validateRouteAgainstStructures` (private static, single declaration),
    `@Redirect` on `ScanResult.predictions()` at `ordinal = 0` (the fresh
    corridor scan seeded into validation).
- Shape: all four handlers are `private static` (the targets are static) and
  re-enter the original call from the handler body - a different call site,
  so the redirect never re-triggers itself. No `@Shadow`, no copied control
  flow. Config reads are behind `RailwaysUnboundConfigs.isLoaded()` so an
  unloaded SERVER spec falls back to upstream behavior.
- Why: Railways Untold has three routes to laying rail through a structure
  even with `allAvoidance = true`, all closed here behind
  `alwaysAvoidStructures` (see the structure avoidance section below). The
  `predictions()` redirects append one conservative `PredictedStructure` per
  remaining `DeferredCandidate` (set-name based, chunk center at Y=64, no
  footprint so `getStructureRadius` falls back to 48 or the surveyed layout),
  so both planning and `validateRouteAgainstStructures` see structures the
  defer overflow used to drop. Duplicates collapse through the existing
  `structureKey` (chunk + set name) and `mergeStructuresDedupByChunk`.
- Deliberately untouched: the recheck-carry `predictions()` read
  (`ordinal = 0` in `createAndAttachInternal`) and
  `AvoidanceConstraintService.apply` - terrain steering stays under
  Railways Untold's own `allAvoidance`.

### `StructureAvoidanceFilterMixin`

- Target: `...config.StructureAvoidanceFilter.isBlacklisted(Ljava/lang/String;)Z`
  (static, single declaration - the only consumer of the datapack's
  `structure_avoidance_blacklist`).
- Shape: `@Inject` at `HEAD`, `cancellable = true`; returns `false` when
  `alwaysAvoidStructures` is on. The handler declares only the
  `CallbackInfoReturnable` - no `structureId` parameter - for the same
  `checkDescriptor`/`captureArgs = false` reason as
  `VillageAssignmentTrackerMixin`, and to avoid a CodeQL `Useless parameter`
  finding.
- Why: the blacklist means "exempt from avoidance" (default `["wythers:"]`),
  and `tryPredictStructure` early-returns on it, dropping the structure from
  the obstacle set entirely. With the flag on, no structure is exempt on that
  route; the cache fields are never consulted because the handler cancels
  before them.

## Structure avoidance

Railways Untold's `allAvoidance` (default `true`) is meant to keep rails off
structures, but 1.2.2 has four holes. `alwaysAvoidStructures` (default
`true`) closes the first three; the fourth is deliberately kept:

1. **Defer overflow.** Candidates whose chunks have not generated yet are
   deferred (`[ROUTE-DEFER]`, up to six cycles of 5t) while prewarm runs.
   On the seventh cycle the route commits with `ScanResult.predictions()`
   only - the still-unverified candidates are dropped from the obstacle
   list, so neither planning nor `validateRouteAgainstStructures` ever sees
   them (the pack this was measured in logged 2777 defer events). Fixed by
   the two `predictions()` redirects in `CoarseRouteFactoryMixin`.
2. **The `allAvoidance` master gate.** With it off, route validation and
   deferring are both skipped entirely. The two `isAllAvoidanceEnabled()`
   redirects force the structure gates on regardless. Terrain avoidance and
   steering are *not* forced - that is `AvoidanceConstraintService`, left
   alone.
3. **The avoidance blacklist.** `structure_avoidance_blacklist` exempts
   named structures; neutralized by `StructureAvoidanceFilterMixin`.
4. **Underground/buried exemptions.** `structure_underground_list`
   (upstream: ocean ruins, shipwrecks) and fully buried structures are still
   never obstacles - rails may pass over or through them. Kept on purpose:
   detouring around every submerged or buried blob would be absurd.

Already-correct upstream behavior, left as is: the lostcity passthrough
exemptions, the 200 block obstacle removal around the target center (the
station approach), the `target_village_skirt`, and the two-replan-then-
`STRUCTURE_CONFLICT` rejection cap.

`alwaysAvoidStructures` is read live per query and applies to the next
planned route; set it `false` to hand structure avoidance entirely back to
Railways Untold's `allAvoidance`.

## Structure targeting gates

What `[structures]` opens, in the order `tryAssignVillage` applies them:

1. **Angular claim cap.** `VillageHeadAssigner.lambda$buildAngularFilter$1`
   calls `RailNetwork.isClaimAngleTooClose(angle, headId, 25.0)`, which ends
   at `VillageAssignmentTracker.isAngleTooClose`. A second caller,
   `VillageHeadAssigner.finalizeVillageAssignment`, rejects original heads at
   25.0/15.0 (branch heads are exempt). Turning the cap off also stops
   `BranchDirectionSelector.getAngularSpreadPenalty` from adding its +60
   penalty and `ExpansionHeadManager.createBranch` from rejecting a branch's
   village claim - both read the same check.
2. **Forward search.** `findNearestForwardVillageExpanding(level, pos, 1000,
   4000, ...)` - the radius and the limit are inline `sipush` constants.
3. **Unassigned fallback.** `findNearestUnassignedVillage(level, pos, 1000,
   ...)` - the same `sipush 1000`.
4. **Spawn distance.** Two sites, both now on `minDistanceFromSpawn`.
   In `tryAssignVillage`, `horizontalDistance < 100.0` calls
   `markVillageAttempted(TOO_CLOSE_TO_SPAWN)` and returns `false`. The
   attempt is written to the network's persistent record, so a village
   rejected once is rejected forever - the reason `[structures]` defaults this
   gate off rather than merely widening it. In
   `VillageLocator.findNearestUnassignedVillage`, `distManhattan < 100`
   merely `continue`s the fallback loop; it is not recorded, but it would
   still hide villages near spawn on every search, so
   `VillageLocatorMixin` replaces it too.
5. **Layout confirmation.** `VillagePredictor.confirmPrediction` →
   `VillageLayoutPredictor.predict` → `computeLayout`, which pins to
   `StructureTagResolver.resolve(level).get(0)`. Every village in any other
   set fails with `No village variant matched biome`.

`StructureTagResolver.resolve` caches per `ServerLevel` and 1.2.2 has no
caller of its `clearCache()`, so the resolved target list is fixed on first
use per level - the structure tags must be right by then, which is why
`targetStructureTags` reads at datapack-reload time rather than per query.

The targeting pipeline itself is structure-agnostic end to end: the resolved
tags flow through `StructureTagResolver` to the locator (`Holder.is`),
`computeLayout` re-runs vanilla `Structure.generate` at the chunk and tries
every entry of the set until one produces a valid start, `readPlacedLayout`
gates on `matchesTargetTags` and reads real `StructureStart` pieces, and the
footprint, station-plan and station-build stages are all driven by layout
bounds rather than by anything village-specific. The hardcoded
`["minecraft:villages", "minecraft:village"]` label inside
`RouteObstacleScanner.scanStructures` is only a label on the resulting
predictions (it also sets `isVillage`, which *enables* the surveyed-footprint
enrichment for any target). So any structure tag listed in
`targetStructureTags` works without further code - the gates above are the
only thing standing between a pack and a station at, say, an ocean monument.

## Dead code (never build on it)

`RailwaysUntoldConfig`'s instance fields `TRACKS_ENABLED`,
`TRACK_PLACEMENT_DELAY_TICKS`, `BRANCH_MIN_DISTANCE`, `BRANCH_CHANCE`,
`MAX_BLOCKS_WITHOUT_BRANCH`, `VILLAGE_TARGETING_CHANCE`,
`VILLAGE_SEARCH_RADIUS`, `VILLAGE_MIN_DISTANCE_FROM_SPAWN`, the `TUNNEL_*`
and `STATION_CLEARING_*` groups are written by the constructor and read
nowhere in the jar. Frequency and spacing tuning lives entirely in Railways
Untold's own TOML (`maxActiveHeads`, `minBranchSpacing`, `maxBranchDepth`,
`eventChance`, `eventSeparationMinDistance`), which its config screen already
exposes. This addon deliberately does not duplicate those settings.

`VILLAGE_SEARCH_RADIUS` and `VILLAGE_MIN_DISTANCE_FROM_SPAWN` deserve their
own line: the fields are dead, but the numbers they name are live - `1000`,
`4000` and `100.0` are inline constants inside `tryAssignVillage`, `100` is
an inline constant inside `VillageLocator.findNearestUnassignedVillage`,
and those are what `[structures]` replaces. Exposing the fields would do
nothing; replacing the constants is the only route.

Known gap, deliberately not closed here: `searchAndAssignVillageImpl` and
`tryAssignFromPlannerEdges` - the retarget-while-exploring path - carry
their own inline `sipush 1000` (and `sipush 4000`) as the radius they pass
to `findNearestForwardVillageExpanding` and
`findNearestUnassignedVillage`. `searchRadius` and `forwardSearchLimit`
therefore govern the primary path a new head takes, not a head that
retargets mid-explore. Both default to upstream's own numbers, so the gap
is invisible unless the radii are raised.

## Behavior notes

- Generator claims bypass the world-type check for claimed levels. This
  repo's claim predicate excludes the overworld so `generateInWorldTypes`
  stays honored there.
- Denying a dimension stops new bootstrap and orphan rescue there, but never
  edits track that already exists.
- The GameTest run stages the pinned jar through `gametestMods`, so mixin
  application is exercised against the real class in CI - for every target
  class that actually loads. It reports
  `[TRACK-BACKEND] No supported rail mod found - track generation is
  disabled`, so no expansion head ever runs and none of the village
  targeting or route-planning classes (`VillageLayoutPredictor`,
  `VillageHeadAssigner`, `VillageAssignmentTracker`, `VillageLocator`,
  `CoarseRouteFactory`, `StructureAvoidanceFilter`) load there. A bad
  injector in any of them is caught only by loading the real pack and
  grepping `logs/latest.log` for `Failed to apply mixin`.
