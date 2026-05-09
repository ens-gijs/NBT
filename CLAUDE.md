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
