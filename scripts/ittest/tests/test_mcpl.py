import json
import os
import re
import subprocess
import tempfile
import unittest
from pathlib import Path

ITTEST = Path(__file__).resolve().parents[1]
ROOT = ITTEST.parents[1]
MCPL = ITTEST / "mcpl.sh"
ARTIFACTS = ITTEST / "artifacts.json"
TIMESTAMPED = re.compile(r"-\d{8}\.\d{6}-\d+$")
PROTOCOL_PATH = "org/geysermc/mcprotocollib/protocol"


def call(function: str, *args: str, repository_root: str | None = None) -> subprocess.CompletedProcess:
    env = dict(os.environ, USER_AGENT="uSkyBlock-ittest-test")
    if repository_root is not None:
        env["MCPL_REPOSITORY_ROOT"] = repository_root
    return subprocess.run(
        ["bash", "-c", f'set -euo pipefail; source "$0"; {function} "$@"', str(MCPL), *args],
        env=env,
        capture_output=True,
        text=True,
        check=False,
    )


def supported_versions() -> list[str]:
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.strip().startswith("minecraftVersions="):
            return [v.strip() for v in line.split("=", 1)[1].split(",") if v.strip()]
    raise AssertionError("gradle.properties has no minecraftVersions")


class PinnedTableTest(unittest.TestCase):
    def test_every_supported_version_has_a_reviewed_row(self) -> None:
        table = json.loads(ARTIFACTS.read_text())["mcProtocolLib"]
        for mc in supported_versions():
            with self.subTest(mc=mc):
                row = table.get(mc)
                self.assertIsNotNone(row, f"no mcProtocolLib row for Minecraft {mc}")
                self.assertIsInstance(row["protocol"], int)
                result = call("mcpl_pinned_build", mc, str(ARTIFACTS))
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertRegex(result.stdout, r"^\S+ maven-(releases|snapshots)\n$")

    def test_snapshot_rows_are_pinned_builds_from_maven_snapshots(self) -> None:
        for mc, row in json.loads(ARTIFACTS.read_text())["mcProtocolLib"].items():
            with self.subTest(mc=mc):
                self.assertFalse(row["version"].endswith("-SNAPSHOT"), "pin the timestamped build")
                repository = row.get("repository", "maven-releases")
                self.assertIn(repository, ("maven-releases", "maven-snapshots"))
                if TIMESTAMPED.search(row["version"]):
                    self.assertEqual(repository, "maven-snapshots")


class PinnedBuildTest(unittest.TestCase):
    def setUp(self) -> None:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.artifacts = Path(directory.name) / "artifacts.json"
        self.artifacts.write_text(json.dumps({"mcProtocolLib": {
            "1.21.10": {"version": "1.21.9-20251210.010914-21", "repository": "maven-snapshots", "protocol": 773},
            "26.1.2": {"version": "26.1-1", "protocol": 775},
        }}))

    def test_prints_the_rows_build_and_repository(self) -> None:
        self.assertEqual(call("mcpl_pinned_build", "26.1.2", str(self.artifacts)).stdout, "26.1-1 maven-releases\n")
        self.assertEqual(call("mcpl_pinned_build", "1.21.10", str(self.artifacts)).stdout,
                         "1.21.9-20251210.010914-21 maven-snapshots\n")

    def test_a_version_without_a_row_fails(self) -> None:
        result = call("mcpl_pinned_build", "26.1.3", str(self.artifacts))
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, "")
        self.assertIn("add a reviewed row", result.stderr)


class LatestBuildTest(unittest.TestCase):
    def setUp(self) -> None:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.repository = Path(directory.name)

    def publish(self, repository: str, *versions: str, snapshot: tuple[str, str, str] | None = None) -> None:
        artifact = self.repository / repository / PROTOCOL_PATH
        artifact.mkdir(parents=True, exist_ok=True)
        listed = "\n".join(f"      <version>{v}</version>" for v in versions)
        (artifact / "maven-metadata.xml").write_text(
            f"<metadata>\n  <versioning>\n    <versions>\n{listed}\n    </versions>\n  </versioning>\n</metadata>\n")
        if snapshot is not None:
            version, timestamp, number = snapshot
            (artifact / version).mkdir()
            (artifact / version / "maven-metadata.xml").write_text(
                f"<metadata>\n  <versioning>\n    <snapshot>\n      <timestamp>{timestamp}</timestamp>\n"
                f"      <buildNumber>{number}</buildNumber>\n    </snapshot>\n  </versioning>\n</metadata>\n")

    def latest(self, mc: str) -> subprocess.CompletedProcess:
        return call("mcpl_latest_build", mc, repository_root=self.repository.as_uri())

    def test_prefers_the_highest_release_build(self) -> None:
        self.publish("maven-releases", "26.4", "26.4-2", "26.4-10", "26.4-feature-custom-entities",
                     "26.4-SNAPSHOT", "26.40")
        self.assertEqual(self.latest("26.4").stdout, "26.4-10 maven-releases\n")

    def test_a_plain_release_counts_as_build_zero(self) -> None:
        self.publish("maven-releases", "26.2", "26.21-1")
        self.assertEqual(self.latest("26.2").stdout, "26.2 maven-releases\n")

    def test_falls_back_to_the_timestamped_snapshot_build(self) -> None:
        self.publish("maven-releases", "1.21.7-1", "1.21.9-feature-custom-entities")
        self.publish("maven-snapshots", "1.21.9-SNAPSHOT", snapshot=("1.21.9-SNAPSHOT", "20251210.010914", "21"))
        self.assertEqual(self.latest("1.21.9").stdout, "1.21.9-20251210.010914-21 maven-snapshots\n")

    def test_no_build_in_either_repository_fails(self) -> None:
        self.publish("maven-releases", "26.3")
        self.publish("maven-snapshots", "26.3-SNAPSHOT")
        result = self.latest("26.4")
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, "")
        self.assertIn("No MCProtocolLib build for Minecraft 26.4", result.stderr)

    def test_an_unreachable_repository_fails(self) -> None:
        result = self.latest("26.3")
        self.assertEqual(result.returncode, 1)
        self.assertIn("Cannot read MCProtocolLib metadata", result.stderr)

        self.publish("maven-releases", "26.3")
        result = self.latest("26.4")
        self.assertEqual(result.returncode, 1)
        self.assertIn("maven-snapshots", result.stderr)

    def test_a_snapshot_without_a_timestamped_build_fails(self) -> None:
        self.publish("maven-releases", "26.3")
        self.publish("maven-snapshots", "26.4-SNAPSHOT")
        snapshot = self.repository / "maven-snapshots" / PROTOCOL_PATH / "26.4-SNAPSHOT"
        snapshot.mkdir()
        (snapshot / "maven-metadata.xml").write_text("<metadata>\n  <versioning/>\n</metadata>\n")
        result = self.latest("26.4")
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, "")
        self.assertIn("no timestamped build", result.stderr)


if __name__ == "__main__":
    unittest.main()
