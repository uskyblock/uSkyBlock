# Live-server integration harness

The live-server harness exercises uSkyBlock against real Paper and Spigot servers. It is separate from the normal Gradle build. GitHub Actions runs the four supported-version lanes (`paper-min`, `paper-max`, `spigot-min`, `spigot-max`) on every push to `master` and on every pull request, runs `paper-canary` nightly, and any lane can be dispatched manually.

## Lanes

The harness runs a `platform × tier` matrix. The Minecraft versions are not pinned in the harness — they are derived from `minecraftVersions` in `gradle.properties` (the same list the Hangar/Modrinth/DevBukkit publish paths use), which is maintained oldest→newest:

- `paper-min` / `spigot-min` — the **first** supported version (the floor, which equals `plugin.yml` `api-version`). Catches old-API regressions.
- `paper-max` / `spigot-max` — the **last** supported version. The deterministic "known-good newest"; bumping it is the same edit as adding a version to `minecraftVersions` for publishing.
- `paper-canary` — the newest **available** Paper (dynamic, via Fill API v3), independent of the supported list, with the newest MCProtocolLib build for it. Early warning for upstream breaks. It runs only on the nightly schedule (or when dispatched), so it never blocks a push or pull request; like every lane, a failure fails its run.

Every lane runs the player-driven scenarios and the restart phase with a presence client (see below); there is no server-only mode. Spigot server jars are compiled on demand with BuildTools (`--rev <version>`) and cached by build number; Paper jars are downloaded and SHA-256 verified.

Lanes pin WorldEdit 7.4.4 and WorldGuard 7.0.17 (recorded in `scripts/ittest/artifacts.json`); the same bukkit jars run on both Paper and Spigot. A newer dependency build can drop support for an older Minecraft version — WorldGuard 7.0.17 declares `api-version 1.21.11` and will not load on 1.21.10, which uSkyBlock hard-depends on — so `artifacts.json` also carries per-Minecraft-version `overrides` that pin an older, compatible build for such lanes (1.21.10 uses WorldGuard 7.0.15). The same mechanism covers a version newer than the base pin: WorldEdit 7.4.4 has no 26.3 adapter, so 26.3 (the `-max` lanes) uses WorldEdit 7.4.6. Overrides are keyed by Minecraft version, not lane, so they follow the version and lapse once the supported floor moves up. Downloads are cached outside each disposable server directory and verified before use.

## Presence client

The presence clients (`UsbItPlayer`, `UsbItVisitor`) are built on MCProtocolLib (MCPL), whose build must speak the server's protocol. MCPL cuts one build per protocol, usually named after the first Minecraft version with that protocol, and that build serves every later version that shares it. So the stable lanes — `-min`, `-max`, the local default, and `--minecraft X` on any of them — take the build from a reviewed table, `mcProtocolLib` in `scripts/ittest/artifacts.json`, chosen by **protocol number**:

| Minecraft | Protocol | MCPL build | Repository |
| --- | ---: | --- | --- |
| 1.21.10 | 773 | `1.21.9-20251210.010914-21` | `maven-snapshots` |
| 1.21.11 | 774 | `1.21.11-1` | `maven-releases` |
| 26.1, 26.1.1, 26.1.2 | 775 | `26.1-1` | `maven-releases` |
| 26.2 | 776 | `26.2` | `maven-releases` |
| 26.3 | 777 | `26.3` | `maven-releases` |

1.21.10 shares protocol 773 with 1.21.9, which has no regular MCPL release, so its row pins the newest 1.21.9 snapshot by its timestamped build. A row names its `repository` only when it is not `maven-releases`; the client resolves the build from that repository, next to `maven-releases` for MCPL's own dependencies.

Only `paper-canary` resolves the build at run time, because the newest Paper may be newer than the table: the highest release build named after its Minecraft version in `maven-releases` (a plain `<mc>` counts as build 0, `<mc>-<n>` as build n, other suffixes are ignored), otherwise the newest `<mc>-SNAPSHOT` in `maven-snapshots`, pinned to its timestamped build so the manifest replays exactly. The lookup goes by name, so it finds no build for a version that MCPL serves under an earlier name (as `26.1-1` serves 26.1.2).

No client found always ends the run as `HARNESS-ERROR`: a Minecraft version without a row, a canary lookup that finds nothing, an MCPL repository that cannot be read, or a replay manifest recorded without player flows (by the former server-only mode).

### Adding a Minecraft version

Adding a version to `minecraftVersions` means adding its row:

1. Read the server's protocol from the `version.json` in Mojang's server jar (the `downloads.server.url` of the version's entry in Mojang's version manifest): `unzip -p server.jar version.json | jq .protocol_version`.
2. Find the MCPL build with that protocol. Its codec names it: `javap -c -classpath protocol-<build>.jar org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec | grep -A3 'static {}'` shows `sipush <protocol>`. Look in `maven-releases` first; snapshots are only in `maven-snapshots`.
3. Add `"<mc>": { "version": "<build>", "protocol": <protocol> }` to `mcProtocolLib`. For a snapshot, pin the timestamped build (`<base>-<yyyyMMdd.HHmmss>-<n>`, from the snapshot's `maven-metadata.xml`), never `-SNAPSHOT`, and add `"repository": "maven-snapshots"`.
4. If no MCPL build has the protocol, add no row: the version's lanes fail until there is one, or until the supported floor moves past it.

`python3 -m unittest discover -s scripts/ittest/tests` checks that every supported version has a row and tests both lookups. The `-min` and `-max` lanes on the pull request are the live check of their rows; the rows in between get one from `--minecraft X` or a dispatched run.

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

Every provisioned run creates `run-manifest.json`, per-phase JSON Lines verdicts, server/client logs, and a stable-identifier `state.properties` file. The manifest records the server platform (paper/spigot), build and (for Paper) URL and hash, dependency URLs, versions and hashes, the MCProtocolLib build and the repository it came from, Git SHA, Java version, fixture revision, lane, and randomized localhost port.

| Exit | Outcome | Meaning |
| ---: | --- | --- |
| `0` | `PASS` | All required verdicts passed and raw logs are clean. |
| `10` | `FAIL` | A uSkyBlock assertion or dependency behavior failed. |
| `20` | `HARNESS-ERROR` | Provisioning (including no presence-client build), client, timeout, malformed result, missing/duplicate/skipped required verdict, or unattributed log failure. |

The fixture player is `UsbItPlayer`, whose offline UUID is derived from `OfflinePlayer:UsbItPlayer`. A second presence client, `UsbItVisitor`, is the player it bans from and locks out of its island. The fixture challenge is `ittest_trade`; the fixture island scheme is `ittest`. Persistence checks resolve these stable identifiers through uSkyBlock services after a real restart and never inspect SQLite directly.

On success, the disposable server directory is removed unless `--keep` is set. Manifests, verdicts, and logs remain. CI always uploads the manifest and verdicts, and uploads the complete runtime directory on non-PASS outcomes.
