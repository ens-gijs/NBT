# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-09

First Maven Central release of the `ens-gijs` fork. Coordinates and artifact name
have changed from the previous local-Maven-only `io.github.ensgijs:ens-nbt:0.1-SNAPSHOT`
to `io.github.ens-gijs.nbt:nbt-mca:0.1.0`. The Java package layout
(`io.github.ensgijs.nbt.*`) is unchanged.

### Added
- Maven Central publishing via the vanniktech maven-publish plugin.
- Automated `-SNAPSHOT` publishing to the Central snapshot repository on every push to `master` that touches build inputs.
- `japicmp` binary-compatibility task (advisory at 0.x; will be promoted to blocking at 1.0.0).
- `CHANGELOG.md` (this file).

### Changed
- **GroupId / artifactId:** `io.github.ensgijs:ens-nbt` → `io.github.ens-gijs.nbt:nbt-mca`.
- **Version scheme:** added a patch component (`0.1-SNAPSHOT` → `0.1.0-SNAPSHOT`) so the project follows full SemVer.
- LICENSE updated to add Ens Gijs (Ross) copyright alongside the original Querz copyright.
- README rewritten around the new coordinates and Central availability.

### Removed
- `.travis.yml` (Travis CI is no longer in use).
- `.github/workflows/gradle-publish.yml` (broken upstream cruft pointing at GitHub Packages).
- Coveralls plugin and the GitHub Packages publish block from `build.gradle`.
- `TerrainMCAFileBase.java` (entirely commented-out scaffolding referencing the original `net.rossquerz` package).
- All inherited upstream release tags (`1.0` … `6.1`). Releases of this fork begin at `v0.1.0`.

[Unreleased]: https://github.com/ens-gijs/NBT/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/ens-gijs/NBT/releases/tag/v0.1.0
