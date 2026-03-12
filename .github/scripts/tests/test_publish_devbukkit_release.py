import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

import publish_devbukkit_release as publish_devbukkit_release


class ParseVersionSpecTest(unittest.TestCase):
    def test_parse_version_spec_splits_and_deduplicates(self) -> None:
        self.assertEqual(
            publish_devbukkit_release.parse_version_spec("1.21.10, 1.21.11,1.21.10"),
            ["1.21.10", "1.21.11"],
        )

    def test_parse_version_spec_rejects_empty_input(self) -> None:
        with self.assertRaisesRegex(ValueError, "at least one Minecraft version"):
            publish_devbukkit_release.parse_version_spec(" , ")


class BuildChangelogTest(unittest.TestCase):
    def test_build_changelog_includes_heading_and_body(self) -> None:
        self.assertEqual(
            publish_devbukkit_release.build_changelog("3.0.0", "3.0.0", "Fixes\n\nMore fixes"),
            "# 3.0.0\n\nFixes\n\nMore fixes\n",
        )

    def test_build_changelog_falls_back_to_version_when_title_is_blank(self) -> None:
        self.assertEqual(
            publish_devbukkit_release.build_changelog(" ", "3.0.0", ""),
            "# 3.0.0\n",
        )


class ResolveGameVersionIdsTest(unittest.TestCase):
    def test_resolve_game_version_ids_collects_all_exact_matches(self) -> None:
        versions = [
            {"id": 157, "gameVersionTypeID": 42, "name": "1.21.10"},
            {"id": 158, "gameVersionTypeID": 43, "name": "1.21.10"},
            {"id": 159, "gameVersionTypeID": 42, "name": "1.21.11"},
        ]

        self.assertEqual(
            publish_devbukkit_release.resolve_game_version_ids(
                versions,
                ["1.21.10", "1.21.11"],
            ),
            [157, 158, 159],
        )

    def test_resolve_game_version_ids_reports_missing_versions(self) -> None:
        versions = [{"id": 157, "gameVersionTypeID": 42, "name": "1.21.10"}]

        with self.assertRaisesRegex(ValueError, "1.21.11"):
            publish_devbukkit_release.resolve_game_version_ids(
                versions,
                ["1.21.11"],
            )


if __name__ == "__main__":
    unittest.main()
