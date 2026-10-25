# Contributing

uSkyBlock is open source and relies on community contributions.

Everyone can contribute in their own way, whether they write code or not. If you care about the plugin and want to help improve it, your contribution is welcome :)

## Ideas, bugs, and suggestions

If you have an idea, found a bug, or want to improve the default experience, start by opening an issue:

- [Issues](https://github.com/uskyblock/uSkyBlock/issues)
- [New bug report](https://github.com/uskyblock/uSkyBlock/issues/new?template=bug_report.md)
- [New feature request](https://github.com/uskyblock/uSkyBlock/issues/new?template=feature_request.md)

Good issues are concrete. Explain what is wrong, what you expected instead, and who it affects.

## Translations

Translations live in the repository as gettext `.po` files under `uSkyBlock-Core/src/main/i18n/`. Each language has one file in each of three folders:

- `player_facing/` — messages players see
- `admin_ops/` — messages for server operators
- `system_debug/` — debug and log output

If a translation is wrong, clumsy, or uses the wrong Minecraft term, fix it in a pull request against the `.po` file (editing it on GitHub is fine). Or open an [issue](https://github.com/uskyblock/uSkyBlock/issues) with the language, the current text, and what it should say.

Each entry pairs the English text (`msgid`) with its translation (`msgstr`). Change only the `msgstr`:

```po
msgid "** You are entering <leader>'s island."
msgstr "** Du betrittst die Insel von <leader>."
```

When you edit a translation:

- keep MiniMessage tags (`<primary>`, `<cmd>`, `<newline>`, ...) and placeholders (`<leader>`, `<seconds>`, ...) exactly as in the `msgid` — you may move them within the sentence, but never translate, rename, or drop them
- never change the `msgid`; it is the lookup key
- use the terms from `uSkyBlock-Core/src/main/i18n/glossary.yml`, so words like island, challenge, and party stay consistent

To try a change on a server, copy the file to `plugins/uSkyBlock/i18n/<locale>.po` (for example `de.po`) and restart. Files there override the bundled translations.

For details on how the translation system works internally, see [Translation system](developers.md#translation-system).

## Challenge ideas

Challenge contributions do not have to start as code.

Challenge ideas are very welcome! Useful contributions include:

- proposing new default challenges
- improving rewards
- smoothing difficulty ramps
- improving progression between ranks

If you have a challenge idea, open a [new feature request](https://github.com/uskyblock/uSkyBlock/issues/new?template=feature_request.md).

## Docs

Docs improvements are always welcome too.

The quickest way is to edit files under `docs/` on GitHub and open a pull request. For a local preview, see [Previewing docs locally](developers.md#previewing-docs-locally).

## Code contributions

Code contributions are appreciated too, whether they are small fixes or larger improvements.

Keep it simple:

- one problem per pull request
- add or update tests when behavior changes
- keep user-facing messages and config keys backward-compatible where practical
- describe the operational impact, not just the code change

Public API changes need clear migration notes. When in doubt, open an issue first.
