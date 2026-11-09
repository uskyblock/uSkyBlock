# Useful Commands & Permissions

This page covers the most useful player and admin commands.

It is intentionally not exhaustive. For the full admin command tree available on your server, use `/usb` in-game.

## Permission groups

Assign these group nodes where possible rather than individual permissions.

| Node | Default | Who gets it |
|---|---|---|
| `usb.use` | everyone | All normal player commands |
| `usb.social` | everyone | Party chat and social features |
| `usb.mod` | no one by default | Moderator tools (goto, bypass protection) |
| `usb.admin` | no one by default | All admin commands |

## Player commands

All player commands use `/island` (alias: `/is`).

| Subcommand | Permission | Description |
|---|---|---|
| `auto` | `usb.island.create` | Teleport to or create island |
| `create` | `usb.island.create` | Create an island |
| `home` | `usb.island.home` | Go to island home |
| `sethome` | `usb.island.sethome` | Set home location |
| `spawn` | `usb.island.spawn` | Go to skyblock spawn |
| `info` | `usb.island.info` | Show island info |
| `level` | `usb.island.level` | Show island level |
| `top` | `usb.island.top` | Top 10 leaderboard |
| `limits` | `usb.island.limit` | Show spawn/block limits |
| `restart` | `usb.island.restart` | Reset island |
| `biome` | `usb.biome.<name>` | Change island biome |
| `lock` / `unlock` | `usb.island.lock` | Lock island to visitors |
| `ban` / `unban` | `usb.island.ban` | Ban player from island |
| `setwarp` | `usb.island.setwarp` | Set warp point |
| `togglewarp` | `usb.island.togglewarp` | Enable/disable warps |
| `warp` | `usb.island.warp` | Warp to island |
| `invite` | `usb.party.invite` | Invite a player |
| `accept` / `reject` | `usb.party.join` | Accept or reject invite |
| `party` | — | Open the party menu and view party info |
| `party invites` | `usb.party.invites` | Show pending invites |
| `party uninvite <player>` | `usb.party.uninvite` | Withdraw an invite |
| `kick` | `usb.party.kick` | Remove a member |
| `leave` | `usb.party.leave` | Leave island party |
| `makeleader` | `usb.island.makeleader` | Transfer leadership |
| `trust` / `untrust` | `usb.island.trust` | Trust a visitor |
| `perm` | `usb.island.perm` | Set member permissions |
| `log` | `usb.island.log` | View island activity log |

Challenges: `/challenges` (alias: `/c`) — requires `usb.island.challenges`

Chat commands:

| Command | Permission | Description |
|---|---|---|
| `/islandtalk` (`/it`) | `usb.island.talk` | Message all island members |
| `/partytalk` (`/ptk`) | `usb.party.talk` | Message party members |

## Notable individual permissions

| Permission | Notes |
|---|---|
| `usb.biome.<name>` | Unlocks a specific biome (`ocean` is on by default) |
| `usb.schematic.<name>` | Unlocks an island schematic choice |
| `usb.exempt.ban` | Player cannot be banned from islands |
| `usb.exempt.cooldown` | Bypass all cooldowns |
| `usb.mod.bypassprotection` | Ignore island protection |
| `usb.mod.goto` | Teleport to any player's island |
| `usb.donor.25/50/75/100` | Tiered donor perks (configure in `config.yml`) |
| `usb.extra.partysize1/2/3` | Grant extra party size slots |

## Admin commands

All admin commands use `/usb`. Requires `usb.admin` or the specific `usb.admin.*` node.

These are the most useful admin commands for day-to-day server management. Additional import, debug, region, and maintenance tooling is available through `/usb`.

| Subcommand | Description |
|---|---|
| `reload` | Reload config and language files |
| `lang [locale]` | Show or change the active plugin language |
| `island info <player>` | Show island data for a player |
| `island delete <player>` | Delete a player's island |
| `island addmember <island> <player>` | Force-add a member |
| `island setbiome <player> <biome>` | Override island biome |
| `island makeleader <player>` | Transfer island leadership |
| `goto <player>` | Teleport to a player's island (`usb.mod.goto`) |
| `maintenance` | Toggle maintenance mode |
| `topten` | Recalculate the top 10 list |
| `purge <days> [level]` | Remove abandoned islands below a level (see below) |
| `cooldown <player>` | View or reset player cooldowns |
| `challenge <player>` | View or reset player challenges (`usb.mod.challenges`) |
| `perk <player>` | Manage player perks |
| `flush` | Write cached data to disk |
| `version` | Show plugin and dependency versions |
| `update` | Stage the newest uSkyBlock version for the next restart (`usb.admin.update`, see [Updates](setup.md#updates)) |

### Purging abandoned islands

`/usb purge <days> [level]` looks for abandoned islands below `level`, or below `options.advanced.purgeLevel` if you
give none, and tells you how many it found. Run `/usb purge confirm` to purge them.

- An island is abandoned when **every** member has been away for more than `<days>` days. One recent member keeps it.
- A member who is online counts as active. A member who never played counts as inactive.
- An island marked with `/usb island ignore <player>` is never purged.
- Right before it deletes an island, the purge checks it again and skips it if it no longer matches.

`/usb purge <days> [level] force` purges right after the scan, without asking first.

`/usb purge stop` stops a running scan or purge after the island it is on.

A purge removes the island's data and its WorldGuard regions, and its members lose the island. The blocks stay in the
world.
