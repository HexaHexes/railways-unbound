# Railways Unbound

Worldgen tuning for [Railways Untold](https://modrinth.com/mod/railways-untold): control which
dimensions its procedurally generated train tracks appear in, which biomes
they route around, and which villages they can reach for a station, through a
synced server config. Without Railways Untold installed, every option stays
inert.

## What it does

- **Dimension control** for track generation. Keep Railways Untold's default
  overworld-only behavior, whitelist specific dimensions (each whitelisted
  dimension beyond the overworld bootstraps its own track network, so modded
  dimensions work), or blacklist the ones you never want touched.
- **Biome gating** for track placement, which Railways Untold does not offer
  itself. Blacklist biomes or biome tags the tracks must route around, or
  whitelist the only ones they may cross. Heads re-route at the placement
  pipeline, so existing networks are never edited retroactively.
- **Station reach** for village targeting. Railways Untold only confirms a
  village it can pass its own gates for: a 25 degree angular claim cap, one
  structure set per layout prediction, a 100 block spawn exclusion, and two
  hardcoded search radii. This addon opens all four so stations can generate
  at every village, including the modded ones its default tag never lists.
- Everything reads the live server config and applies on `/reload`, with no
  restart.

Railways Untold's own config already covers generation frequency and spacing
(`maxActiveHeads`, `minBranchSpacing`, `maxBranchDepth`, `eventChance`), so
this addon does not duplicate those settings. The "hidden constants" its
community sometimes cites (`BRANCH_CHANCE` and friends) are dead code in 1.2.2,
verified against the shipped jar; exposing them would do nothing. The three
constants `[villages]` does expose are the opposite: live, hardcoded, and read
on every village attempt.

## Configuration

`config/railways_unbound-server.toml`. The file is SERVER config, so values
sync and accept per-world overrides in `<world>/serverconfig/`.

### [dimensions]

| Key | Default | Effect |
| --- | --- | --- |
| `mode` | `OVERWORLD_ONLY` | `OVERWORLD_ONLY` leaves Railways Untold unchanged. `WHITELIST` generates only in the listed dimensions. `BLACKLIST` denies the listed dimensions. |
| `list` | *(empty)* | Dimension ids, e.g. `minecraft:the_nether`. |

In `WHITELIST` mode the overworld must be listed to stay enabled. A
whitelisted dimension beyond the overworld bootstraps its own track network
the first time a player logs in there. The overworld always keeps Railways
Untold's own world-type check, whatever the mode.

### [biomes]

| Key | Default | Effect |
| --- | --- | --- |
| `mode` | `NONE` | `NONE` disables gating. `BLACKLIST` routes tracks around the listed biomes. `WHITELIST` routes them around every biome except the listed ones. |
| `list` | *(empty)* | Biome ids or `#tags`, e.g. `minecraft:deep_ocean`, `#minecraft:is_ocean`. |
| `scanRadiusChunks` | `4` | Chunk radius around each active head rescanned every tick. Smaller is cheaper; blocked chunks outside the ring still block once tracks reach them. |

Gating samples the biome at each head's own elevation and blocks that whole
chunk column, so cave biomes gate tunnels and surface biomes gate open track.
Only loaded chunks are scanned, and the placement-time check always sees
loaded chunks, so nothing places inside a blocked biome either way.

### [villages]

| Key | Default | Effect |
| --- | --- | --- |
| `angularClaimCap` | `false` | `false` lets villages be targeted from any bearing. `true` restores Railways Untold's 25 degree claim cap. |
| `validateAllStructureSets` | `true` | `true` predicts layouts from every structure set the village tag resolves to, so mixed-set packs confirm all their villages. `false` pins prediction to the first set, upstream's behavior. |
| `extraStructureTags` | `["railways_unbound:villages"]` | Extra structure tags merged into Railways Untold's village targets. Tags only, never bare ids; a leading `#` is accepted and stripped. |
| `minDistanceFromSpawn` | `0` | Blocks a village must be from world spawn to be targeted. `0` disables the gate; `100` is upstream. A rejected village is remembered forever, so this gate is permanent. |
| `searchRadius` | `1000` | Block radius of the fallback search for an unassigned village. Railways Untold's own value, exposed. |
| `forwardSearchLimit` | `4000` | Block limit on the forward search for a village the head is already heading toward. Railways Untold's own value, exposed. |

`angularClaimCap`, `validateAllStructureSets` and `minDistanceFromSpawn` are
read live. `extraStructureTags` is read when Railways Untold rebuilds its
structure settings, so it takes effect on world load or `/reload`.

The shipped `railways_unbound:villages` tag lists the 18 village structures
Railways Untold's default `#minecraft:village` misses in a modded pack:
Tensura's dwarf, goblin, lizardmen and orc villages, More Meow Village's
three, Terralith's two fortified villages, and Ice and Fire's pixie village.
Tags are additive and structures a pack does not have are simply skipped, so
a pack without those mods sees no change. Add your own with
`extraStructureTags`.

One side effect worth naming: with `angularClaimCap = false`, Railways
Untold's branch-direction spread penalty (a +60 score for steering toward an
already-claimed bearing) also stops applying, because it reads the same
claim check. That is consistent - the steering exists only to respect the
cap - but it does mean branches spread more freely.

To keep every Railways Untold 1.2.2 gate exactly as shipped, use the
upstream-equivalent preset:

```toml
[villages]
    angularClaimCap = true
    validateAllStructureSets = false
    extraStructureTags = []
    minDistanceFromSpawn = 100
    searchRadius = 1000
    forwardSearchLimit = 4000
```

## Known limitations

- Denying a dimension stops new bootstrap there. A world that already
  generated track keeps it, and Railways Untold keeps maintaining it.
- Railways Untold is a closed beta. This addon pins 1.2.2 and every hook it
  uses was verified against that exact jar, see
  [`docs/RAILWAYS-UNTOLD.md`](docs/RAILWAYS-UNTOLD.md).

## Dependencies

| | Mod | Notes |
| --- | --- | --- |
| Required | Minecraft 1.21.1 | — |
| Required | NeoForge 21.1.259+ | — |
| Optional | [Railways Untold](https://modrinth.com/mod/railways-untold) 1.2.2+ | the only mod this addon tunes; without it every option stays inert |
| Optional | [Create](https://modrinth.com/mod/create) 0.5.1+ | Railways Untold needs it to generate any track at all, but this addon does not |

## In-game config editing

Mods built with this template can be edited in-game with
[Configured](https://www.curseforge.com/minecraft/mc-mods/configured) - install
it like any other mod; it adds no code to this one. Open the mod list, select
a mod, press Config, and change values directly: Configured writes through
NeoForge's config system, so a reload applies the new values without restarting
the game.

## Credits

- Railways Untold by vodmordia: every gate in this addon goes through its
  public registries (avoidance zones, generator claims, dimension
  extensions) plus the documented mixins in
  [`docs/RAILWAYS-UNTOLD.md`](docs/RAILWAYS-UNTOLD.md).
- Built on [NeoForge](https://neoforged.net/).

This addon embeds no third-party assets, textures, models or code, and no
third-party mod binary is bundled, redistributed or committed by this project.

## License

GNU Lesser General Public License v2.1 — see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.
