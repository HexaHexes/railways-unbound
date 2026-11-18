# Railways Unbound

Worldgen tuning for [Railways Untold](https://modrinth.com/mod/railways-untold): control which
dimensions its procedurally generated train tracks appear in and which biomes
they route around, through a synced server config. Without Railways Untold
installed, every option stays inert.

## What it does

- **Dimension control** for track generation. Keep Railways Untold's default
  overworld-only behavior, whitelist specific dimensions (each whitelisted
  dimension beyond the overworld bootstraps its own track network, so modded
  dimensions work), or blacklist the ones you never want touched.
- **Biome gating** for track placement, which Railways Untold does not offer
  itself. Blacklist biomes or biome tags the tracks must route around, or
  whitelist the only ones they may cross. Heads re-route at the placement
  pipeline, so existing networks are never edited retroactively.
- Everything reads the live server config and applies on `/reload`, with no
  restart.

Railways Untold's own config already covers generation frequency and spacing
(`maxActiveHeads`, `minBranchSpacing`, `maxBranchDepth`, `eventChance`), so
this addon does not duplicate those settings. The "hidden constants" its
community sometimes cites (`BRANCH_CHANCE` and friends) are dead code in 1.2.2,
verified against the shipped jar; exposing them would do nothing.

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
  extensions) plus one documented login-hook mixin.
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
