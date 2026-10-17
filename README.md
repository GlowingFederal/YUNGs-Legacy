# YUNG's Legacy

A home for backporting selected YUNG's mods to legacy Forge. Better Caves terrain generation is implemented on Forge 1.7.10 and 1.12.2. Native 1.6.4 and 1.8.9 tooling is retained as inactive templates for future ports.

Each mod lives under `mods/<product>/` and is independently installable. The root coordinates builds; it does not produce a Minecraft mod. Better Caves needs only the matching Forge installation, with no YUNG's API or repository bootstrap JAR.

## Supported production targets

Each product owns its portable code, resources, version, metadata, and isolated native target projects. Minecraft targets are not forced into the root Gradle environment.

| Product | Minecraft | Loader | Build tooling | Gradle JVM | Compiler/output | Game JVM |
| --- | --- | --- | --- | --- | --- | --- |
| Better Caves | 1.7.10 | Forge 10.13.4.1614 | GTNHGradle 2.0.34 / RetroFuturaGradle 2.0.6, Gradle 9.7.1 | JDK 25 | Jabel, Java 17 syntax / Java 8 APIs and bytecode | Java 8 |
| Better Caves | 1.12.2 | Forge 14.23.5.2859 | Unimined 1.4.1, Gradle 8.13 | JDK 21 | JDK 21, Java 8 | Java 8 |

The retained 1.6.4 template uses Unimined 1.4.1 / Gradle 8.13 on JDK 21, compiles with JDK 17 to Java 7, and preserves stock Forge ASM 4.1. The 1.8.9 template uses Unimined 1.4.1 / Gradle 8.13 on JDK 21 and outputs Java 8. Both use Java 8 for the game. They are not implemented Better Caves releases and are excluded from production build/test tasks.

Portable Better Caves code has no Minecraft or loader imports. Worldgen, configuration, lifecycle, and native block integration belong in its version projects.

## Better Caves

On 1.7.10 and 1.12.2, Better Caves replaces ordinary overworld cave carving with cubic and OpenSimplex cave regions, liquid and floored caverns, surface tunnels, and configurable vanilla cave regions. Ravines retain native tunnel geometry with Better Caves liquid and ocean handling. Noise uses global coordinates and world seeds; terrain need not match across Minecraft versions. Existing chunks are unaffected.

Settings use native Forge `config/bettercaves.cfg`, loaded when a world first generates terrain. Restart the world after changes. The default dimension whitelist is `[0]`; exclusions `[-1, 1]` take precedence over `globalDimensions`. An optional `config/bettercaves/dimension-<numeric-id>.cfg` overrides global defaults for that dimension. Modern TOML/preset files are not consumed. Defaults include flooded oceans, surface caves, flattened bedrock, liquid altitude 10, 40% water regions, and 25% cavern regions. Each cave/cavern category exposes altitude, priority, compression, noise thresholds, generator count and fractal settings; `openSimplex=true` uses the reference ridged OpenSimplex composition, while `false` enables the selectable FastNoise modes. Region sizes are expressed as frequencies (lower means larger regions).

Disabled or excluded dimensions retain the original cave and ravine generators. Support is deliberately limited to native overworld generators (`ChunkProviderGenerate` / `ChunkGeneratorOverworld`); custom provider classes and other mods' replacement cave/ravine generators retain their own generation. Flat worlds and Nether/End providers are not converted. Install only one Better Caves implementation for a given Minecraft version. On 1.7.10, absent magma, andesite and red sandstone use obsidian, stone and sandstone respectively. The bottom bedrock boundary at y=0 is retained even when configured layer width is zero. `/bettercaves` (with `/yungslegacy` retained as an alias) reports the installed mod; it does not report whether the current dimension enables it.

## Layout and upstream reference

```text
mods/better-caves/
  common/               Portable Better Caves code, resources, notices and any shared tests
  versions/1.7.10/       Native Forge project, wrapper, metadata and development runs
  versions/1.12.2/       Native Forge project, wrapper, metadata and development runs
  LICENSE               Better Caves distribution license
  version.properties    Product version and next production build number
  verification.json     Product archive and metadata expectations
build-logic/            Reusable build/version/bytecode infrastructure
  target-templates/     Inactive 1.6.4 and 1.8.9 native projects and prototype scaffolding
reference/1.16.5/       Intact upstream behavioral and provenance references
scripts/                Product discovery, wrapper dispatch and production JAR verification
.run/                   IntelliJ build/test/run shortcuts
```

