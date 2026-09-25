# Experimental island-variety levels

The 4.0 variety scorer is available for staging, but it is not yet the default. It rewards a wider building palette instead of the market value of particular materials. Each placed block type contributes the cube root of its count, up to 1,000 blocks. Beyond 1,000, every doubling adds only 0.5 units. The marginal contribution falls as the count grows; air does not count. The sum is divided by `unitsPerLevel` to produce the island level.

For example, ten distinct block types placed once give 10 units; 1,000 blocks of one type also give 10. Ten types used for 100 blocks each give about 46.4 units, so variety still wins when the total number of blocks is the same. A diamond block and a glass block have exactly the same contribution at the same count. Newly introduced Minecraft block types count without an update to `levelConfig.yml`.

To test this on a **copy** of a server, set the following in its complete `plugins/uSkyBlock/levelConfig.yml`:

```yaml
general:
  scoringMode: variety
  variety:
    unitsPerLevel: 1.0
```

The `blocks` section, `default` block value, limits, and diminishing returns in that file are ignored in variety mode. With `scoringMode: legacy` or no mode key, scoring is unchanged. Scheme and island multipliers/offsets still apply after either calculation. The Nether is included only once the overworld score reaches `nether.activate-at.level` (default 100), as before.

This mode is **not yet calibrated for a production 4.0 migration**. Existing islands keep their stored level until rescored; when rescored, their level and leaderboard position can change substantially. Challenge unlock thresholds must be playtested against actual builds and block access. The intended pacing is for the first two challenge ranks to open through normal play, for variety to contribute from rank 3, and for it to matter increasingly in later ranks. Back up world and plugin data before evaluating it on a staging server. Do not use legacy block-value changes to balance variety mode: adjust the overall level scale and challenge gates after measuring representative islands.
