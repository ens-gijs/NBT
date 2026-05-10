# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:
- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost). If the command fails to run do not attempt to install it or locate it, simply inform the user.

## Behavioral rules

- **Surgical changes.** Touch only what the task requires; match existing style; do not refactor adjacent code.
- **Simplicity.** No speculative abstractions, configurability, or error handling for impossible scenarios.
- **Ask before assuming.** State assumptions explicitly; surface ambiguity instead of picking silently.
- **Verify, don't claim.** Define a success check before coding; loop until it passes.

## Build, test, run

Java 17, Gradle wrapper, JUnit 4. On Windows use `gradlew.bat`; on POSIX use `./gradlew`.

- Build: `./gradlew build`
- All tests: `./gradlew test`
- Single test: `./gradlew test --tests "io.github.ensgijs.nbt.<Pkg>.<Class>.<method>"`
- Coverage report: `./gradlew jacocoTestReport` (HTML + XML under `build/reports/jacoco`)
- Javadoc: `./gradlew javadoc` (output: `./doc/`)
- Publish `0.1-SNAPSHOT` to local Maven cache (this is how downstream projects consume the library — it is not yet on a public repo): `./gradlew publishToMavenLocal`
- JMH benchmarks (sources in `src/jmh/java`): `./gradlew jmh`
- Custom JFR profiling task defined in `build.gradle`: `./gradlew profilingRun` — runs `io.github.ensgijs.nbt.profiler.ProfilingRunner` with a 10s flight recording into `build/jfr-profiling/`

## Architecture

One published artifact (`io.github.ensgijs:ens-nbt`) contains three cooperating libraries under `io.github.ensgijs.nbt`:

