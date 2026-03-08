# Setup & Upgrade

## Requirements

**Required:**

- Spigot or Paper (API version 1.21+)
- WorldEdit
- WorldGuard

**Recommended:**

- Vault — for economy integration
- FastAsyncWorldEdit — significantly faster island generation

## Installation

1. Stop the server.
2. Install WorldEdit and WorldGuard (and optionally Vault / FAWE).
3. Drop `uSkyBlock.jar` into `plugins/`.
4. Start the server once — default config files are generated under `plugins/uSkyBlock/`.
5. Stop the server, review the config, then start again.

Check startup: the console should confirm uSkyBlock loaded without dependency errors, and `/island` should work in-game.

## Upgrading

1. Back up your server, especially `plugins/uSkyBlock/`.
2. Read the release notes for breaking changes.
3. Replace the JAR and start the server — config migration runs automatically.
4. Backups of the old config are written to `plugins/uSkyBlock/backup/`.

If something goes wrong, stop the server, restore the previous JAR and backup, and start again.

## Key configuration

Config lives in `plugins/uSkyBlock/config.yml`. The most important options:

| Option | Default | Notes |
|---|---|---|
| `options.general.worldName` | `skyworld` | Name of the skyblock world |
| `options.general.maxPartySize` | `4` | Max players per island |
| `options.island.distance` | `128` | Blocks between island centers |
| `options.island.protectionRange` | `128` | Protection radius per island |
| `options.island.height` | `150` | Spawn height for new islands |
| `options.island.allowPvP` | `deny` | PvP on islands: `deny` or `allow` |
| `options.island.schematicName` | `default` | Starting island schematic |
| `nether.enabled` | `true` | Enable a skyblock nether world |
| `nether.activate-at.level` | `100` | Island level required to unlock nether |

Cooldowns, spawn limits, block limits, biome permissions, and starting chest items are also configurable. The config file is well-commented — read through it before going live.

To apply config changes without a full restart: `/usb reload`

## Other config files

Beyond `config.yml`, two files control gameplay tuning:

- **`challenges.yml`** — defines all challenges, organised by rank. Each challenge has a type (`onPlayer` for items in inventory, `onIsland` for blocks/entities nearby, or `islandLevel` for level thresholds), requirements, and rewards.
- **`levelConfig.yml`** — controls how blocks contribute to island score. You can set per-block values, group similar blocks, apply diminishing returns, and set hard caps. Edit with care — small changes affect every island.

Both files are well-commented. The defaults are a solid starting point.

## Schematics

Island schematics live in `plugins/uSkyBlock/schematics/`. Additional schematics are registered under `island-schemes` in `config.yml` and gated behind permissions using `usb.schematic.<name>`.

### Creating a custom schematic

1. Build your island in-game. Include at least one chest — the chest closest to the island center becomes the player's spawn point.
2. Select the island bounds with WorldEdit and run `//copy`, then `//schem save <name>`.
3. Copy the `.schem` file from `plugins/WorldEdit/schematics/` to `plugins/uSkyBlock/schematics/`.
4. Add a config entry under `island-schemes` in `config.yml` with the schematic name, permission, description, display item, and any limits.
5. `/usb reload` to pick up the changes.

To disable a built-in schematic without deleting it (it would be recreated on restart), set `enabled: false`:

```yaml
island-schemes:
  skySMP:
    enabled: false
```

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

Islands are automatically protected. The `protection.visitors` section in `config.yml` controls what visitors can and cannot do. Key flags:

| Flag | Default | Notes |
|---|---|---|
| `kill-animals` | off | Visitors cannot kill animals |
| `kill-monsters` | off | Visitors cannot kill monsters |
| `villager-trading` | off | Visitors cannot trade |
| `shearing` | off | Visitors cannot shear |
| `item-drops` | on | Visitors can pick up item drops |
| `trample` | off | Visitors cannot trample crops |
| `portal-access` | off | Visitors cannot use portals |

Island members and trusted players bypass these restrictions. Creeper and wither damage, fire spread, and lava flow protection are also configurable under the `protection` section.
