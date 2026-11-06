# AZYRON CLIENT

A real client-side Fabric mod for **Minecraft: Java Edition 1.21.11 only**, branded AZYRON CLIENT, version 1.0, by **JIGNESH GAMING**. It is not a website or a launcher mock-up: the source adds an in-game menu, applies Minecraft video options, draws HUD overlays, and provides a pinned Fabric performance-mod profile.

## Target and compatibility

- Minecraft Java Edition **1.21.11 only** (the mod metadata uses an exact version dependency).
- Fabric Loader **0.20.1**; Java **21 or newer**.
- Fabric API **0.141.6+1.21.11**.
- Client-only; do not install this mod on a dedicated server.
- PojavLauncher is a target for manual installation, but Android GPU/renderer combinations are device-specific and were not runtime-tested in this workspace.

## Included performance stack

The pack pins exact versioned files: its Modrinth releases are tagged for Minecraft 1.21.11/Fabric, and MixinSquared 0.2.0 is pinned from Bawnorton Maven as More Culling's required runtime dependency. No versions are resolved dynamically.

| Mod | Pinned version | Purpose / dependency |
| --- | --- | --- |
| Sodium | 0.8.14 | Rendering engine and frame-time improvements |
| Lithium | 0.21.4 | Game logic optimizations |
| FerriteCore | 8.2.0 | Lower memory use |
| ImmediatelyFast | 1.14.3 | GUI and immediate-mode rendering |
| Entity Culling | 1.11.3 | Avoid rendering hidden entities and block entities; requires Fabric API |
| More Culling | 1.6.2 | Additional visibility culling; requires Cloth Config and MixinSquared |
| MixinSquared | 0.2.0 | Required version used by More Culling's mixin handlers |
| ModernFix-mVUS | 5.26.3-build.2 | 1.21.11-compatible continuation build of ModernFix |
| Dynamic FPS | 3.11.6 | Reduces resource use while unfocused; requires Fabric API |
| Fabric API | 0.141.6+1.21.11 | Required API dependency |
| Cloth Config API | 21.11.153 | Required by More Culling; optional integration for other mods |

Versions, URLs, SHA-1/SHA-512 hashes, and loader dependencies are in [`modrinth.index.json`](modrinth.index.json). Development `runClient` also uses this pinned stack via Modrinth Maven dependencies. The AZYRON mod jar itself does not bundle or shade third-party mods.

## Features

- AZYRON-branded home, FPS Boost, HUD, Mods, and About pages. A title-screen button opens the hub; **Right Shift** opens it in-game by default.
- First-run optimization applies the requested low settings once. Subsequent manual Minecraft video-option changes are not overwritten on startup.
- One-click Potato optimization and Potato, Balanced, and Smooth FPS profiles.
- Configurable FPS, CPS, ping, coordinates, keystrokes, and armor/durability HUD modules. Each module has its own screen-relative anchor, adjustable from the HUD page.
- Mods page reports which pinned performance mods are active in the current instance.
- Lightweight AZYRON branding overlay on Minecraft's level-loading screen.
- Small properties-file config; no background workers, network calls, or FPS promises.

HUD and profile preferences are saved to `config/azyron-client.properties`.

## Default low-graphics options

On the first launch, AZYRON applies the Potato profile and saves the vanilla options:

- Graphics Fast; render distance 2; simulation distance 2
- Clouds Off; particles Minimal; smooth lighting Off
- Entity distance 50%; mipmap levels 0
- VSync Off; max FPS 60; biome blend 0
- Entity shadows Off

The one-click Potato action reapplies these values at any time. Balanced uses a 6/4 chunk-distance pair and a 120 FPS cap; Smooth FPS uses 4/3 chunks and a 60 FPS cap. These are settings profiles, not guarantees of a particular frame rate.

## Build the client jar

Requirements: **JDK 25 to run the pinned Fabric Loom 1.19 snapshot** (the compiled mod targets Java 21 bytecode), internet access to Fabric, Mojang, Modrinth, and Bawnorton Maven repositories, and **Python 3.8+** for packaging. The finished mod and Minecraft 1.21.11 runtime require Java 21 or newer; Pojav can use Java 21.

```bash
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

The remapped Fabric jar is produced at:

```text
build/libs/Azyron-Client-1.0.jar
```

To launch a development instance with the pinned mods:

```bash
./gradlew runClient
```

To create a complete installable Modrinth pack **after** building the jar:

```bash
./gradlew buildMrpack
```

The Modrinth pack is written to `release/Azyron-Client-1.0.mrpack`. Its pinned performance jars download from Modrinth during import; the compiled AZYRON jar is included as an override. Import the `.mrpack` using Prism Launcher or another Modrinth-pack-compatible launcher.

For a self-contained mods-folder ZIP with **all 12 JAR files included**, run:

```bash
./gradlew buildBundleZip
```

This downloads each pinned dependency, checks its SHA-1, SHA-512, and file size, then writes `release/Azyron-Client-1.0-Fabric-1.21.11.zip`. Extract that ZIP into the game directory of a Minecraft 1.21.11 Fabric instance, or copy its `mods/` contents into the instance's `mods/` folder. This ZIP works for standard Fabric installs and the manual PojavLauncher flow.

## PojavLauncher installation (manual)

1. Back up the target instance and any worlds first.
2. In PojavLauncher, create/select a **Minecraft 1.21.11** instance and install Fabric Loader 0.20.1 (or a newer Fabric Loader build that still installs against 1.21.11). Do not choose another Minecraft version.
3. Select a **Java 21** runtime in PojavLauncher. Minecraft 1.21.11 requires Java 21; a different runtime may fail before mod loading.
4. Download the self-contained `Azyron-Client-1.0-Fabric-1.21.11.zip` bundle, extract it into the selected instance's game directory, and merge its `mods/` folder. It contains AZYRON and all 11 pinned dependency jars, including MixinSquared for More Culling. Alternatively, install `Azyron-Client-1.0.jar` with the other exact-version jars listed above; keep one version of each mod only.
5. Launch the Fabric profile. AZYRON applies its first-run settings automatically; open its title-screen button or press Right Shift in-game.
6. If rendering fails, try a Pojav renderer supported by the device/GPU and remove optional performance mods one at a time to isolate driver-specific issues. Sodium depends on working OpenGL support; no renderer or device can be guaranteed here.

Pojav's game-directory path can differ by Android version and instance configuration. Use the selected instance's actual game directory rather than copying jars into a guessed global folder. Never put these client-only mods in a server's `mods` folder.

## Validation status

The source and packaging configuration target the single requested Minecraft version, and the Modrinth index is checked for its exact version and hash fields when creating a pack. This coding workspace does **not** have Java/Gradle installed and cannot reach the Fabric/Mojang/Modrinth Maven endpoints from its shell, so a real Gradle compile, Minecraft launch, and Pojav runtime test could not be truthfully claimed here. The repository includes a GitHub Actions build workflow to run the actual `./gradlew build` and package steps in a Java-enabled CI environment. Fixes should be made if that run reports mapping/API changes; a local build or successful CI run is still required before calling the binary verified.

## Repository map

- `src/client/java/` — Fabric client entrypoint, HUD, UI, profiles, and Mixins
- `src/main/resources/fabric.mod.json` — exact Minecraft/Fabric metadata
- `modrinth.index.json` — pinned 1.21.11 performance stack and checksums
- `scripts/build_mrpack.py` — assembles the pack with the compiled mod jar
- `gradle/` and `gradlew*` — Gradle Wrapper for reproducible builds
