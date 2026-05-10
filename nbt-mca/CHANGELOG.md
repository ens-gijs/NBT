# Changelog — nbt-mca

All notable changes to `io.github.ens-gijs.nbt:nbt-mca` will be documented in
this file. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Releases are cut as `mca-vX.Y.Z` git tags handled by
`.github/workflows/publish-mca.yml`.

## [Unreleased]

## [0.2.0] - TBD

Module split: the NBT-format classes that previously shipped inside
`nbt-mca:0.1.0` (`io.github.ensgijs.nbt.{tag,io,query,util}`) have been
moved into their own artifact, `io.github.ens-gijs.nbt:nbt`.

### Changed
- **Now depends on `io.github.ens-gijs.nbt:nbt`** via Gradle `api` /
  Maven `<scope>compile</scope>`. Consumers of `nbt-mca` keep getting NBT
  tag types transitively — no source-code changes are required to upgrade
  from 0.1.0; only the build-file dependency line changes.
- Java package layout for `io.github.ensgijs.nbt.mca.*` is unchanged from
  0.1.0. The classes that moved to `nbt` also kept their packages.
- Versioned independently from `nbt`. Future Minecraft data-version
  additions will ship under `nbt-mca` minor/patch bumps without dragging
  `nbt`'s version forward.

### Migrating from 0.1.0
- Gradle: no change required. Your existing
  `implementation 'io.github.ens-gijs.nbt:nbt-mca:<version>'` keeps working;
  the NBT classes now arrive transitively via `nbt`.
- If you want to depend only on the NBT tag types and not the MCA machinery,
  switch your dependency to `io.github.ens-gijs.nbt:nbt` instead.

## [0.1.0] - 2026-05-09

First Maven Central release of the `ens-gijs` fork. Coordinates and artifact
name changed from the previous local-Maven-only
`io.github.ensgijs:ens-nbt:0.1-SNAPSHOT` to
`io.github.ens-gijs.nbt:nbt-mca:0.1.0`. The Java package layout
(`io.github.ensgijs.nbt.*`) is unchanged.

### Added
- Maven Central publishing via the vanniktech maven-publish plugin.
- Automated `-SNAPSHOT` publishing to the Central snapshot repository on
  every push to `master` that touches build inputs.
- `japicmp` binary-compatibility task (advisory at 0.x; will be promoted
  to blocking at 1.0.0).

### Changed
- **GroupId / artifactId:** `io.github.ensgijs:ens-nbt` →
  `io.github.ens-gijs.nbt:nbt-mca`.
- **Version scheme:** added a patch component (`0.1-SNAPSHOT` →
  `0.1.0-SNAPSHOT`) so the project follows full SemVer.
- LICENSE updated to add Ens Gijs (Ross) copyright alongside the original
  Querz copyright.

### Removed
- `.travis.yml`, the broken upstream `gradle-publish.yml` workflow, the
  Coveralls plugin, and `TerrainMCAFileBase.java` (commented-out scaffolding).
- All inherited upstream release tags (`1.0` … `6.1`).

[Unreleased]: https://github.com/ens-gijs/NBT/compare/mca-v0.2.0...HEAD
[0.2.0]: https://github.com/ens-gijs/NBT/releases/tag/mca-v0.2.0
[0.1.0]: https://github.com/ens-gijs/NBT/releases/tag/v0.1.0
