# Developer Overview

## Architecture

uSkyBlock is a standard Bukkit plugin. The main components:

- **Island management** — creates, stores, and regenerates island regions using WorldEdit schematics and WorldGuard region protection.
- **Party/team system** — handles island membership and per-member permission flags.
- **Challenge system** — evaluates requirements and tracks player completion.
- **Async job queue** — heavy operations (island generation, deletion, chunk regen) run off the main thread.
- **Adventure / MiniMessage** — all user-facing text goes through the Adventure library. Strings are formatted with MiniMessage tags.
- **i18n** — messages are externalized in gettext-style `.po` files, synced with Crowdin. Run `./gradlew translation` to regenerate templates.
- **Economy** — optional Vault hook; the plugin degrades gracefully if Vault is absent.

## Building

Requires Java 21. The Gradle wrapper is included.

```bash
./gradlew build
```

Output: `uSkyBlock-Plugin/build/libs/uSkyBlock.jar`

To regenerate translation templates:

```bash
./gradlew translation
```

## API integration

Add the APIv2 artifact from Maven Central as a provided/compileOnly dependency:

```xml
<!-- Maven -->
<dependency>
  <groupId>ovh.uskyblock</groupId>
  <artifactId>uSkyBlock-APIv2</artifactId>
  <version>LATEST</version>
  <scope>provided</scope>
</dependency>
```

```kotlin
// Gradle
compileOnly("ovh.uskyblock:uSkyBlock-APIv2:LATEST")
```

Declare a soft dependency so uSkyBlock loads first:

```yaml
# plugin.yml
softdepend: [uSkyBlock]
```

Get the API instance after the plugin enables:

```java
UltimateSkyblock api = UltimateSkyblockProvider.getInstance();
```

The legacy `uSkyBlockAPI` interface (artifact `uSkyBlock-Core`) is still available and provides island/player data methods like `getIslandLevel(player)`, `getIslandInfo(player)`, and `getTopTen()`. It is stable but APIv2 is the direction going forward.

## Previewing docs locally

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
pip install -r requirements-docs.txt
mkdocs serve
```

Docs are served at `http://127.0.0.1:8000`.
