# uSkyBlock

[![Build](https://img.shields.io/github/actions/workflow/status/uskyblock/uSkyBlock/build.yml?branch=master&label=build)](https://github.com/uskyblock/uSkyBlock/actions/workflows/build.yml)
[![Version](https://img.shields.io/github/v/tag/uskyblock/uSkyBlock?label=version&sort=semver&color=007EC6)](https://github.com/uskyblock/uSkyBlock/tags)
[![Spigot](https://img.shields.io/badge/plugin-spigot-ED8106)](https://www.spigotmc.org/resources/uskyblock-revived.66795/)
[![Hangar](https://img.shields.io/badge/plugin-hangar-007EC6)](https://hangar.papermc.io/uSkyBlock/uSkyBlock)
[![Docs](https://img.shields.io/badge/docs-site-1F7A52)](https://uskyblock.github.io/docs/)
[![License](https://img.shields.io/badge/license-GPLv3-007EC6)](http://www.gnu.org/copyleft/gpl.html)

uSkyBlock is a Skyblock plugin for Spigot and Paper servers. Build a server around islands, progression, challenges, party play, and deep gameplay customization, backed by more than a decade of project history and continued maintenance for modern Minecraft versions.

uSkyBlock is an open-source Skyblock plugin for Spigot and Paper, with public docs, a public API, and a long community-maintained lineage.

**We need your help on translations!** Join the project on [Crowdin](https://crowdin.com/project/uskyblock-revived) to improve translations in your language.

## Quick Links

- Homepage: https://uskyblock.github.io/
- Docs: https://uskyblock.github.io/docs/
- Releases: https://www.spigotmc.org/resources/uskyblock-revived.66795/history
- Hangar page: https://hangar.papermc.io/uSkyBlock/uSkyBlock
- Spigot page: https://www.spigotmc.org/resources/uskyblock-revived.66795/
- Issues: https://github.com/uskyblock/uSkyBlock/issues
- Contributing: https://uskyblock.github.io/docs/contributing/

## Why uSkyBlock

- Build a Skyblock server around islands, progression, challenges, and party play.
- Customize the experience with gameplay-focused configuration such as `challenges.yml`, biome options, schematics, and server tuning.
- Support a global player base with 53 included locale files and a maintained translation workflow.
- Run a project with more than a decade of lineage and continued maintenance while still targeting modern Minecraft server versions and APIs.
- Extend or integrate with the plugin through a public API and documented developer workflow.

## Installation

uSkyBlock requires the following plugins:

- Spigot/Paper
- WorldEdit
- WorldGuard

Optional integrations:

- Vault

For installation, upgrade guidance, and server setup, start here:

- https://uskyblock.github.io/docs/admin/setup/

## Start Here

- Players: https://uskyblock.github.io/docs/players/
- Server admins: https://uskyblock.github.io/docs/admin/setup/
- Server customization: https://uskyblock.github.io/docs/admin/customization/
- Challenges config: https://uskyblock.github.io/docs/admin/challenges/
- Useful commands and permissions: https://uskyblock.github.io/docs/admin/reference/
- Developers: https://uskyblock.github.io/docs/developers/
- Contributors: https://uskyblock.github.io/docs/contributing/

## Building

To build the plugin, you need Java 21 and the project uses Gradle.

```bash
./gradlew build
```

The resulting JAR can be found in `uSkyBlock-Plugin/build/libs/uSkyBlock.jar`.

For API integration, translation workflow, and local docs preview, see:

- https://uskyblock.github.io/docs/developers/

## Project History

uSkyBlock traces back to the [skySMP plugin](https://dev.bukkit.org/projects/skysmp), which was licensed under GPLv3, and to [rlf/uSkyBlock](https://github.com/rlf/uSkyBlock), an earlier continuation of Talabrek's Ultimate SkyBlock work.

For a time, public continuation of the project was difficult because the Bukkit listing was registered as `All rights reserved`, which complicated sharing and maintenance even as community interest continued.

At the end of 2014, Talabrek explicitly gave his blessing for this repository to continue the project and consolidate the different community branches into a single, officially maintained uSkyBlock codebase.

## License

[GPLv3](http://www.gnu.org/copyleft/gpl.html) - [tl;dr Legal](https://www.tldrlegal.com/l/gpl-3.0)
