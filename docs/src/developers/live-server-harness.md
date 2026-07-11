# Live-server integration harness

The live-server harness exercises uSkyBlock against a real Paper server. It is intentionally separate from normal Gradle builds and pull-request checks; GitHub Actions runs it nightly and it can also be dispatched manually.

## Lanes

- `core-stable` pins Minecraft 1.21.11 to MCProtocolLib 1.21.11-1 and runs the complete fresh/restart lifecycle.
- `latest-canary` resolves the newest stable Paper version through Fill API v3. If `scripts/ittest/protocol-mapping.json` has no exact protocol mapping, boot/setup and secondary smokes remain required and the player flow is reported as unsupported rather than failed.

Both lanes pin WorldEdit 7.4.4 and WorldGuard 7.0.17. Their recorded SHA-256 values live in `scripts/ittest/artifacts.json`. Downloads are cached outside each disposable server directory and are verified before use.

## Running locally

The server, client, and classifier must execute on Java 25. Production modules continue to target Java 21 bytecode.

```bash
scripts/ittest/run --lane core-stable
scripts/ittest/run --lane latest-canary
```

Useful overrides:

```bash
scripts/ittest/run --lane latest-canary --minecraft 26.2 --paper-build 17
scripts/ittest/run --manifest build/ittest/runs/<run>/run-manifest.json
scripts/ittest/run --lane core-stable --keep
```

For driver-only debugging on an already provisioned server, run `uskyblock-ittest run fresh` or `uskyblock-ittest run restart` from the console. Automated runs start from `ServerLoadEvent` and shut the server down when the phase verdicts have been atomically finalized.

## Output and exit codes

Every provisioned run creates `run-manifest.json`, per-phase JSON Lines verdicts, server/client logs, and a stable-identifier `state.properties` file. The manifest records Paper and dependency URLs, versions and hashes, MCProtocolLib mapping, Git SHA, Java version, fixture revision, lane, and randomized localhost port.

| Exit | Outcome | Meaning |
| ---: | --- | --- |
| `0` | `PASS` | All required verdicts passed and raw logs are clean. |
| `10` | `FAIL` | A uSkyBlock assertion or dependency behavior failed. |
| `20` | `HARNESS-ERROR` | Provisioning, client, timeout, malformed result, missing/duplicate verdict, or unattributed log failure. |

The fixture player is `UsbItPlayer`, whose offline UUID is derived from `OfflinePlayer:UsbItPlayer`. The fixture challenge is `ittest_trade`; the fixture island scheme is `ittest`. Persistence checks resolve these stable identifiers through uSkyBlock services after a real restart and never inspect SQLite directly.

On success, the disposable server directory is removed unless `--keep` is set. Manifests, verdicts, and logs remain. CI always uploads the manifest and verdicts, and uploads the complete runtime directory on non-PASS outcomes.