### 1. NBT core — `nbt.tag` + `nbt.io`
- 13 tag types per the [NBT spec](https://minecraft.gamepedia.com/NBT_format). Hierarchy: `Tag` → `NumberTag` (Byte/Short/Int/Long/Float/Double), `ArrayTag` (ByteArray/IntArray/LongArray), plus `StringTag`, `ListTag`, `CompoundTag`, `EndTag`.
- `nbt.io` provides big- and little-endian stream classes (`BigEndianNbtInputStream`/`OutputStream`, `LittleEndian…`), binary helpers (`BinaryNbtHelpers`, `BinaryNbtSerializer`/`Deserializer`), and text/SNBT helpers (`TextNbtHelpers`, `TextNbtParser`, `TextNbtSerializer`/`Deserializer`). Pretty-printed SNBT round-trips through the parser.
- `MaxDepthIO` enforces `Tag.DEFAULT_MAX_DEPTH` (512) on every recursive operation to prevent circular references and DoS via deeply nested payloads.

### 2. NbtPath query — `nbt.query`
JSON-path-like accessor for nested tags (`NbtPath` + `evaluator` package — `NameEvaluator`, `IndexEvaluator`).

### 3. MCA library — `nbt.mca`
Reads/writes Minecraft Java Edition region (`.mca`) files, supporting MC 1.9.0 → 1.21.3+ (no Bedrock).

- **`DataVersion`** is the central enum — maps MC versions ↔ data versions and gates which features each chunk format supports. Most behavior across the MCA stack is version-conditional. Version-aware fields are wrapped in `VersionAware<T>` / `VersionedDataContainer`.
- **File entry points** (extend `McaFileBase`): `McaRegionFile` (terrain), `McaEntitiesFile`, `McaPoiFile`.
- **Chunks** extend `ChunkBase` → `SectionedChunkBase` → `TerrainChunkBase`/`EntitiesChunkBase`/`PoiChunkBase` (concrete: `TerrainChunk`, `EntitiesChunk`, `PoiChunk`). Sections via `SectionBase` / `TerrainSection`. The README notes this hierarchy is slated for a heavier refactor with an abstraction layer over the three chunk types.
- **I/O strategies in `nbt.mca.io`** — pick by memory/access pattern:
  - `McaFileHelpers` — load/save an entire file at once.
  - `RandomAccessMcaFile` — low-overhead random access.
  - `McaFileChunkIterator` — sequential read, one chunk at a time.
  - `McaFileStreamingWriter` — sequential write.
  - `RegionFileRelocator` — relocate whole region files.
  - `LoadFlags` and `MoveChunkFlags` are bitmask config constants.
- **Chunk relocation** (move a chunk to new coords, rewriting all internal coordinate references) is a flagship, well-tested feature — the reason the project was revived. Don't break it.
- **Utilities in `nbt.mca.util`** — reuse before reinventing:
  - `PalettizedCuboid` and `LongArrayTagPackedIntegers` are the canonical handlers for MC's bit-packed long-array palettes (block states, biomes, heightmaps) across every data version.
  - `BlockAlignedBoundingRectangle`, `ChunkBoundingRectangle`, `RegionBoundingRectangle`, `IntPointXZ`/`IntPointXYZ` for spatial math.
  - `LegacyBiomes`, `BlockStateIterator`, `McaDumper`, `McaWorld`.

## Tests

- Unit tests: `src/test/java` (JUnit 4).
- Integration data — real Minecraft saves spanning multiple versions — lives in `src/test/resources`. Many MCA tests load these fixtures rather than synthesising data, so version-conditional logic is validated end-to-end.
- JMH benchmarks live in their own source set at `src/jmh/java` (e.g. `BinaryNbtTagSorterJmhBenchmark`).

## Project status

Per `README.md`, the library is under heavy development. The NBT half is largely stable; the MCA half is still iterating, with the chunk class hierarchy explicitly flagged for refactor. Treat existing patterns (especially `DataVersion`-gated behavior and the palette utilities) as load-bearing.

## Version coupling between `:nbt` and `:nbt-mca`

Two modules, two versions in `gradle.properties` (`nbtVersion`, `mcaVersion`).
`nbt-mca` declares its dependency on `nbt` via `api project(':nbt')` — a Gradle
project dependency that auto-resolves at evaluation time. **Don't replace it with
a hardcoded coordinate string** (`api 'io.github.ens-gijs.nbt:nbt:0.x.y'`) — the
project-dep is what makes the rules below "just work."

### The version-state convention

The value of each `xVersion` property in `gradle.properties` reflects "the next
thing that will be published," and **encodes whether the module has unreleased
changes**:

| `nbtVersion` value         | What it means                                             |
| -------------------------- | --------------------------------------------------------- |
| Bare release, e.g. `0.1.1` | nbt has no unreleased changes since `nbt-v0.1.1` shipped. |
| `-SNAPSHOT`, e.g. `0.1.2-SNAPSHOT` | nbt has unreleased changes accumulating toward `0.1.2`. |

After releasing nbt 0.1.1, **`nbtVersion` stays at `0.1.1`**. There is no
"resume development" auto-bump. The first commit that touches `nbt/src/main/**`
must also bump `nbtVersion` to the next `-SNAPSHOT` (e.g. `0.1.2-SNAPSHOT`) in
the same commit. From then on, the snapshot workflow publishes that SNAPSHOT
on every push.

Same convention for `nbt-mca` and `mcaVersion`.

### What this gives us

- "Does nbt have changes that need releasing before nbt-mca releases?" is
  answered by reading one line of `gradle.properties`.
- nbt-mca's release flow doesn't need a temp-pin dance in the common case —
  if `nbtVersion` is already a bare release, the published POM declares the
  correct release dep automatically.
- The snapshot workflow's per-module SNAPSHOT detection naturally skips
  publishing a module whose version is a bare release (no double-publish of
  release versions to Central's snapshot repo).

### Source-time rule

`nbt-mca` source builds always see whatever `nbtVersion` currently is, via
`api project(':nbt')`. No manual sync needed.

### Publish-time rules

- **SNAPSHOT publish of nbt-mca** declares a dep on whatever nbt version is
  current — `nbt:<x>-SNAPSHOT` if nbt has unreleased changes, otherwise
  `nbt:<x>` (a release version). Both are fine.
- **Release publish of nbt-mca** must declare a dep on a *released* version
  of nbt. In the steady state (nbt has no unreleased changes), this is
  automatic. If nbt has unreleased changes, the release skill walks the
  user through "release nbt first" or "pin to a previous nbt release."

### Enforcement layers

1. **CI check on PRs** (in `.github/workflows/build.yml`) — fails if a PR
   modifies `<module>/src/main/**` but `<module>Version` in
   `gradle.properties` is unchanged AND not a `-SNAPSHOT`. Forces the
   convention at PR review time.
2. **CI check on direct pushes to master** (in `.github/workflows/publish-snapshot.yml`)
   — same logic, comparing HEAD against HEAD~1, in case someone bypasses
   PR review.
3. **Build-time guard in `nbt-mca/build.gradle`** — `gradle.taskGraph.whenReady`
   hook hard-fails any nbt-mca publish where `mcaVersion` is a release
   (no `-SNAPSHOT`) but the resolved `:nbt` version is `-SNAPSHOT`. Backstop
   for the truly bypass-everything case.
4. **Release skill** — codifies the procedure for both modules, including
   the "release nbt first / pin to previous" branch when nbt-mca needs to
   release while nbt has unreleased changes.

If you see a CI failure like *"Modified `nbt/src/main/...` but `nbtVersion`
is still `0.1.1` (a release version). Bump it to the next `-SNAPSHOT`."* —
edit `gradle.properties` to bump the version and push again.