Future mods receive their own `mods/<product>/common`, `versions`, version file, license and verification manifest. Only genuinely reusable build infrastructure belongs at the root; product helpers are privately namespaced and included in that product's JAR. Inactive templates retain their original prototype sources for later adaptation and are excluded from the production IDE model and release discovery.

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
./gradlew.bat buildBetterCaves
./gradlew.bat testBetterCaves
./gradlew.bat buildBetterCaves1_7_10
./gradlew.bat buildBetterCaves1_12_2
./gradlew.bat cleanAll
```

Use `./gradlew` on Unix. A native target builds directly with its own wrapper from `mods/better-caves/versions/<minecraft>/`. The existing `scripts/build-all`, `scripts/build-version`, and `scripts/test-all` helpers remain available as PowerShell and shell scripts. The Python dispatcher also accepts `--mod better-caves`, for example `python scripts/build.py build 1.12.2 --mod better-caves`. When several products exist, select a product for individual target builds or runs; aggregate actions discover all product verification manifests.

`cleanAll` cleans active products' generated build outputs and retains dependency caches, development worlds, references and inactive templates. `testAll` runs product common and native target test tasks. No test sources currently exist; archive assertions run as part of coordinated production builds. Tests, cleaning, IDE import, and development runs do not consume production build numbers.

Production output is the remapped/reobfuscated JAR under `mods/better-caves/versions/<minecraft>/build/libs/`, named `better-caves-<mod_version>.<build_number>+mc<minecraft>-forge.jar`. `-dev.jar` files are development artifacts. Every product owns its own version counter. A successful aggregate build uses one number across that product's implemented targets, verifies all of its production JARs, and advances its counter once. Successful individual production builds advance the product counter individually; failed builds retain it. `pruneBuilds` is an optional manual helper to keep the newest production JAR per product/target.

Product `verification.json` and the dispatcher check the sole Better Caves entry point, metadata, notices, Java 8 classfiles, remapped native hooks, version agreement, and absence of generic bootstrap classes, YUNG's API dependencies, upstream source, unrelated resources, or bundled libraries. Better Caves currently uses native Forge events and requires no Mixin, coremod, access transformer or separate runtime helper.

Open the root Gradle project in IntelliJ for product common code and aggregate tasks. Link `mods/better-caves/versions/1.12.2/` as a separate Gradle project with JDK 21, and `mods/better-caves/versions/1.7.10/` with JDK 25. Generate native development configurations with `genSources genIntellijRuns` for 1.12.2 or `setupDecompWorkspace idea` for 1.7.10. The checked-in **Better Caves Run Client/Server 1.12.2/1.7.10** shortcuts point directly to these native product projects and invoke `runClient` or `runServer`. Repository wrapper launchers `runBetterCavesClient1_12_2`, `runBetterCavesServer1_12_2`, `runBetterCavesClient1_7_10`, and `runBetterCavesServer1_7_10` are also available. Configure `PYTHON` in the IntelliJ environment when needed.

Reference code remains browsable in the file tree, but the root IDEA model excludes it and no production source set includes it. Do not attach reference projects as legacy target modules or dependencies, or mark their directories as production source roots.

## Licensing and provenance

Each upstream project retains its original license and notices under its own reference directory. The repository's original LGPLv3 `LICENSE` is retained at the root and with `yungs-api`; Better Caves owns a copy in its product directory and packages it in its releases. Dungeons and Fortresses retain GPLv3 license files; API, Caves, Mineshafts, Strongholds and Extras retain LGPLv3 files. The two Better Caves targets include adapted Caves controller logic and privately packaged noise, with corresponding notices under `META-INF/licenses/`. No upstream bootstrap or API package is included.

FastNoise retains Jordan Peck's MIT notice. OpenSimplex2S comes directly from KdotJPG's original CC0 source at revision `e09a94744b3d69e4c58ce615445f18712cb50104`, rather than the retained API adaptation whose provenance was unresolved. Future ports must retain the corresponding upstream attribution and license context. The API snapshot's publishing metadata labels its license GPLv3 while its license file is LGPLv3; that existing discrepancy is preserved and remains relevant when selecting other API source for future ports.
