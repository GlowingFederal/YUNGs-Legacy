# Changelog

2026-10-13 05:05 — Establish isolated legacy multiversion architecture

- Preserve the original API checkout and six selected YUNG's mod projects unchanged under `reference/1.16.5/`, including upstream licenses, resources, metadata, and build context.
- Adopt the ExampleMod independent-wrapper architecture for Forge 1.6.4, 1.7.10, 1.8.9, and 1.12.2, with pinned Unimined and GTNHGradle/Jabel tooling, production version allocation, bytecode checks, and archive verification.
- Add an internal neutral bootstrap, a portable status helper, shared resources, and native `/yungslegacy` status adapters without porting upstream API or gameplay features.
- Exclude reference sources from production and IDE source resolution; reject upstream classes and bundled dependencies in production archive verification.
- Provide aggregate build/test/clean commands, per-target builds, portable IntelliJ shortcuts, and documentation for future independently installable ports without a separate legacy YUNG's API dependency.

2026-10-17 07:30 — Add Better Caves terrain generation on Forge 1.7.10 and 1.12.2

- Replace native overworld cave carving through Forge's terrain-generation hooks, using a shared Better Caves computation core and separate native block/metadata and ChunkPrimer adapters. Preserve vanilla fallback in disabled/excluded dimensions, unsupported providers and custom map generators.
- Add cubic/OpenSimplex cave regions, liquid/floored caverns, region blending, surface/vanilla tunnels, native ravine geometry, ocean flooding/coastline protection, liquid-region buffers, surface protection and bedrock flattening. Keep deterministic world seeds/global coordinates and release cached settings/noise on world unload.
- Load native Forge configuration with optional numeric-dimension overrides; retain major reference defaults and configurable noise modes. Use native legacy substitutes for blocks absent from 1.7.10 and retain y=0 bedrock.
- Package private FastNoise with its MIT notice and original CC0 OpenSimplex2S with pinned provenance, alongside Better Caves LGPL attribution. Add no YUNG's API dependency or bytecode transformer; leave retained references and other targets unchanged.
- Document supported behavior/configuration and extend production archive checks for both implemented targets.

2026-10-17 07:49 — Separate Better Caves product ownership from the repository

- Move existing Better Caves common code, notices and native 1.7.10/1.12.2 projects under `mods/better-caves`, preserving generation logic, native Forge integration and configuration paths.
- Give Better Caves its own `bettercaves` mod identity, private package, entry points, metadata, license, version counter and `better-caves-<version>+mc<target>-forge.jar` outputs. Keep `/yungslegacy` as an alias for the product-owned `/bettercaves` status command.
- Make root tasks discover independent products, with product-scoped build numbering, archive checks and pruning. Reject additional mod entry points, generic bootstrap classes/resources and YUNG's API runtime dependencies in Better Caves releases.
- Point client/server IntelliJ shortcuts at the native Better Caves projects. Retain 1.6.4 and 1.8.9 projects and their neutral prototype scaffolding as inactive build templates for future work; exclude them from production tasks. Preserve upstream reference material.
