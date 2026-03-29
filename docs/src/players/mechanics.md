# Custom Mechanics

uSkyBlock adds a few gameplay mechanics on top of vanilla Minecraft. These are server-configurable, so not every feature listed here may be active on your server.

## Guardian habitats

Skyblock worlds don't have ocean monuments, but you can still obtain guardians by building your own habitat.

Place **prismarine**, **prismarine bricks**, or **dark prismarine** blocks in a deep ocean biome on your island. When water mobs would normally spawn beneath these blocks, there is a chance a guardian spawns instead.

!!! note
    This is a building mechanic, not natural spawning. Guardians will not appear in deep ocean biomes without prismarine blocks above the spawn location.

Requirements:

- The biome must be one of: deep ocean, deep cold ocean, deep frozen ocean, or deep lukewarm ocean. Use `/island biome` to set your island's biome.
- A prismarine block must be directly above the spawn point (acts as the "roof" of your habitat).
- Your island's monster spawn limit must not be full. Check with `/island limits`.

The spawn chance and per-island guardian cap are set by the server.

## Nether terraforming

In the skyblock nether, breaking blocks with a pickaxe can generate new blocks nearby. This is the primary way to expand and gather nether resources on a sky island.

When you break a block, there is a chance a new block spawns in a nearby valid location. What spawns depends on what you broke:

| Block broken | Can generate |
|---|---|
| Netherrack | Netherrack, nether quartz ore, soul sand |
| Nether quartz ore | Nether quartz ore |
| Soul sand | Soul sand, gravel |
| Gravel | Gravel, soul sand |
| Glowstone | Glowstone |

### Tool tiers matter

Your pickaxe material affects the probability of block generation:

| Pickaxe | Effectiveness |
|---|---|
| Wood | No terraforming |
| Stone | Normal |
| Iron | Slightly reduced |
| Gold | Best (1.5x) |
| Diamond | Heavily reduced |
| Netherite | Good (1.2x) |

!!! tip
    Gold pickaxes are the most effective for terraforming despite their low durability. Consider bringing several.

### Nether mob replacement

When a zombified piglin spawns on nether bricks (fortress walkways), it has a chance to be replaced by a more dangerous mob:

| Mob | Chance |
|---|---|
| Wither skeleton | 20% |
| Blaze | 20% |
| Skeleton | 10% |
| Zombified piglin | 50% (no change) |

This gives you a way to farm wither skulls and blaze rods without needing a full nether fortress.

## Spawn limits

Islands have per-type mob caps for animals, monsters, villagers, and golems. These prevent lag from excessive mob farms and apply to all spawn sources.

Run `/island limits` to see your island's current limits and usage.
