#!/usr/bin/env python3

import argparse
import json
import mimetypes
import sys
import uuid
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin
from urllib.request import Request, urlopen

DEFAULT_GAME_VERSION_TYPE_ID = 1


def parse_version_spec(spec: str) -> list[str]:
    versions: list[str] = []
    seen: set[str] = set()
    for part in spec.split(","):
        version = part.strip()
        if not version or version in seen:
            continue
        versions.append(version)
        seen.add(version)
    if not versions:
        raise ValueError("Provide at least one Minecraft version in --game-version-spec.")
    return versions


def build_changelog(title: str, version: str, body: str) -> str:
    heading = title.strip() or version.strip()
    rendered_body = body.strip()
    if rendered_body:
        return f"# {heading}\n\n{rendered_body}\n"
    return f"# {heading}\n"


def fetch_json(base_url: str, path: str, api_token: str) -> Any:
    url = urljoin(base_url.rstrip("/") + "/", path.lstrip("/"))
    request = Request(
        url,
        headers={
            "Accept": "application/json",
            "User-Agent": "uSkyBlock DevBukkit publisher",
            "X-Api-Token": api_token,
        },
    )
    try:
        with urlopen(request) as response:
            return json.load(response)
    except HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Request to {url} failed with HTTP {exc.code}: {body}") from exc
    except URLError as exc:
        raise RuntimeError(f"Request to {url} failed: {exc.reason}") from exc


def resolve_game_version_ids(
    versions: list[dict[str, Any]],
    requested_names: list[str],
    game_version_type_id: int,
) -> list[int]:
    matches_by_name: dict[str, list[int]] = {}
    for version in versions:
        name = str(version.get("name", ""))
        if name not in requested_names or int(version.get("gameVersionTypeID", -1)) != game_version_type_id:
            continue
        matches_by_name.setdefault(name, []).append(int(version["id"]))

    missing = [name for name in requested_names if name not in matches_by_name]
    if missing:
        available = ", ".join(
            sorted(
                {
                    str(version.get("name", ""))
                    for version in versions
                    if int(version.get("gameVersionTypeID", -1)) == game_version_type_id
                }
            )
        )
        raise ValueError(
            "Missing CurseForge game versions for "
            f"{', '.join(missing)} in dependency type {game_version_type_id}. "
            f"Available Bukkit versions: {available}"
        )

    resolved_ids: list[int] = []
    seen_ids: set[int] = set()
    for name in requested_names:
        for version_id in matches_by_name[name]:
            if version_id in seen_ids:
                continue
            resolved_ids.append(version_id)
            seen_ids.add(version_id)
    return resolved_ids


def encode_multipart_formdata(
    fields: list[tuple[str, str]],
    files: list[tuple[str, str, bytes, str]],
) -> tuple[str, bytes]:
    boundary = f"----uSkyBlock{uuid.uuid4().hex}"
    body = bytearray()

    for name, value in fields:
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8")
        )
        body.extend(value.encode("utf-8"))
        body.extend(b"\r\n")

    for field_name, filename, content, content_type in files:
        body.extend(f"--{boundary}\r\n".encode("utf-8"))
        body.extend(
            (
                f'Content-Disposition: form-data; name="{field_name}"; '
                f'filename="{filename}"\r\n'
            ).encode("utf-8")
        )
        body.extend(f"Content-Type: {content_type}\r\n\r\n".encode("utf-8"))
        body.extend(content)
        body.extend(b"\r\n")

    body.extend(f"--{boundary}--\r\n".encode("utf-8"))
    return f"multipart/form-data; boundary={boundary}", bytes(body)


def upload_release(
    *,
    base_url: str,
    project_id: str,
    api_token: str,
    jar_path: Path,
    display_name: str,
    changelog: str,
    game_version_ids: list[int],
    release_type: str,
) -> int:
    metadata = {
        "changelog": changelog,
        "changelogType": "markdown",
        "displayName": display_name,
        "gameVersions": game_version_ids,
        "releaseType": release_type,
    }
    mime_type = mimetypes.guess_type(jar_path.name)[0] or "application/java-archive"
    content_type, request_body = encode_multipart_formdata(
        fields=[("metadata", json.dumps(metadata))],
        files=[("file", jar_path.name, jar_path.read_bytes(), mime_type)],
    )
    url = urljoin(base_url.rstrip("/") + "/", f"api/projects/{project_id}/upload-file")
    request = Request(
        url,
        data=request_body,
        method="POST",
        headers={
            "Accept": "application/json",
            "Content-Type": content_type,
            "User-Agent": "uSkyBlock DevBukkit publisher",
            "X-Api-Token": api_token,
        },
    )
    try:
        with urlopen(request) as response:
            payload = json.load(response)
    except HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Upload to {url} failed with HTTP {exc.code}: {body}") from exc
    except URLError as exc:
        raise RuntimeError(f"Upload to {url} failed: {exc.reason}") from exc

    file_id = payload.get("id")
    if not isinstance(file_id, int):
        raise RuntimeError(f"Unexpected upload response: {payload!r}")
    return file_id


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--project-id", required=True)
    parser.add_argument("--api-token", required=True)
    parser.add_argument("--file", required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--body-file", required=True)
    parser.add_argument("--game-version-spec", required=True)
    parser.add_argument("--game-version-type-id", type=int, default=DEFAULT_GAME_VERSION_TYPE_ID)
    parser.add_argument("--release-type", default="release", choices=["alpha", "beta", "release"])
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    jar_path = Path(args.file)
    if not jar_path.is_file():
        raise FileNotFoundError(f"Release artifact not found: {jar_path}")

    requested_versions = parse_version_spec(args.game_version_spec)
    changelog = build_changelog(
        title=args.title,
        version=args.version,
        body=Path(args.body_file).read_text(encoding="utf-8"),
    )
    versions = fetch_json(args.base_url, "/api/game/versions", args.api_token)
    game_version_ids = resolve_game_version_ids(
        versions,
        requested_versions,
        args.game_version_type_id,
    )
    file_id = upload_release(
        base_url=args.base_url,
        project_id=args.project_id,
        api_token=args.api_token,
        jar_path=jar_path,
        display_name=args.title.strip() or args.version.strip(),
        changelog=changelog,
        game_version_ids=game_version_ids,
        release_type=args.release_type,
    )
    print(file_id)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(1)
