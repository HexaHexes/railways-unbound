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
`RailwaysUntoldAccess`.

## The one mixin

- Target: `com.vodmordia.railwaysuntold.worldgen.placement.TrackPlacementInitializer.onPlayerLoginImpl(Lnet/minecraft/server/level/ServerPlayer;)V`
  (private static, single, no overloads).
- Shape: `@Inject` at `HEAD`, `cancellable = true`; cancels when the live
  dimension gate denies the level. No `@Shadow`, no refmap, gated by
  `requiredMods = ["railwaysuntold"]` in `src/main/templates/META-INF/neoforge.mods.toml`.
- Why a mixin at all: the default path's overworld and world-type checks are
  hardcoded with no public veto. A `GeneratorClaim` can only widen
  generation, never narrow it, so deny semantics for the overworld have no
  API route.

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

## Behavior notes

- Generator claims bypass the world-type check for claimed levels. This
  repo's claim predicate excludes the overworld so `generateInWorldTypes`
  stays honored there.
- Denying a dimension stops new bootstrap and orphan rescue there, but never
  edits track that already exists.
- The GameTest run stages the pinned jar through `gametestMods`, so mixin
  application is exercised against the real class in CI.
