# Contributing

## Editing docs

The quickest way: open any file under `docs/` on GitHub and click the pencil icon to edit it directly in the browser, then submit a pull request.

For a local preview before opening a PR, see [Previewing docs locally](developers.md#previewing-docs-locally).

**Writing style:** plain language, short sections, concrete steps. Prefer one clear instruction over multiple hedged alternatives.

## Contributing code

- One problem per pull request.
- Add or update tests when behavior changes.
- Keep user-facing messages and config keys backward-compatible where practical.
- Describe the operational impact in your pull request description — not just what changed, but why and what admins or players might notice.

Public API changes need clear migration notes. When in doubt, open an issue to discuss before writing code.

**Code style:** spaces for indentation, opening braces on the same line, `lowerCamelCase` for methods and variables, `UpperCamelCase` for classes, `ALL_CAPS` for constants. Always use braces around `if`/`else` blocks. Avoid wildcard imports.

## Translating

Translations are managed through [Crowdin](https://crowdin.com). The project has two translation domains synced there:

- **Player-facing strings** — messages players see in-game
- **Admin/ops strings** — messages for server operators

To contribute a translation, join the project on Crowdin and translate directly in the web editor. Completed translations are pulled into the repository automatically.
