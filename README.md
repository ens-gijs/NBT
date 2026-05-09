# NBT for Minecraft Java Edition

Two cooperating Java libraries for working with [NBT](https://minecraft.gamepedia.com/NBT_format)
data and Minecraft Java Edition `.mca` region files.

> **Forked from [Querz/NBT](https://github.com/Querz/NBT).** This fork has
> diverged substantially — coordinates, package layout, and APIs have changed.
> It is not a drop-in replacement for the upstream artifact.

## The two modules

| Module                                              | What it gives you                                                         | Depend on this if…                                  |
| --------------------------------------------------- | ------------------------------------------------------------------------- | --------------------------------------------------- |
| [**`nbt-core`**](nbt-core/) `0.1.1-SNAPSHOT`        | NBT tag types (binary + SNBT I/O), `NbtPath` query, generic util classes. | You want NBT data only — no Minecraft `.mca` machinery. |
| [**`nbt-mca`**](nbt-mca/) `0.2.0-SNAPSHOT`          | Minecraft Java Edition `.mca` region/entities/POI file support, chunk relocation, palette utilities. Depends on `nbt-core` via Gradle `api` so you get NBT types transitively. | You're working with `.mca` files. Most consumers want this. |

Both are published to **Maven Central**. `-SNAPSHOT` builds are pushed to
**Central's snapshot repository** automatically when `master` is updated —
useful for picking up new `DataVersion` entries as Mojang ships Minecraft
snapshots.

Each module is **versioned independently**. Releases are cut as `core-vX.Y.Z`
and `mca-vX.Y.Z` git tags.

## nbt-core

NBT tag types (`io.github.ensgijs.nbt.tag`), binary and text I/O
(`io.github.ensgijs.nbt.io`), JSON-path-like `NbtPath` query
(`io.github.ensgijs.nbt.query`), and a small `util` package
(`io.github.ensgijs.nbt.util`) with the binary tag sorter and JSON
pretty-printer.

### Coordinates

```groovy
repositories {
    mavenCentral()
    // Only needed if you want bleeding-edge SNAPSHOT builds:
    maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
    implementation 'io.github.ens-gijs.nbt:nbt-core:0.1.1-SNAPSHOT'
}
```

```xml
<dependency>
    <groupId>io.github.ens-gijs.nbt</groupId>
    <artifactId>nbt-core</artifactId>
    <version>0.1.1-SNAPSHOT</version>
</dependency>
```

### Highlights

- [`NbtPath`](nbt-core/src/main/java/io/github/ensgijs/nbt/query/NbtPath.java) — JSON-path-like accessor for nested tags.
- [`TextNbtHelpers`](nbt-core/src/main/java/io/github/ensgijs/nbt/io/TextNbtHelpers.java) — read/write SNBT, with pretty-printing that round-trips through the parser.
- [`BinaryNbtHelpers`](nbt-core/src/main/java/io/github/ensgijs/nbt/io/BinaryNbtHelpers.java) — binary NBT I/O (compressed or uncompressed, big- and little-endian).

See the [nbt-core CHANGELOG](nbt-core/CHANGELOG.md) for release history.

## nbt-mca

Minecraft Java Edition `.mca` region/entities/POI file library, supporting MC
1.9.0 → 1.21.3+ (Bedrock not supported). Depends on `nbt-core` via Gradle
`api`, so consumers of `nbt-mca` get NBT tag types transitively without an
explicit `nbt-core` dependency.

### Coordinates

```groovy
dependencies {
    implementation 'io.github.ens-gijs.nbt:nbt-mca:0.2.0-SNAPSHOT'
}
```

```xml
<dependency>
    <groupId>io.github.ens-gijs.nbt</groupId>
    <artifactId>nbt-mca</artifactId>
    <version>0.2.0-SNAPSHOT</version>
</dependency>
```

### Highlights

- [`DataVersion`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/DataVersion.java) — near-complete data-version ↔ MC-version mapping back to 1.9.0. SNAPSHOT consumers get new `DataVersion` entries as soon as Mojang ships a snapshot.
- Supports terrain (region), entities, and POI mca files.
- Safely **relocate (move) chunks** to new coordinates — all internal coordinate references are rewritten. Well-tested; the reason this fork was revived. See [`RegionFileRelocator`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/RegionFileRelocator.java).
- Multiple I/O strategies — pick by memory/access pattern:
  - [`RandomAccessMcaFile`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/RandomAccessMcaFile.java) — low-overhead random access.
  - [`McaFileChunkIterator`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/McaFileChunkIterator.java) — sequential read.
  - [`McaFileStreamingWriter`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/McaFileStreamingWriter.java) — sequential write.
  - [`McaFileHelpers`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/McaFileHelpers.java) — whole-file load/save.
- Powerful palette utilities:
  - [`LongArrayTagPackedIntegers`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/util/LongArrayTagPackedIntegers.java) — comprehensive handler for MC's `long[]` packed values (block palettes, biome palettes, heightmaps) across all data versions.
  - [`PalettizedCuboid`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/util/PalettizedCuboid.java) — block- and biome-palette manipulation across every data-versioned palette format.

See the [nbt-mca CHANGELOG](nbt-mca/CHANGELOG.md) for release history.

## Migrating

### From `io.github.ens-gijs.nbt:nbt-mca:0.1.0` → `0.2.0` (the module split)

No source-code changes required. The Java packages
(`io.github.ensgijs.nbt.{tag,io,query,util}`) that moved to `nbt-core` kept
their package names, and `nbt-mca` declares an `api` dependency on
`nbt-core`, so they arrive transitively. Just bump your dependency version.

If you want to depend only on the NBT half (without `.mca` machinery), switch
your dependency to `nbt-core` instead.

### From the old local-Maven `0.1-SNAPSHOT`

If you were previously consuming this fork via `./gradlew publishToMavenLocal`:

| Before                                       | After                                                     |
| -------------------------------------------- | --------------------------------------------------------- |
| `io.github.ensgijs:ens-nbt:0.1-SNAPSHOT`     | `io.github.ens-gijs.nbt:nbt-mca:0.2.0-SNAPSHOT`           |

The Java package layout (`io.github.ensgijs.nbt.*`) is unchanged.

## Building from source

Java 17, Gradle wrapper, JUnit 4. On Windows use `gradlew.bat`; on POSIX use `./gradlew`.

```sh
./gradlew build                          # compile + test both modules
./gradlew :nbt-core:test                 # tests for one module
./gradlew :nbt-core:jacocoTestReport     # coverage report for one module
./gradlew javadoc                        # javadocs under ./doc/{nbt-core,nbt-mca}
./gradlew :nbt-core:jmh                  # JMH benchmarks (NBT-only)
./gradlew publishToMavenLocal            # stash both modules in your local ~/.m2 cache
```

## Project status

The library is under active development. The NBT half (`nbt-core`) is largely
stable; the MCA half (`nbt-mca`) continues to iterate. `DataVersion` and the
palette utilities are load-bearing — treat existing patterns there as stable.

## License

MIT — see [LICENSE](LICENSE). Portions derived from
[Querz/NBT](https://github.com/Querz/NBT) (also MIT).
