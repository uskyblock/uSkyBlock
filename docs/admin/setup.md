# Setup

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

## First setting to check

Config lives in `plugins/uSkyBlock/config.yml`.

The first setting most servers should review is:

| Option | Default | Notes |
|---|---|---|
| `language` | `en` | Server language — see [Crowdin](https://crowdin.com/project/uskyblock-revived) for available translations. Help improve them! |

To apply config changes without a full restart: `/usb reload`

## Upgrading

1. Back up your server, especially `plugins/uSkyBlock/`.
2. Read the release notes for breaking changes.
3. Replace the JAR and start the server. Config migration runs automatically.
4. Backups of the old config are written to `plugins/uSkyBlock/backup/`.

If something goes wrong, stop the server, restore the previous JAR and backup, and start again.

## Next steps

- For gameplay tuning, schematics, biomes, and protection settings, see [Customization](customization.md).
- For challenge progression and rewards, see [Challenges config](challenges.md).
- For command and permission nodes, see [Useful Commands & Permissions](reference.md).
