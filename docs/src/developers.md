# Developer Overview

## Architecture

uSkyBlock is a standard Bukkit plugin. The main components:

- **Island management** — creates, stores, and regenerates island regions using WorldEdit schematics and WorldGuard region protection.
- **Party/team system** — handles island membership and per-member permission flags.
- **Challenge system** — evaluates requirements and tracks player completion.
- **Async job queue** — heavy operations (island generation, deletion, chunk regen) run off the main thread.
- **Adventure / MiniMessage** — all user-facing text goes through the Adventure library. Strings are formatted with MiniMessage tags.
- **i18n** — messages are externalized in gettext-style `.po` files, synced with [Crowdin](https://crowdin.com/project/uskyblock-revived). See [Translation system](#translation-system) below.
- **Economy** — optional Vault hook; the plugin degrades gracefully if Vault is absent.

## Building

Requires Java 21. The Gradle wrapper is included.

```bash
./gradlew build
```

Output: `uSkyBlock-Plugin/build/libs/uSkyBlock.jar`

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

### Events

The API fires Bukkit events you can listen to:

- `uSkyBlockEvent` — base event for all uSkyBlock actions
- `uSkyBlockScoreChangedEvent` — island score recalculated (carries the new `IslandScore`)
- `CreateIslandEvent`, `RestartIslandEvent` — island lifecycle
- `MemberJoinedEvent`, `IslandChatEvent` — social actions

Register a listener the standard Bukkit way:

```java
@EventHandler
public void onScoreChanged(uSkyBlockScoreChangedEvent event) {
    IslandScore score = event.getScore();
    // ...
}
```

## Translation system

uSkyBlock uses a custom gettext-based i18n pipeline. All user-facing strings are translatable, formatted with MiniMessage, and managed through [Crowdin](https://crowdin.com/project/uskyblock-revived).

### Writing translatable strings

Use these methods from `I18nUtil` and `Msg`:

| Method | Returns | Use for |
|---|---|---|
| `tr("message")` | `Component` | Translate a string and get a Component back |
| `sendTr(sender, "message")` | void | Translate and send to a player/sender |
| `sendErrorTr(sender, "message")` | void | Same, but styled as an error (red) |
| `marktr("message")` | `String` | Mark a string for extraction without translating it at call time (used for command descriptions) |

Strings use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) syntax. Placeholders are passed as `TagResolver` arguments:

```java
// Simple message
sendTr(player, "Island created successfully.");

// With placeholders — use Placeholder.number() or Placeholder.unparsed()
sendTr(player, "Your rank is: <rank>.", number("rank", rank.getRank(), PRIMARY));

// Error style
sendErrorTr(player, "You must wait <seconds> seconds.", number("seconds", cooldown));

// Mark for extraction only (translated later, e.g. in help text)
super("create|c", "usb.island.create", "?schematic", marktr("create an island"));
```

The `Placeholder` utility class (`us.talabrek.ultimateskyblock.message.Placeholder`) provides helpers for building tag resolvers: `number()`, `unparsed()`, `component()`, and `legacy()`.

Add `// I18N:` comments above tricky strings to give translators context — these are extracted into the POT files automatically.

### How extraction works

The build uses `xgettext` to scan all Java source files for calls to `tr`, `marktr`, `sendTr`, `sendErrorTr`, and `trLegacy`. The extracted strings are split into three domains:

| Domain | POT file | Crowdin | Purpose |
|---|---|---|---|
| `player_facing` | `keys.player_facing.pot` | synced | Messages players see |
| `admin_ops` | `keys.admin_ops.pot` | synced | Messages for server operators |
| `system_debug` | `keys.system_debug.pot` | excluded | Debug/internal logging — AI-translated only |

Domain assignment is based on the source file's package. Classes in `command/admin/` and specific debug classes go to `admin_ops` or `system_debug`; everything else is `player_facing`. See the classification rules in `uSkyBlock-Core/build.gradle.kts`.

POT templates and translated PO files live under `uSkyBlock-Core/src/main/i18n/`.

### Rebuilding translations

**Any time you add, remove, or change a translatable string**, rebuild the templates:

```bash
./gradlew translation
```

This runs the full pipeline: extract strings → split into domains → merge with existing PO files → generate extra locales (Pirate, Kitteh). Commit the updated POT and PO files.

If you only change code without touching translatable strings, this step is not needed.

### Runtime locale resolution

`I18nUtil.initialize(folder, locale)` loads translations at startup from three sources in priority order:

1. `i18n.zip` bundled in the JAR (packaged translations)
2. `po/` resources in the JAR (fallback)
3. `plugins/uSkyBlock/i18n/` on disk (admin overrides)

The server-wide locale is set via `language` in `config.yml`. Admins can switch it at runtime with `/usb lang`.

## Previewing docs locally

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
pip install -r docs/requirements-docs.txt
mkdocs serve -f docs/mkdocs.yml
```

Docs are served at `http://127.0.0.1:8000`.
