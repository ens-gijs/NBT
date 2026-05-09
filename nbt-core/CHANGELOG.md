# Changelog — nbt-core

All notable changes to `io.github.ens-gijs.nbt:nbt-core` will be documented in
this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Releases are cut as `core-vX.Y.Z` git tags handled by
`.github/workflows/publish-core.yml`.

## [Unreleased]

## [0.1.1] - TBD

First independent release of `nbt-core` after splitting `nbt-mca` (released
0.1.0 as a single artifact) into two modules. The Java packages
(`io.github.ensgijs.nbt.{tag,io,query,util}`) are unchanged from the
single-module 0.1.0 release; only the artifact coordinates have moved.

### Changed
- **Coordinates:** the NBT-side classes are now published separately as
  `io.github.ens-gijs.nbt:nbt-core` (previously they shipped only as part of
  `io.github.ens-gijs.nbt:nbt-mca:0.1.0`).
- Versioned independently from `nbt-mca`. `nbt-core` will likely tick over
  far less often than `nbt-mca` (which has to chase Minecraft data versions).

### Notes
- `nbt-mca` 0.2.0+ depends on `nbt-core` via Gradle `api`, so consumers of
  `nbt-mca` keep getting NBT tag types transitively without an explicit
  `nbt-core` dependency. Direct `nbt-core` consumers are users who want the
  NBT tag types and I/O without pulling in any Minecraft `.mca` machinery.

[Unreleased]: https://github.com/ens-gijs/NBT/compare/core-v0.1.1...HEAD
[0.1.1]: https://github.com/ens-gijs/NBT/releases/tag/core-v0.1.1
