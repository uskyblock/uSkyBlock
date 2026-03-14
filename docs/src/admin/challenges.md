# Challenges Config

`plugins/uSkyBlock/challenges.yml` controls the challenge menu, unlock progression, requirements, and rewards. See the [default challenges.yml on GitHub](https://github.com/uskyblock/uSkyBlock/blob/master/uSkyBlock-Core/src/main/resources/challenges.yml) for the full file with inline documentation.

If you want your server to feel custom rather than stock, `challenges.yml` is one of the highest-impact files to tune.

## Safe workflow

1. Back up `plugins/uSkyBlock/challenges.yml`.
2. Make a small change at a time.
3. Run `/usb reload` to reload the plugin config.
4. Test in-game with `/challenges`, `/challenges info <name>`, and `/challenges complete <name>`.

If a change does not load as expected, check the server log first. YAML mistakes and invalid item or entity definitions usually show up there.

If you are replacing the default challenge set completely, uncomment the `merge-ignore` block in the file so the defaults are not merged back in on restart.

## Top-level settings

These control overall challenge behavior:

| Key | What it does |
|---|---|
| `allowChallenges` | Enable or disable the challenge system entirely |
| `challengeSharing` | Track progress per `island` or per `player` |
| `broadcastCompletion` | Announce first completions server-wide |
| `requirePreviousRank` | Require earlier ranks before later ranks unlock |
| `rankLeeway` | How many challenges in a rank can be skipped by default |
| `defaultResetInHours` | Default cooldown before repeat requirements reset |
| `radius` | Default scan radius for `onIsland` challenges |
| `repeatLimit` | Default max repeat count; `0` means unlimited |
| `resetChallengesOnCreate` | Reset progress when an island is created or restarted |

Some of these act as defaults and can be overridden per rank or per challenge, especially `rankLeeway`, `defaultResetInHours`, `radius`, and `repeatLimit`.

The file also defines default GUI colors and locked-menu items.

## Rank structure

Challenges are grouped under `ranks:`. Each rank has:

- an internal id such as `Tier1`
- a display `name`
- a menu `displayItem`
- an optional `resetInHours`
- optional `requires` rules
- a `challenges` section

Minimal example:

```yaml
ranks:
  Tier1:
    name: '&7Novice'
    displayItem: cyan_terracotta
    challenges:
      cobblestonegenerator:
        name: '&7Cobble Stone Generator'
        type: onPlayer
        requiredItems:
          - cobblestone:64;+2
        displayItem: cobblestone
        reward:
          text: 3 leather
          items:
            - leather:3
```

Use stable, lowercase ids for challenge names such as `cobblestonegenerator`. Players can see the formatted `name`, but the id is what you maintain and reference.

## Challenge types

uSkyBlock supports three main challenge types:

| Type | Use for | Main requirement field |
|---|---|---|
| `onPlayer` | Items in the player's inventory | `requiredItems` |
| `onIsland` | Blocks or entities near the player on their island | `requiredBlocks`, optionally `requiredEntities` |
| `islandLevel` | Island level milestones | `requiredLevel` |

Notes:

- `onIsland` uses the global `radius` unless the challenge overrides it.
- `islandLevel` challenges depend on the island level data, so players may need to run `/island level` first.
- In current code, `onIsland` challenges behave as one-time challenges even if you configure repeat rewards.

## Common challenge fields

Most challenges use a small subset of fields:

| Key | Use |
|---|---|
| `name` | Display name shown in menus |
| `description` | Extra text shown in challenge details |
| `type` | `onPlayer`, `onIsland`, or `islandLevel` |
| `requiredItems` | Item requirements for `onPlayer` |
| `requiredBlocks` | Block requirements for `onIsland` |
| `requiredEntities` | Entity requirements for advanced `onIsland` challenges |
| `requiredLevel` | Island level threshold for `islandLevel` |
| `requiredChallenges` | Specific challenge ids that must be completed first |
| `displayItem` | Menu icon when available |
| `lockedDisplayItem` | Optional menu icon while locked |
| `resetInHours` | Override repeat reset time for this challenge |
| `repeatLimit` | Override max repeat count |
| `takeItems` | Whether inventory requirements are consumed (default: `true`) |
| `reward` | First-completion rewards |
| `repeatReward` | Rewards for repeats |
| `disabled` | Hide this challenge without deleting it |

`name`, `description`, and reward `text` are player-facing text from `challenges.yml`. They are not translated automatically by the plugin locale or Crowdin integration, so translate or rewrite them yourself if your server is not using English.

## Rewards

`reward:` and `repeatReward:` share the same structure:

| Key | Use |
|---|---|
| `text` | Short reward description shown to players |
| `items` | Items granted on completion |
| `permission` | Permission node granted on completion |
| `currency` | Economy reward if Vault is installed |
| `xp` | Experience reward |
| `commands` | Commands run as `op:` or `console:` |

Available command placeholders: `{player}`, `{playerName}`, `{challenge}`, `{challengeName}`, `{position}`, and `{party}` (runs the command once per island member). Commands also support `{p=0.5}` (probability) and `{d=1000}` (delay in ms) modifiers.

Example:

```yaml
reward:
  text: 8 dirt and 20 coins
  items:
    - dirt:8
    - '{p=0.1}bone:1'
  currency: 20
  xp: 10
  commands:
    - console: give {party} torch 16
```

## Rank progression

There are two ways to gate later content:

- rank-level rules under `requires:`
- per-challenge rules with `requiredChallenges:`

Use rank-level rules when you want a tier to unlock as a group. Use `requiredChallenges` when one challenge should explicitly depend on another.

## Practical advice

- Start by copying and modifying an existing challenge instead of writing one from scratch.
- Keep ids stable once players are using them.
- Prefer changing rewards and counts before changing progression rules.
- Test new `requiredItems` strings carefully, especially if they include item components.
- For advanced syntax such as item components, probability rewards, and entity requirements, use the comments already at the top of `challenges.yml` as the detailed reference.
