# Player Guide

## Getting your island

Run `/island` to get started — it opens a menu where you can create an island or manage your existing one. You can also use commands directly:

| Command | What it does |
|---|---|
| `/island create [schematic]` | Create a new island (server may offer a choice of starting islands) |
| `/island home` | Teleport to your island |
| `/island sethome` | Move your home point |
| `/island spawn` | Go to the skyblock spawn |

## Island level and ranking

Your island level is based on the blocks you've placed. Higher levels unlock more features (like the Nether).

| Command | What it does |
|---|---|
| `/island level` | Check your island's level |
| `/island top` | Top 10 islands on the server |
| `/island info` | Island details |
| `/island limits` | View spawn and block limits |

## Playing with others

You can share your island with other players. The party size limit is set by your server.

| Command | What it does |
|---|---|
| `/island invite <player>` | Invite someone to join your island |
| `/island accept` / `/island reject` | Accept or decline an invite |
| `/island party` | Open the party menu and view party info |
| `/island party invites` | Show pending invites |
| `/island party uninvite <player>` | Withdraw an invite |
| `/island kick <player>` | Remove a member |
| `/island leave` | Leave someone else's island |
| `/island makeleader <player>` | Hand over island leadership |
| `/island perm <player>` | Adjust a member's permissions |

Chat with your island or party members using dedicated channels:

| Command | What it does |
|---|---|
| `/islandtalk <message>` | Send a message to all island members (alias: `/it`) |
| `/partytalk <message>` | Send a message to your party (alias: `/ptk`) |

You can also trust players to build without them joining:

| Command | What it does |
|---|---|
| `/island trust <player>` | Allow a player to build on your island |
| `/island untrust <player>` | Revoke that access |

## Warps

Let other players visit your island, or visit theirs.

| Command | What it does |
|---|---|
| `/island setwarp` | Set your island's warp point |
| `/island togglewarp` | Open or close your island to visitors |
| `/island warp <player>` | Warp to another player's island |

## Challenges

Run `/challenges` to browse available challenges. Complete them to earn rewards.

| Command | What it does |
|---|---|
| `/challenges` | Browse all challenges |
| `/challenges complete <name>` | Complete a challenge |
| `/challenges info <name>` | See what a challenge requires |

## Protecting your island

Your island is automatically protected from other players. You can fine-tune access:

| Command | What it does |
|---|---|
| `/island lock` / `/island unlock` | Block or allow visitor entry |
| `/island ban <player>` | Ban a specific player from your island |
| `/island unban <player>` | Remove the ban |
| `/island biome <biome>` | Change your island's biome (cooldown applies). Optionally add a radius, `chunk`, or `all` |

## Restarting

`/island restart [schematic]` deletes your island and creates a fresh one. This is permanent. Depending on your server's settings, your inventory may also be cleared.
