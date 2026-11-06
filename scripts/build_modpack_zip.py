#!/usr/bin/env python3
"""Create an installation-ready mods-folder ZIP with every pinned jar included."""

import argparse
import hashlib
import json
import tempfile
import time
import urllib.error
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "modrinth.index.json"
EXPECTED_MOD_ID = "azyron_client"
EXPECTED_MOD_VERSION = "1.0"
EXPECTED_MINECRAFT = "1.21.11"
CLIENT_JAR_NAME = "Azyron-Client-1.0.jar"
INSTALL_TEXT = """AZYRON CLIENT 1.0 — Minecraft Java 1.21.11 only
Developer: JIGNESH GAMING

INSTALL
1. Back up the target Minecraft instance and its worlds.
2. Create/select a Minecraft Java 1.21.11 instance with Fabric Loader 0.20.1
   and a Java 21 runtime. Do not use another Minecraft version.
3. Extract this ZIP into that instance's game directory, preserving the
   included mods/ folder. Alternatively, copy every JAR inside mods/ into the
   instance's mods/ folder. Do not put these client mods on a server.
4. Launch the Fabric profile. AZYRON applies the low-graphics first-run
   profile. Open AZYRON CLIENT from the title screen or press Right Shift
   while in-game.

POJAVLAUNCHER
Use the selected instance's actual game directory. Set Java 21 and Minecraft
1.21.11 with Fabric Loader 0.20.1 before copying the mods. OpenGL renderer and
GPU compatibility vary by device; no Pojav renderer/device is guaranteed.

This bundle includes AZYRON CLIENT plus 11 pinned dependency jars: Fabric API,
Cloth Config, Sodium, Lithium, FerriteCore, ImmediatelyFast, More Culling,
MixinSquared, Conditional Mixin, ModernFix-mVUS, and Dynamic FPS. The included
modrinth.index.json records pinned versions and checksums.
"""


def load_and_validate_index():
    index = json.loads(INDEX.read_text(encoding="utf-8"))
    dependencies = index.get("dependencies", {})
    if (index.get("formatVersion") != 1 or index.get("game") != "minecraft"
            or dependencies.get("minecraft") != EXPECTED_MINECRAFT
            or dependencies.get("fabric-loader") != "0.20.1"):
        raise SystemExit("Refusing to package: expected Minecraft 1.21.11 and Fabric Loader 0.20.1.")

    entries = index.get("files", [])
    if len(entries) != 11:
        raise SystemExit(f"Expected the 11 pinned client mods and dependencies; found {len(entries)}.")

    seen_paths = set()
    for entry in entries:
        relative = PurePosixPath(entry.get("path", ""))
        if (not relative.parts or relative.parts[0] != "mods" or ".." in relative.parts
                or str(relative) in seen_paths):
            raise SystemExit(f"Unsafe or duplicate mod path in index: {entry.get('path')}")
        seen_paths.add(str(relative))
        hashes = entry.get("hashes", {})
        if (len(hashes.get("sha1", "")) != 40 or len(hashes.get("sha512", "")) != 128
                or not isinstance(entry.get("fileSize"), int) or entry["fileSize"] <= 0):
            raise SystemExit(f"Invalid hash or file size in index: {relative}")
        if (not entry.get("downloads")
                or not all(url.startswith("https://") for url in entry["downloads"])):
            raise SystemExit(f"Missing HTTPS download URL in index: {relative}")
        if entry.get("env", {}).get("client") != "required" or entry.get("env", {}).get("server") != "unsupported":
            raise SystemExit(f"Bundle only supports client-only mods: {relative}")
    return index, entries


def download_and_verify(entry, destination):
    last_error = None
    for attempt in range(3):
        try:
            request = urllib.request.Request(
                entry["downloads"][0],
                headers={"User-Agent": "AZYRON-CLIENT/1.0 packager"},
            )
            sha1 = hashlib.sha1()
            sha512 = hashlib.sha512()
            size = 0
            with urllib.request.urlopen(request, timeout=90) as response, destination.open("wb") as output:
                while True:
                    chunk = response.read(1024 * 1024)
                    if not chunk:
                        break
                    size += len(chunk)
                    sha1.update(chunk)
                    sha512.update(chunk)
                    output.write(chunk)

            expected = entry["hashes"]
            if size != entry["fileSize"]:
                raise ValueError(f"size mismatch: got {size}, expected {entry['fileSize']}")
            if sha1.hexdigest() != expected["sha1"] or sha512.hexdigest() != expected["sha512"]:
                raise ValueError("SHA-1/SHA-512 mismatch")
            return
        except (OSError, urllib.error.URLError, ValueError) as error:
            last_error = error
            destination.unlink(missing_ok=True)
            if attempt < 2:
                time.sleep(2 ** attempt)
    raise SystemExit(f"Could not download/verify {entry['path']}: {last_error}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", required=True, type=Path, help="Remapped AZYRON CLIENT jar")
    parser.add_argument("--output", required=True, type=Path, help="Output installation bundle ZIP")
    args = parser.parse_args()

    jar_path = args.jar.resolve()
    output_path = args.output.resolve()
    if not jar_path.is_file():
        raise SystemExit(f"A built AZYRON CLIENT jar is required first: {jar_path}")
    try:
        with zipfile.ZipFile(jar_path) as jar:
            metadata = json.loads(jar.read("fabric.mod.json"))
    except (OSError, KeyError, json.JSONDecodeError, zipfile.BadZipFile) as error:
        raise SystemExit(f"Not a valid Fabric mod jar: {error}") from error
    if metadata.get("id") != EXPECTED_MOD_ID or metadata.get("version") != EXPECTED_MOD_VERSION:
        raise SystemExit("The jar must be AZYRON CLIENT 1.0 (fabric.mod.json id/version mismatch).")
    if metadata.get("depends", {}).get("minecraft") != EXPECTED_MINECRAFT:
        raise SystemExit("The AZYRON jar must target Minecraft 1.21.11 exactly.")

    index, entries = load_and_validate_index()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_output = output_path.with_suffix(output_path.suffix + ".tmp")
    try:
        with tempfile.TemporaryDirectory(prefix="azyron-mods-") as scratch:
            scratch_path = Path(scratch)
            downloaded = []
            for entry in entries:
                local_file = scratch_path / PurePosixPath(entry["path"]).name
                print(f"Downloading and verifying {entry['path']}...")
                download_and_verify(entry, local_file)
                downloaded.append((local_file, entry["path"]))

            with zipfile.ZipFile(temporary_output, "w", compression=zipfile.ZIP_DEFLATED,
                                 compresslevel=6) as bundle:
                bundle.write(jar_path, f"mods/{CLIENT_JAR_NAME}")
                for local_file, relative_path in downloaded:
                    bundle.write(local_file, relative_path)
                bundle.writestr("README-INSTALL.txt", INSTALL_TEXT)
                bundle.writestr("modrinth.index.json", json.dumps(index, indent=2) + "\n")
        temporary_output.replace(output_path)
    finally:
        temporary_output.unlink(missing_ok=True)

    print(f"Created complete installation bundle: {output_path}")
    print("Extract it into a Minecraft 1.21.11 Fabric instance's game directory.")


if __name__ == "__main__":
    main()
