# Repo rules — railways-unbound

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in [`neoforge-mod-template`](https://github.com/HexaHexes/neoforge-mod-template)
and pulled in verbatim — **never hand-edit it.** Everything in this file is this
repo's own and is edited freely; keep it aligned with the org rules rather than
restating them.

**Repo:** `HexaHexes/railways-unbound` · **Mod ID:** `railways_unbound` · **Branch:** `neoforge-1.21.1` (the loader line; this is the default branch - no `main`)
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 · **Jar:** `build/libs/railways_unbound-neoforge-1.21.1-<version>.jar`

Worldgen tuning for Railways Untold (vodmordia, closed beta, pinned 1.2.2): a
server config that gates which dimensions its procedural train tracks
bootstrap in and which biomes they route around. Every hook is a public
Railways Untold registry except one documented mixin veto.

**Dependencies**
- Required: Minecraft, NeoForge.
- Optional: Railways Untold 1.2.2+
  (CurseMaven `curse.maven:railways-untold-1470432:8755353`). Gating:
  `railways/RailwaysUntoldBridge` checks `ModList` before loading
  `railways/RailwaysUntoldAccess`, the only class importing Railways Untold
  types. Do not import a third-party type anywhere else.

**Test:** `./gradlew runGameTestServer` — `RailwaysUnboundGameTests` (1 test).
`expectedTests = 1` in `build.gradle` must match the `@GameTest` count. The
GameTest run stages the real Railways Untold jar via `gametestMods`, so the
gated mixin applies against the real target class: a Railways Untold update
that renames `onPlayerLoginImpl` fails CI, not a player's world.

**Gotchas (do not regress them)**
- `railways_untold_file_id` and `railways_untold_mod_version` are one decision
  with the range in `src/main/templates/META-INF/neoforge.mods.toml`. Bump them
  together, after re-verifying every signature listed in
  `docs/RAILWAYS-UNTOLD.md` against the new jar.
- The mixin is the repo's only sanctioned one and targets a private static
  method of a closed-beta mod. Verify the exact descriptor after every pin
  bump with
  `javap -cp <jar> -p com.vodmordia.railwaysuntold.worldgen.placement.TrackPlacementInitializer`.
- The avoidance provider caches the derived zone set for one server tick per
  level, keyed by dimension. The config itself is read live on every query;
  only the zones ride the cache. Never cache config values.
- WHITELIST dimension mode registers a `GeneratorClaim`, and claims bypass
  Railways Untold's world-type check for the levels they match. The claim
  predicate excludes the overworld on purpose so `generateInWorldTypes`
  stays honored there.
- Railways Untold's `BRANCH_*`/`TRACK_*`/`VILLAGE_*` instance fields are dead
  code in 1.2.2 (constructed, never read). Never build a feature on them.

## Scope doctrine — repo additions

**Mixins are sanctioned here, narrowly:** exactly one exists, the login veto
in `TrackPlacementInitializerMixin`. Railways Untold hardcodes its overworld
and world-type checks with no public veto, so deny semantics for the overworld
have no other implementation. New work must prefer the public registries
(`TrackAvoidanceApi.registerProvider`,
`TrackPlacementInitializer.registerGeneratorClaim`,
`ChunkLoadTrackExpander.registerDimensionExtension`,
`TrackPlacementInitializer.registerBootstrapCustomizer`); any additional
mixin needs a new justification written here first. The org rule "public API
only, no mixins" applies to everything else.

## Naming — this repo

Config keys live in `RailwaysUnboundConfigs`; the decision surface is the
pure `worldgen` classes (`DimensionGate`, `BiomeFilter`, `BiomeZoneBuilder`),
which stay Minecraft-free so the JUnit suite covers all of it.

## CI & release model — this repo

Unchanged from the org workflows; no repo-specific steps.
