#!/usr/bin/env python3
"""Assemble an installable .mrpack with external Modrinth jars and the AZYRON mod jar."""

import argparse
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "modrinth.index.json"
EXPECTED_MOD_ID = "azyron_client"
EXPECTED_MOD_VERSION = "1.0"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", required=True, type=Path, help="Remapped AZYRON CLIENT jar from build/libs")
    parser.add_argument("--output", required=True, type=Path, help="Output .mrpack path")
    args = parser.parse_args()

    jar_path = args.jar.resolve()
    output_path = args.output.resolve()
    if not jar_path.is_file():
        raise SystemExit(f"A built client jar is required first: {jar_path}")

    with zipfile.ZipFile(jar_path) as jar:
        try:
            metadata = json.loads(jar.read("fabric.mod.json"))
        except (KeyError, json.JSONDecodeError) as error:
            raise SystemExit(f"Not a valid Fabric mod jar: {error}") from error
    if (metadata.get("id") != EXPECTED_MOD_ID or metadata.get("version") != EXPECTED_MOD_VERSION
            or metadata.get("depends", {}).get("minecraft") != "1.21.11"):
        raise SystemExit("The jar must be AZYRON CLIENT 1.0 for Minecraft 1.21.11 exactly.")

    index = json.loads(INDEX.read_text(encoding="utf-8"))
    dependencies = index.get("dependencies", {})
    if (index.get("formatVersion") != 1 or index.get("game") != "minecraft"
            or dependencies.get("minecraft") != "1.21.11"
            or dependencies.get("fabric-loader") != "0.20.1"):
        raise SystemExit("Refusing to package: this pack must target Minecraft 1.21.11 and Fabric Loader 0.20.1.")

    seen_paths = set()
    for entry in index.get("files", []):
        path = entry.get("path", "")
        if not path.startswith("mods/") or path in seen_paths:
            raise SystemExit(f"Unexpected or duplicate mod path in pack index: {path}")
        seen_paths.add(path)
        hashes = entry.get("hashes", {})
        if len(hashes.get("sha1", "")) != 40 or len(hashes.get("sha512", "")) != 128:
            raise SystemExit(f"Missing or invalid SHA-1/SHA-512 in pack index: {path}")
        if not isinstance(entry.get("fileSize"), int) or entry["fileSize"] <= 0:
            raise SystemExit(f"Missing or invalid file size in pack index: {path}")
        if not entry.get("downloads") or not all(url.startswith("https://") for url in entry["downloads"]):
            raise SystemExit(f"Missing or invalid HTTPS download URL in pack index: {path}")
        if entry.get("env", {}).get("client") != "required" or entry.get("env", {}).get("server") != "unsupported":
            raise SystemExit(f"Performance mods must be marked client-only: {path}")
    if not seen_paths:
        raise SystemExit("Refusing to package: the pinned performance-mod list is empty.")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = output_path.with_suffix(output_path.suffix + ".tmp")
    try:
        with zipfile.ZipFile(temporary_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as pack:
            pack.writestr("modrinth.index.json", json.dumps(index, indent=2) + "\n")
            pack.write(jar_path, "overrides/mods/Azyron-Client-1.0.jar")
        temporary_path.replace(output_path)
    finally:
        temporary_path.unlink(missing_ok=True)

    print(f"Created {output_path}")
    print("Import this file in Prism Launcher or another Modrinth-pack-compatible launcher.")
    print("For PojavLauncher, use the manual Fabric/mods-folder steps in README.md.")


if __name__ == "__main__":
    main()
