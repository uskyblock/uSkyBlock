# Configuration

uSkyBlock writes and merges config files on startup.

## Important behavior

- Config file structures can change between versions.
- On startup, uSkyBlock attempts to merge old configs with new defaults.
- A backup copy is stored under `uSkyBlock/backup`.

## Safe configuration workflow

1. Keep your server stopped while editing config files.
2. Commit or back up your config folder before every plugin update.
3. After startup, compare merged files with your expected values.
4. Validate changes on a staging server when possible.

## Migration note

Detailed config reference pages are being migrated from the legacy wiki and updated for current versions.
