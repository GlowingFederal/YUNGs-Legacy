# Changelog

2026-10-13 05:05 — Establish isolated legacy multiversion architecture

- Preserve the original API checkout and six selected YUNG's mod projects unchanged under `reference/1.16.5/`, including upstream licenses, resources, metadata, and build context.
- Adopt the ExampleMod independent-wrapper architecture for Forge 1.6.4, 1.7.10, 1.8.9, and 1.12.2, with pinned Unimined and GTNHGradle/Jabel tooling, production version allocation, bytecode checks, and archive verification.
- Add an internal neutral bootstrap, a portable status helper, shared resources, and native `/yungslegacy` status adapters without porting upstream API or gameplay features.
- Exclude reference sources from production and IDE source resolution; reject upstream classes and bundled dependencies in production archive verification.
- Provide aggregate build/test/clean commands, per-target builds, portable IntelliJ shortcuts, and documentation for future independently installable ports without a separate legacy YUNG's API dependency.
