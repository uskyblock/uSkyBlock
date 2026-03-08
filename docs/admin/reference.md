# Commands & Permissions Reference

## Permission groups

Assign these group nodes where possible rather than individual permissions.

| Node | Default | Who gets it |
|---|---|---|
| `usb.use` | everyone | All normal player commands |
| `usb.social` | everyone | Party chat and social features |
| `usb.mod` | op | Moderator tools (goto, bypass protection) |
| `usb.admin` | op | All admin commands |

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
| `biome` | `usb.island.biome` | Change island biome |
| `lock` / `unlock` | `usb.island.lock` | Lock island to visitors |
| `ban` / `unban` | `usb.island.ban` | Ban player from island |
| `setwarp` | `usb.island.setwarp` | Set warp point |
| `togglewarp` | `usb.island.togglewarp` | Enable/disable warps |
| `warp` | `usb.island.warp` | Warp to island |
| `invite` | `usb.party.invite` | Invite a player |
| `accept` / `reject` | `usb.party.join` | Accept or reject invite |
| `kick` | `usb.party.kick` | Remove a member |
| `leave` | `usb.party.leave` | Leave island party |
| `party` | — | List party members |
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

| Subcommand | Description |
|---|---|
| `reload` | Reload config and language files |
| `island info <player>` | Show island data for a player |
| `island delete <player>` | Delete a player's island |
| `island addmember <island> <player>` | Force-add a member |
| `island purge` | Remove abandoned islands |
| `island setbiome <player> <biome>` | Override island biome |
| `island makeleader <player>` | Transfer island leadership |
| `goto <player>` | Teleport to a player's island (`usb.mod.goto`) |
| `maintenance` | Toggle maintenance mode |
| `topten` | Recalculate the top 10 list |
| `purge` | Remove islands below the configured purge level |
| `cooldown <player>` | View or reset player cooldowns |
| `challenge <player>` | View or reset player challenges |
| `perk <player>` | Manage player perks |
| `flush` | Write cached data to disk |
| `debug` | Toggle debug logging |
| `version` | Show plugin and dependency versions |
