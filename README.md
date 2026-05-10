# NBT Library + MCA Library for Minecraft Java Edition

Two cooperating Java libraries for working with [NBT](https://minecraft.gamepedia.com/NBT_format)
data and `.mca` region files (mca support is for MC Java only).

> **Forked from [Querz/NBT](https://github.com/Querz/NBT).** This fork has
> diverged substantially — coordinates, package layout, and APIs have changed.
> It is not a drop-in replacement for the upstream artifact.

## The two modules

| Module                            | What it gives you                                                                                                                                                              | Depend on this if…                                                        |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|
| [**`nbt`**](nbt/) `0.1.1`         | NBT library with binary & SNBT (text) I/O, [`NbtPath`](nbt/src/main/java/io/github/ensgijs/nbt/query/NbtPath.java), pretty printing, and more.                            | You want NBT data processing only.                                        |
| [**`nbt-mca`**](nbt-mca/) `0.2.0` | Minecraft Java Edition `.mca` region/entities/POI file support, chunk relocation, palette utilities. Depends on `nbt` via Gradle `api` so you get NBT types transitively. | You're working with `.mca` region files. *You're probably here for this.* |

Both are published to **Maven Central**. `-SNAPSHOT` builds are pushed to
**Central's snapshot repository** automatically when `master` is updated —
useful for picking up new `DataVersion` entries as Mojang ships Minecraft
snapshots.

Each module is **versioned independently**. Releases are cut as `core-vX.Y.Z`
and `mca-vX.Y.Z` git tags.

## nbt-mca — Library for reading and manipulating world region files.

Minecraft Java Edition `.mca` region/entities/POI file library, supporting MC
v1.9.0 → v26.1+ (Bedrock not supported). Depends on `nbt` via Gradle
`api`, so consumers of `nbt-mca` get NBT tag types transitively without an
explicit `nbt` dependency.

*At this point, this layer is mostly inspired by [Querz/NBT](https://github.com/Querz/NBT)'s (v6.1) implementation.*

### Coordinates

```groovy
repositories {
    mavenCentral()
    // Only needed if you want bleeding-edge SNAPSHOT builds:
    maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
    implementation 'io.github.ens-gijs.nbt:nbt-mca:0.2.0'
}
```

```xml
<dependency>
    <groupId>io.github.ens-gijs.nbt</groupId>
    <artifactId>nbt-mca</artifactId>
    <version>0.2.0</version>
</dependency>
```

### Highlights

- Supports Minecraft Java terrain (region), entities, and POI `.mca` files from MC v1.9.0 to v26.1 and beyond.
- [`RegionFileRelocator`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/RegionFileRelocator.java) — safely **relocate (move) chunks** to new coordinates — all internal coordinate references are rewritten. Well-tested; this **is** the reason this fork was revived.
- [`DataVersion`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/DataVersion.java) — near-complete data-version ↔ MC-version mapping back to 1.9.0. SNAPSHOT consumers get new `DataVersion` entries shortly after Mojang ships a snapshot.
- [`VersionAware<T>`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/util/VersionAware.java) — simple utility class for managing data version support, an alternative to using massive `if.. else if .. else` blocks checking data versions.
- Multiple I/O strategies — pick by memory/access pattern:
    - [`RandomAccessMcaFile`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/RandomAccessMcaFile.java) — low-overhead random access.
    - [`McaFileChunkIterator`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/McaFileChunkIterator.java) — sequential read.
    - [`McaFileStreamingWriter`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/McaFileStreamingWriter.java) — sequential write.
    - [`McaFileHelpers`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/io/McaFileHelpers.java) — whole-file load/save.
- Powerful palette utilities for working with chunk and biome data:
    - [`LongArrayTagPackedIntegers`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/util/LongArrayTagPackedIntegers.java) — comprehensive handler for MC's `long[]` packed values (block palettes, biome palettes, heightmaps) across all data versions.
    - [`PalettizedCuboid`](nbt-mca/src/main/java/io/github/ensgijs/nbt/mca/util/PalettizedCuboid.java) — block- and biome-palette manipulation across every data-versioned palette format.

See the [nbt-mca CHANGELOG](nbt-mca/CHANGELOG.md) for release history.

## nbt — Library for working with NBT tags.

NBT tag types (`io.github.ensgijs.nbt.tag`), binary and text I/O
(`io.github.ensgijs.nbt.io`), JSON-path-like `NbtPath` query
(`io.github.ensgijs.nbt.query`), and a small `util` package
(`io.github.ensgijs.nbt.util`) with the binary tag sorter and JSON
pretty-printer.

Supports both Minecraft Java and Bedrock.

*The heart of this layer is still heavily based on [Querz/NBT](https://github.com/Querz/NBT)'s implementation (v6.1).
However, it also has been modified considerably. Things have moved. Things have been renamed. If you're already using
Querz/NBT you can expect migrating to this impl to be of low to moderate effort. The libraries will not
conflict on the classpath or in any way and can be used at the same time. **There is no need to migrate
if you don't want to.** Note that the package names have changed, you'll need to fix all imports.*

> [!TIP]
> Use nbt serialization to pass data between any two NBT libraries (`old.nbt.Tag` → `byte[]` → `other.nbt.Tag`).

### Coordinates

```groovy
repositories {
    mavenCentral()
    // Only needed if you want bleeding-edge SNAPSHOT builds:
    maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
    implementation 'io.github.ens-gijs.nbt:nbt:0.1.1'
}
```

```xml
<dependency>
    <groupId>io.github.ens-gijs.nbt</groupId>
    <artifactId>nbt</artifactId>
    <version>0.1.1</version>
</dependency>
```

### Highlights

- [`NbtPath`](nbt/src/main/java/io/github/ensgijs/nbt/query/NbtPath.java) — JSON-path-like accessor for nested tags. Provides a simple mechanism to retrieve, and store, structured data using simple path expressions without having to handle the intermediate tags yourself. Ex: `int homeY = NbtPath.of("Brain.memories.minecraft:home.value.pos[1]").getInt(tag)`
- [`BinaryNbtTagSorter`](nbt/src/main/java/io/github/ensgijs/nbt/util/BinaryNbtTagSorter.java) — performance optimized binary data sorter. Skips parsing bytes into tags. *Sorted data is valuable for its ability to be deterministically fingerprinted & hashcoded and enables direct equality comparison of nbt byte[]'s.* 
- [`TextNbtHelpers`](nbt/src/main/java/io/github/ensgijs/nbt/io/TextNbtHelpers.java) — read/write SNBT, with real pretty-printing output that can then be passed back into the snbt parser as valid input.
- [`BinaryNbtHelpers`](nbt/src/main/java/io/github/ensgijs/nbt/io/BinaryNbtHelpers.java) — binary NBT I/O (compressed or uncompressed, big- and little-endian).

See the [nbt CHANGELOG](nbt/CHANGELOG.md) for release history.

## Migrating

### From `io.github.ens-gijs.nbt:nbt-mca:0.1.0` → `0.2.0` (the module split)

No source-code changes required. The Java packages
(`io.github.ensgijs.nbt.{tag,io,query,util}`) that moved to `nbt` kept
their package names, and `nbt-mca` declares an `api` dependency on
`nbt`, so they arrive transitively. Just bump your dependency version.

If you want to depend only on the NBT half (without `.mca` machinery), switch
your dependency to `nbt` instead.

### From the old local-Maven `0.1-SNAPSHOT`

If you were previously consuming this fork via `./gradlew publishToMavenLocal`:

| Before                                       | After                                      |
| -------------------------------------------- |--------------------------------------------|
| `io.github.ensgijs:ens-nbt:0.1-SNAPSHOT`     | `io.github.ens-gijs.nbt:nbt-mca:0.2.0`     |

The Java package layout (`io.github.ensgijs.nbt.*`) is unchanged.

## Building from source

Java 17, Gradle wrapper, JUnit 4. On Windows use `gradlew.bat`; on POSIX use `./gradlew`.

```sh
./gradlew build                          # compile + test both modules
./gradlew :nbt:test                 # tests for one module
./gradlew :nbt:jacocoTestReport     # coverage report for one module
./gradlew javadoc                        # javadocs under ./doc/{nbt,nbt-mca}
./gradlew :nbt:jmh                  # JMH benchmarks (NBT-only)
./gradlew publishToMavenLocal            # stash both modules in your local ~/.m2 cache
```

## Project status

The library is under active development. The NBT half (`nbt`) is largely
stable; the MCA half (`nbt-mca`) continues to iterate. `DataVersion` and the
palette utilities are load-bearing — treat existing patterns there as stable.

## License

MIT — see [LICENSE](LICENSE). Portions derived from
[Querz/NBT](https://github.com/Querz/NBT) (also MIT).
