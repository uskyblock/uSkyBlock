# Live-server integration harness

The live-server harness exercises uSkyBlock against real Paper and Spigot servers. It is intentionally separate from normal Gradle builds and pull-request checks; GitHub Actions runs it nightly and it can also be dispatched manually.

## Lanes

The harness runs a `platform × tier` matrix. The Minecraft versions are not pinned in the harness — they are derived from `minecraftVersions` in `gradle.properties` (the same list the Hangar/Modrinth/DevBukkit publish paths use), which is maintained oldest→newest:

- `paper-min` / `spigot-min` — the **first** supported version (the floor, which equals `plugin.yml` `api-version`). Catches old-API regressions.
- `paper-max` / `spigot-max` — the **last** supported version. The deterministic "known-good newest"; bumping it is the same edit as adding a version to `minecraftVersions` for publishing.
- `paper-canary` — the newest **available** Paper (dynamic, via Fill API v3), independent of the supported list. Early warning for upstream breaks. On CI this lane is non-blocking: a failure files/updates a `canary`-labelled issue instead of failing the run.

The presence-client (MCProtocolLib) version is **derived** from the target Minecraft version by querying MCProtocolLib's Maven metadata (release builds preferred over snapshots). If no codec exists for a version, that lane runs server-only: boot/setup and secondary smokes remain required and player-driven scenarios plus the restart phase are skipped. Spigot server jars are compiled on demand with BuildTools (`--rev <version>`) and cached by build number; Paper jars are downloaded and SHA-256 verified.

All lanes pin WorldEdit 7.4.4 and WorldGuard 7.0.17 (recorded in `scripts/ittest/artifacts.json`); the same bukkit jars run on both Paper and Spigot. Downloads are cached outside each disposable server directory and verified before use.

## Running locally

The server, client, and classifier must execute on Java 25 (which builds every supported Spigot revision too). Production modules continue to target Java 21 bytecode.

```bash
scripts/ittest/run --lane paper-min
scripts/ittest/run --lane paper-max
scripts/ittest/run --lane spigot-min
scripts/ittest/run --lane spigot-max
scripts/ittest/run --lane paper-canary
```

Useful overrides:

```bash
scripts/ittest/run --lane paper-max --minecraft 26.2 --paper-build 56
scripts/ittest/run --manifest build/ittest/runs/<run>/run-manifest.json
scripts/ittest/run --lane paper-min --keep
```

For driver-only debugging on an already provisioned server, run `uskyblock-ittest run fresh` or `uskyblock-ittest run restart` from the console. Automated runs start from `ServerLoadEvent` and shut the server down when the phase verdicts have been atomically finalized.

## Output and exit codes

Every provisioned run creates `run-manifest.json`, per-phase JSON Lines verdicts, server/client logs, and a stable-identifier `state.properties` file. The manifest records the server platform (paper/spigot), build and (for Paper) URL and hash, dependency URLs, versions and hashes, the derived MCProtocolLib version and whether player flows ran, Git SHA, Java version, fixture revision, lane, and randomized localhost port.

| Exit | Outcome | Meaning |
| ---: | --- | --- |
| `0` | `PASS` | All required verdicts passed and raw logs are clean. |
| `10` | `FAIL` | A uSkyBlock assertion or dependency behavior failed. |
| `20` | `HARNESS-ERROR` | Provisioning, client, timeout, malformed result, missing/duplicate verdict, or unattributed log failure. |

The fixture player is `UsbItPlayer`, whose offline UUID is derived from `OfflinePlayer:UsbItPlayer`. The fixture challenge is `ittest_trade`; the fixture island scheme is `ittest`. Persistence checks resolve these stable identifiers through uSkyBlock services after a real restart and never inspect SQLite directly.

On success, the disposable server directory is removed unless `--keep` is set. Manifests, verdicts, and logs remain. CI always uploads the manifest and verdicts, and uploads the complete runtime directory on non-PASS outcomes.
