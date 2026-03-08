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

## Schematics

Island schematics live in `plugins/uSkyBlock/schematics/`. Additional schematics can be registered under `island-schemes` in `config.yml` and gated behind permissions using `usb.schematic.<name>`.

## Cooldowns and limits

| Option | Default | Notes |
|---|---|---|
| `options.general.cooldownRestart` | `30` s | Cooldown between island restarts |
| `options.general.biomeChange` | `60` s | Cooldown between biome changes |
| `options.island.spawn-limits.animals` | `64` | Per-island animal cap |
| `options.island.spawn-limits.monsters` | `50` | Per-island monster cap |
| `options.island.block-limits.hopper` | `50` | Per-island hopper limit |
| `options.island.block-limits.spawner` | `10` | Per-island spawner limit |
