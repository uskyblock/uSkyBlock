# Installation

## Requirements

uSkyBlock depends on:

- Spigot or Paper
- Vault
- WorldEdit
- WorldGuard

## Install steps

1. Stop your Minecraft server.
2. Install or update all required dependencies.
3. Place the `uSkyBlock.jar` file in your server `plugins/` folder.
4. Start the server once so default config files are generated.
5. Stop the server and review config files before going live.
6. Start the server again and verify startup logs contain no dependency or config errors.

## First startup checklist

- Confirm `uSkyBlock` loaded successfully in server logs.
- Confirm `/usb` command is available in-game.
- Verify economy and region integrations (Vault and WorldGuard) are active.

## Troubleshooting

- Missing dependency errors: verify plugin versions and load order.
- Config merge warnings after update: compare generated files and backups in `uSkyBlock/backup`.
