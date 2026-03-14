# Customization

Once uSkyBlock is installed and working, these are the highest-value places to make the server feel distinct from the defaults.

## Main config

Most general customization starts in `plugins/uSkyBlock/config.yml`.
See the [Configuration Reference](config-reference.md) for every available key.

Common settings to review:

| Option | Default | Notes |
|---|---|---|
| `language` | `en` | Plugin language |
| `options.general.worldName` | `skyworld` | Name of the skyblock world |
| `options.general.maxPartySize` | `4` | Max players per island |
| `options.island.default-scheme` | `default` | Default starting island schematic |
| `options.island.chestItems` | *(see config)* | Items placed in the starter chest |
| `options.island.distance` | `128` | Blocks between island centers |
| `options.island.protectionRange` | `128` | Protection radius per island |
| `options.island.height` | `150` | Spawn height for new islands |
| `options.island.allowPvP` | `deny` | PvP on islands: `deny` or `allow` |
| `options.extras.obsidianToLava` | `true` | Right-click obsidian with empty bucket to recover lava |
| `nether.enabled` | `true` | Enable a skyblock nether world |

To apply config changes without a full restart: `/usb reload`

## Schematics

Island schematics live in `plugins/uSkyBlock/schematics/`. Additional schematics are registered under `island-schemes` in `config.yml` and gated behind permissions using `usb.schematic.<name>`.

### Creating a custom schematic

1. Build your island in-game. Include at least one chest. The chest closest to the island center becomes the player's spawn point.
2. Select the island bounds with WorldEdit and run `//copy`, then `//schem save <name>`.
3. Copy the `.schem` file from `plugins/WorldEdit/schematics/` to `plugins/uSkyBlock/schematics/`.
4. Add a config entry under `island-schemes` in `config.yml` with the schematic name, permission, description, display item, and any limits.
5. Run `/usb reload`.

To disable a built-in schematic without deleting it, set `enabled: false`:

```yaml
island-schemes:
  skySMP:
    enabled: false
```

## Biomes

Biome choices are defined in `plugins/uSkyBlock/biomes.yml`.

This file controls:

- which biome options appear in the biome menu
- the display item, name, and description for each biome
- which biome keys are valid for `/island biome`

Access is still controlled by permissions. Grant `usb.biome.<name>` for individual biomes, or `usb.biome.*` for all configured biomes.

## Gameplay tuning files

Beyond `config.yml`, two files are especially important:

- **`challenges.yml`** — defines challenge progression, requirements, and rewards. See [Challenges config](challenges.md).
- **`levelConfig.yml`** — controls how blocks contribute to island score, including block values, diminishing returns, and hard caps.

Both are high-impact tuning files. Change them carefully and test on a staging server first if you are already live.

## Cooldowns and limits

| Option | Default | Notes |
|---|---|---|
| `options.general.cooldownRestart` | `30` s | Cooldown between island restarts |
| `options.general.biomeChange` | `60` s | Cooldown between biome changes |
| `options.island.spawn-limits.animals` | `64` | Per-island animal cap |
| `options.island.spawn-limits.monsters` | `50` | Per-island monster cap |
| `options.island.block-limits.hopper` | `50` | Per-island hopper limit |
| `options.island.block-limits.spawner` | `10` | Per-island spawner limit |

## Visitor protection

Islands are automatically protected. The `protection.visitors` section in `config.yml` controls what visitors can and cannot do.

Key flags:

| Flag | Default | Notes |
|---|---|---|
| `kill-animals` | on | Visitors cannot kill animals |
| `kill-monsters` | on | Visitors cannot kill monsters |
| `villager-trading` | on | Visitors cannot trade with villagers |
| `shearing` | on | Visitors cannot shear animals |
| `item-drops` | on | Visitors cannot drop items |
| `trampling` | on | Visitors cannot trample crops |
| `use-portals` | off | Visitors can use portals |

Island members and trusted players bypass these restrictions. Creeper and wither damage, fire spread, and lava flow protection are also configurable under `protection`.
