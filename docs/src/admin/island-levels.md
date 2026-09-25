# Island levels

Island level is calculated from placed blocks. Leaderboard position is the island's rank relative to other islands; challenge ranks use the calculated level as one of their unlock conditions.

Scoring settings live in `plugins/uSkyBlock/levelConfig.yml`. If the file is absent, uSkyBlock uses the copy bundled in the plugin. To customize it, extract the bundled file first, then edit that copy: a partial file does not inherit the bundled block values.

## Calculation order

1. Count blocks, grouping any `additionalBlocks` with their configured base material.
2. Apply each group's block value, limits, and diminishing or negative returns to obtain points.
3. Convert the combined points to an island level using the progression curve.
4. Apply scheme and island multipliers, then their level offsets, as in legacy scoring.

Nether blocks join the same counts once the overworld alone reaches `nether.activate-at.level` (default 100), before multipliers and offsets. The curve is applied to the combined total; the Nether does not receive a separate curve or separate block limits.

`/is info` shows each block group's proportional contribution to the level before multipliers and offsets. With a nonlinear curve, this is a share of the total, not the exact gain from placing another block. `/is level` refreshes the stored level used by the leaderboard, API, and challenge gates.

## Progression curve

The default remains linear. An existing configuration without `levelProgression` keeps its current calculation and `pointsPerLevel` value. No existing configuration file is rewritten.

For a staging playtest, an example nonlinear configuration is:

```yaml
general:
  pointsPerLevel: 1000
  levelProgression:
    linearUntilLevel: 100
    exponent: 1.5
```

Insert this section into the complete level configuration, preserving its block settings. The example is a calibration starting point, not a finalized 4.0 balance profile.

Up to level 100, the example still costs 1,000 points per level. Above 100, the points needed for a target level are:

```text
points = 1000 × 100 × (level / 100)^1.5
```

| Target level | Legacy points | Example curve points, approximately |
|---|---:|---:|
| 20 | 20,000 | 20,000 |
| 100 | 100,000 | 100,000 |
| 200 | 200,000 | 282,843 |
| 400 | 400,000 | 800,000 |
| 600 | 600,000 | 1,469,694 |

Set `exponent: 1.0` for linear scoring. Exponents must be finite numbers of at least 1; `pointsPerLevel` and `linearUntilLevel` must be finite and positive. Negative point totals retain the linear penalty behavior.

## Variety and repeated blocks

The progression curve makes later levels more expensive. It does not by itself reward variety: an island worth more points still receives a higher level. Use per-group diminishing returns and limits to control repeated high-value blocks. Blocks listed together under `additionalBlocks` share these controls.

For example, with a value of 10 and `diminishingReturns: 100`, the first 100 blocks contribute 1,000 points, while 300 contribute 2,000. Placing 100 blocks in each of ten equally valued groups therefore earns more than stacking 1,000 in one group. `limit` stops further point gains altogether; `negativeReturns` can reduce the score and should be reserved for intentional penalties.

## Existing islands and calibration

Changing the curve affects levels on the next calculation; it does not immediately rescore every island. An enabled nonlinear curve can lower existing levels and relock level-gated challenges. Until persistent challenge-rank unlocks and the complete 4.0 migration are available, use nonlinear settings on a staging copy with matched challenge thresholds.

Compare ordinary builds and single-material stacks using the same configuration. Include starter islands, Nether access, and later challenge builds; verify both active playtime and waiting on resource rewards. Rescore all comparison islands before judging leaderboard order. Preserve a backup of worlds, plugin data, and both progression configuration files before deploying a calibrated change.
