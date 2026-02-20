# Upgrading

## Before you upgrade

1. Back up your full server directory.
2. Read release notes for breaking changes.
3. Verify plugin and dependency compatibility with your Minecraft version.

## Upgrade flow

1. Stop the server.
2. Replace the old uSkyBlock JAR with the new JAR.
3. Start the server and allow config migration to run.
4. Review generated backups in `uSkyBlock/backup`.
5. Test core gameplay paths before opening to players.

## Recommended test checklist

- Island creation and reset.
- Level calculation.
- Team invite and coop actions.
- Warps, challenges, and economy integration.
- Admin commands and permissions.

## Rollback plan

If issues are discovered, stop the server and restore both:

- The previous plugin JAR.
- The server and plugin config/data backup from before the upgrade.
