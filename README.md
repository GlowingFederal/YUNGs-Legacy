# YUNG's Legacy

A home for backporting selected YUNG's mods to legacy Forge. This repository currently contains build infrastructure and a neutral bootstrap only. No YUNG's mod features or API have been ported yet.

Future ports are intended to be independently installable. Each mod will package the internal shared functionality it needs; players should not need a separately installed legacy YUNG's API JAR. The temporary `yungslegacybootstrap` mod is only a development skeleton, not a standalone API product.

## Supported production targets

Each target is an independent Gradle environment based on our ExampleMod multiversion template. The root builds portable common code and dispatches target wrappers; Minecraft projects are not root subprojects.

| Minecraft | Loader | Build tooling | Gradle JVM | Main compiler/output | Game JVM |
| --- | --- | --- | --- | --- | --- |
| 1.6.4 | Forge 9.11.1.1345 | Unimined 1.4.1, Gradle 8.13 | JDK 21 | JDK 17, Java 7 | Java 8 |
| 1.7.10 | Forge 10.13.4.1614 | GTNHGradle 2.0.34 / RetroFuturaGradle 2.0.6, Gradle 9.7.1 | JDK 25 | Jabel, Java 17 syntax / Java 8 APIs and bytecode | Java 8 |
| 1.8.9 | Forge 11.15.1.2318 | Unimined 1.4.1, Gradle 8.13 | JDK 21 | JDK 21, Java 8 | Java 8 |
| 1.12.2 | Forge 14.23.5.2859 | Unimined 1.4.1, Gradle 8.13 | JDK 21 | JDK 21, Java 8 | Java 8 |

The 1.6.4 output preserves stock Forge's ASM 4.1 compatibility. Shared production Java must use Java 7 syntax and APIs. Minecraft, Forge, registry, worldgen, configuration, networking, and other platform integration belongs in the version adapters.

## Layout and upstream reference

```text
common/                 Portable production Java and shared resources
versions/1.6.4/         Independent Forge adapter and wrapper
versions/1.7.10/        Independent Forge adapter and wrapper
versions/1.8.9/         Independent Forge adapter and wrapper
versions/1.12.2/        Independent Forge adapter and wrapper
reference/1.16.5/       Intact upstream implementation/behavioral references
build-logic/            Shared versioning, packaging and bytecode checks
scripts/                Wrapper dispatch and production JAR verification
.run/                   Portable IntelliJ build/test/run shortcuts
version.properties     Mod version and next production build number
verification.json      Bootstrap archive and metadata expectations
template.properties    ExampleMod infrastructure baseline revision
```

Late 1.16.5 is the intended primary behavioral baseline for mods available there. The retained upstream projects are:

- `yungs-api`
- `YUNGs-Better-Caves-1.16`
- `YUNGs-Better-Dungeons-forge-1.16`
- `YUNGs-Better-Fortresses-forge-1.16`
- `YUNGs-Better-Mineshafts-forge-1.16`
- `YUNGs-Better-Strongholds-forge-1.16`
- `YUNGs-Extras-forge-1.16`

The reference directory is not compiled, placed on production classpaths, or shipped. There is no 1.16.5 production target. Snapshot directories identify the reference family, not a rewritten upstream version: the retained API checkout still declares Minecraft 1.16.4 in its original Gradle metadata.

## Building and development

Install full JDKs 8, 17, 21 and 25, and Python 3. Set `JAVA8_HOME`, `JAVA17_HOME`, `JAVA21_HOME`, and `JAVA25_HOME` if automatic discovery cannot find them. Set `PYTHON` to a Python executable if `python` (Windows) or `python3` (Unix) is unavailable. The root wrapper uses JDK 21; each target wrapper selects its own build JVM.

From the repository root on Windows:

```powershell
./gradlew.bat buildAll
./gradlew.bat testAll
./gradlew.bat build1_6_4
./gradlew.bat build1_7_10
./gradlew.bat build1_8_9
./gradlew.bat build1_12_2
./gradlew.bat cleanAll
```

Use `./gradlew` on Unix. A target also builds directly with its own wrapper from `versions/<minecraft>/`. The existing `scripts/build-all`, `scripts/build-version`, and `scripts/test-all` helpers are available as PowerShell and shell scripts. `cleanAll` cleans root/common and each production target's generated build outputs; it retains dependency caches, development worlds, and reference files.

`testAll` runs any common tests and tests applicable to each target. The initial skeleton has no unit tests. Tests, cleaning, IDE import, and development runs do not consume production build numbers.

Production output is the normal remapped/reobfuscated JAR under `versions/<minecraft>/build/libs/`, named `yungslegacybootstrap-<mod_version>.<build_number>+mc<minecraft>-forge.jar`. `-dev.jar` files are development artifacts. A successful `buildAll` uses one number across all four targets, verifies every production JAR, and advances the root counter once. Successful individual production builds advance it individually; failed builds retain it. `pruneBuilds` is an optional manual helper to keep the newest production JAR per target.

The neutral bootstrap registers `/yungslegacy`, which reports that no mods have been ported. It registers no blocks, items, structures, or worldgen. `verification.json` checks bootstrap/common classes, shared resources, metadata, manifest/version agreement, bytecode levels, remapping, and absence of upstream reference classes or bundled libraries.

Open the root Gradle project in IntelliJ for common code and aggregate tasks. For Minecraft adapter development, open `versions/<minecraft>/` as a separate Gradle project using the JVM in the table. Generate native development configurations with `genSources genIntellijRuns` for Unimined targets or `setupDecompWorkspace idea` for the GTNH target. Checked-in `.run/` shortcuts can also dispatch the native client/server tasks from the root. Configure `PYTHON` in the IntelliJ environment when needed.

Reference code remains browsable in the file tree, but the root IDEA model excludes it and no production source set includes it. Do not attach reference projects as legacy target modules or dependencies, or mark their directories as production source roots.

## Licensing and provenance

Each upstream project retains its original license and notices under its own reference directory. The repository's original LGPLv3 `LICENSE` is retained at the root and with `yungs-api`; the bootstrap packages that root license. Dungeons and Fortresses retain GPLv3 license files; API, Caves, Mineshafts, Strongholds and Extras retain LGPLv3 files. No reference sources or binaries are included in the bootstrap.

Future ports must retain the corresponding upstream attribution and license context. The API snapshot's publishing metadata labels its license GPLv3 while its license file is LGPLv3; that existing discrepancy is preserved and should be resolved when selecting source for an actual port.
