# uSkyBlock

This is a continually updated and custom version of Talabrek's Ultimate SkyBlock plugin.

We are on [Spigot](https://www.spigotmc.org/resources/uskyblock-revived.66795/). Currently [Open Issues](https://github.com/uskyblock/uSkyBlock/issues)

## Documentation

The official documentation lives in this repository under `docs/` and is published at:

- https://uskyblock.github.io/uSkyBlock/

Useful entry points:

- Server setup: https://uskyblock.github.io/uSkyBlock/admin/setup/
- Server customization: https://uskyblock.github.io/uSkyBlock/admin/customization/
- Challenges config: https://uskyblock.github.io/uSkyBlock/admin/challenges/
- Useful commands and permissions: https://uskyblock.github.io/uSkyBlock/admin/reference/
- Developer overview: https://uskyblock.github.io/uSkyBlock/developers/

## Installation

uSkyBlock requires the following plugins:

* Spigot/Paper
* WorldEdit
* WorldGuard

Optional integrations:

* Vault

## Releases
https://www.spigotmc.org/resources/uskyblock-revived.66795/history

Pre-releases will end in `-SNAPSHOT`, and are considered **unsafe** for production servers.

Releases have a clean version number, have been tested, and should be safe for production servers.

## Building

To build the plugin, you need Java 21 and the project uses Gradle.

```bash
./gradlew build
```

The resulting JAR can be found in `uSkyBlock-Plugin/build/libs/uSkyBlock.jar`.

For API integration, translation workflow, and local docs preview, see:

- https://uskyblock.github.io/uSkyBlock/developers/

## Contributing

- https://uskyblock.github.io/uSkyBlock/contributing/

## Project History

uSkyBlock traces back to the [skySMP plugin](https://dev.bukkit.org/projects/skysmp), which was licensed under GPLv3, and to [rlf/uSkyBlock](https://github.com/rlf/uSkyBlock), an earlier continuation of Talabrek's Ultimate SkyBlock work.

For a time, public continuation of the project was difficult because the Bukkit listing was registered as `All rights reserved`, which complicated sharing and maintenance even as community interest continued.

At the end of 2014, Talabrek explicitly gave his blessing for this repository to continue the project and consolidate the different community branches into a single, officially maintained uSkyBlock codebase.

## License

[GPLv3](http://www.gnu.org/copyleft/gpl.html) - [tl;dr Legal](https://www.tldrlegal.com/l/gpl-3.0)
