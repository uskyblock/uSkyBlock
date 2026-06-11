# Setup

## Requirements

**Required:**

- Spigot or Paper (API version 1.21+)
- [WorldEdit](https://dev.bukkit.org/projects/worldedit)
- [WorldGuard](https://dev.bukkit.org/projects/worldguard)

**Recommended:**

- [Vault](https://www.spigotmc.org/resources/vault.34315/) or [VaultUnlocked](https://www.spigotmc.org/resources/vaultunlocked.117277/) — for economy integration
- [FastAsyncWorldEdit](https://www.spigotmc.org/resources/fastasyncworldedit.13932/) — significantly faster island generation

## Installation

1. Stop the server.
2. Install WorldEdit and WorldGuard (and optionally Vault / FAWE).
3. Drop `uSkyBlock.jar` into `plugins/`.
4. Start the server once — default config files are generated under `plugins/uSkyBlock/`.
5. Stop the server, review the config, then start again.

Check startup: the console should confirm uSkyBlock loaded without dependency errors, and `/island` should work in-game.

## World loading and the void generator

uSkyBlock creates the skyblock world (`options.general.worldName`, default `skyworld`)
automatically and attaches its void chunk generator every time the server starts. The
generator association is **not** stored in the world data — whenever the world is loaded
without it, new chunks generate regular vanilla terrain instead of void.

Most setups need no extra configuration. Two setups do:

### The bukkit.yml generator mapping

uSkyBlock registers its generator in the server's `bukkit.yml` automatically when it sets
up its worlds:

```yaml
worlds:
  skyworld:
    generator: uSkyBlock
  skyworld_nether:
    generator: uSkyBlock
```

This entry is the only mechanism the server consults on *every* load path — including when
the skyblock world is the default world (`level-name` in `server.properties`), which loads
before any plugin can attach a generator. uSkyBlock only adds missing entries; it never
overwrites a generator you configured yourself. If your `bukkit.yml` is not writable
(read-only container images, managed hosting), add the entry above manually — adjust the
world names if you changed `worldName`.

Note for default-world setups: the entry takes effect at the *next* start, so the very
first start of a brand-new setup with `level-name` pointing at the skyblock world will
still log the generator warning once — restart and the warning goes away; chunks generated
during that first start keep their vanilla terrain (see recovery below).

### Multiverse

uSkyBlock imports its worlds into Multiverse-Core automatically, including the correct
generator. If a world is registered in Multiverse without a generator (for example from a
manual `/mv import` without `-g uSkyBlock`), uSkyBlock repairs the registration at startup;
the repair takes effect the next time the world is loaded.

### Symptoms and recovery

If the world was ever loaded without the generator, vanilla terrain appears around islands
while the islands themselves look normal, and uSkyBlock logs a severe warning at startup.
Fix the cause as described above, then remove already generated terrain with
`/usb chunk regen <x> <z> <radius>` while standing in the skyblock world — or delete the
affected region files while the server is stopped. Careful: `x`, `z`, and `radius` are
**chunk** coordinates (not block coordinates), and regeneration resets every block in the
affected chunks — including island builds — so keep the radius clear of islands.

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
- For challenge progression and rewards, see [Challenges](challenges.md).
- For the full default config with inline documentation, see [Configuration](config-reference.md).
- For command and permission nodes, see [Commands & Permissions](commands.md).
