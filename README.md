# nbt-mca

A rich Java library for working with [NBT](https://minecraft.gamepedia.com/NBT_format) data and Minecraft Java Edition `.mca` region files.

> **Forked from [Querz/NBT](https://github.com/Querz/NBT).** This fork has diverged substantially — coordinates, package layout, and APIs have changed. It is not a drop-in replacement for the upstream artifact.

## Coordinates

Released versions are published to **Maven Central**. Snapshots are published to **Central's snapshot repository** automatically when `master` is updated.

### Gradle

```groovy
repositories {
    mavenCentral()
    // Only needed if you want bleeding-edge SNAPSHOT builds:
    maven { url = 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
    implementation 'io.github.ens-gijs.nbt:nbt-mca:0.1.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.ens-gijs.nbt</groupId>
    <artifactId>nbt-mca</artifactId>
    <version>0.1.0</version>
</dependency>
```

> **Heads-up — module split coming.** A future release will split this artifact into `nbt-core` (NBT tag types, I/O, `NbtPath`) and `nbt-mca` (everything Minecraft `.mca`-specific, depending on `nbt-core`). The split will be transparent: existing `nbt-mca` users keep working unchanged, and consumers who only need the NBT half can switch their dependency to `nbt-core`.

### Migrating from the local-Maven `0.1-SNAPSHOT`

If you were previously consuming this fork via `./gradlew publishToMavenLocal`:

| Before                                       | After                                                 |
| -------------------------------------------- | ----------------------------------------------------- |
| `io.github.ensgijs:ens-nbt:0.1-SNAPSHOT`     | `io.github.ens-gijs.nbt:nbt-mca:0.1.0`        |

The Java package layout (`io.github.ensgijs.nbt.*`) is unchanged — no code changes required, just the dependency coordinate.

## Highlights

### NBT
- [`NbtPath`](src/main/java/io/github/ensgijs/nbt/query/NbtPath.java) — JSON-path-like accessor for nested tags.
- [`TextNbtHelpers`](src/main/java/io/github/ensgijs/nbt/io/TextNbtHelpers.java) — read/write SNBT, with pretty-printing that round-trips through the parser.
- [`BinaryNbtHelpers`](src/main/java/io/github/ensgijs/nbt/io/BinaryNbtHelpers.java) — binary NBT I/O (compressed or uncompressed, big- and little-endian).

### MCA
- Minecraft Java Edition support from MC 1.9.0 → 1.21.3+ (Bedrock not supported).
  - [`DataVersion`](src/main/java/io/github/ensgijs/nbt/mca/DataVersion.java) provides near-complete data-version ↔ MC-version mapping back to 1.9.0. SNAPSHOT consumers get new `DataVersion` entries as soon as Mojang ships a snapshot.
- Supports terrain (region), entities, and POI mca files.
- Safely **relocate (move) chunks** to new coordinates — all internal coordinate references are rewritten. Well-tested; the reason this fork was revived. See [`RegionFileRelocator`](src/main/java/io/github/ensgijs/nbt/mca/io/RegionFileRelocator.java).
- Multiple I/O strategies: pick by memory/access pattern.
  - [`RandomAccessMcaFile`](src/main/java/io/github/ensgijs/nbt/mca/io/RandomAccessMcaFile.java) — low-overhead random access.
  - [`McaFileChunkIterator`](src/main/java/io/github/ensgijs/nbt/mca/io/McaFileChunkIterator.java) — sequential read.
  - [`McaFileStreamingWriter`](src/main/java/io/github/ensgijs/nbt/mca/io/McaFileStreamingWriter.java) — sequential write.
  - [`McaFileHelpers`](src/main/java/io/github/ensgijs/nbt/mca/io/McaFileHelpers.java) — whole-file load/save.

### Utilities
- [`LongArrayTagPackedIntegers`](src/main/java/io/github/ensgijs/nbt/mca/util/LongArrayTagPackedIntegers.java) — comprehensive handler for MC's `long[]` packed values (block palettes, biome palettes, heightmaps) across all data versions.
- [`PalettizedCuboid`](src/main/java/io/github/ensgijs/nbt/mca/util/PalettizedCuboid.java) — block- and biome-palette manipulation across every data-versioned palette format.

## Building from source

Java 17, Gradle wrapper, JUnit 4. On Windows use `gradlew.bat`; on POSIX use `./gradlew`.

```sh
./gradlew build                # compile + test
./gradlew test                 # tests only
./gradlew jacocoTestReport     # coverage report under build/reports/jacoco
./gradlew javadoc              # javadocs under ./doc/
./gradlew jmh                  # JMH benchmarks (sources in src/jmh/java)
./gradlew publishToMavenLocal  # stash a build in your local ~/.m2 cache
```

## Project status

The library is under active development. The NBT half is largely stable; the MCA half continues to iterate, with the chunk class hierarchy slated for a refactor as part of the upcoming module split. `DataVersion` and the palette utilities are load-bearing — treat existing patterns there as stable.

## License

MIT — see [LICENSE](LICENSE). Portions derived from [Querz/NBT](https://github.com/Querz/NBT) (also MIT).

---
## NBT Specification
According to the [specification](https://minecraft.gamepedia.com/NBT_format), there are currently 13 different types of tags:

| Tag class    | Superclass | ID | Payload |
| ---------    | ---------- | -- | ----------- |
| [EndTag](src/main/java/io/github/ensgijs/nbt/tag/EndTag.java)             | [Tag](src/main/java/io/github/ensgijs/nbt/tag/Tag.java)               | 0  | None |
| [ByteTag](src/main/java/io/github/ensgijs/nbt/tag/ByteTag.java)           | [NumberTag](src/main/java/io/github/ensgijs/nbt/tag/NumberTag.java)   | 1  | 1 byte / 8 bits, signed |
| [ShortTag](src/main/java/io/github/ensgijs/nbt/tag/ShortTag.java)         | [NumberTag](src/main/java/io/github/ensgijs/nbt/tag/NumberTag.java)   | 2  | 2 bytes / 16 bits, signed, big endian |
| [IntTag](src/main/java/io/github/ensgijs/nbt/tag/IntTag.java)             | [NumberTag](src/main/java/io/github/ensgijs/nbt/tag/NumberTag.java)   | 3  | 4 bytes / 32 bits, signed, big endian |
| [LongTag](src/main/java/io/github/ensgijs/nbt/tag/LongTag.java)           | [NumberTag](src/main/java/io/github/ensgijs/nbt/tag/NumberTag.java)   | 4  | 8 bytes / 64 bits, signed, big endian |
| [FloatTag](src/main/java/io/github/ensgijs/nbt/tag/FloatTag.java)         | [NumberTag](src/main/java/io/github/ensgijs/nbt/tag/NumberTag.java)   | 5  | 4 bytes / 32 bits, signed, big endian, IEEE 754-2008, binary32 |
| [DoubleTag](src/main/java/io/github/ensgijs/nbt/tag/DoubleTag.java)       | [NumberTag](src/main/java/io/github/ensgijs/nbt/tag/NumberTag.java)   | 6  | 8 bytes / 64 bits, signed, big endian, IEEE 754-2008, binary64 |
| [ByteArrayTag](src/main/java/io/github/ensgijs/nbt/tag/ByteArrayTag.java) | [ArrayTag](src/main/java/io/github/ensgijs/nbt/tag/ArrayTag.java)     | 7  | `IntTag` payload *size*, then *size* `ByteTag` payloads |
| [StringTag](src/main/java/io/github/ensgijs/nbt/tag/StringTag.java)       | [Tag](src/main/java/io/github/ensgijs/nbt/tag/Tag.java)               | 8  | `ShortTag` payload *length*, then a UTF-8 string with size *length* |
| [ListTag](src/main/java/io/github/ensgijs/nbt/tag/ListTag.java)           | [Tag](src/main/java/io/github/ensgijs/nbt/tag/Tag.java)               | 9  | `ByteTag` payload *tagId*, then `IntTag` payload *size*, then *size* tags' payloads, all of type *tagId* |
| [CompoundTag](src/main/java/io/github/ensgijs/nbt/tag/CompoundTag.java)   | [Tag](src/main/java/io/github/ensgijs/nbt/tag/Tag.java)               | 10 | Fully formed tags, followed by an `EndTag` |
| [IntArrayTag](src/main/java/io/github/ensgijs/nbt/tag/IntArrayTag.java)   | [ArrayTag](src/main/java/io/github/ensgijs/nbt/tag/ArrayTag.java)     | 11 | `IntTag` payload *size*, then *size* `IntTag` payloads |
| [LongArrayTag](src/main/java/io/github/ensgijs/nbt/tag/LongArrayTag.java) | [ArrayTag](src/main/java/io/github/ensgijs/nbt/tag/ArrayTag.java)     | 12 | `IntTag` payload *size*, then *size* `LongTag` payloads |

* The `EndTag` is only used to mark the end of a `CompoundTag` in its serialized state or an empty `ListTag`.

* The maximum depth of the NBT structure is 512. If the depth exceeds this restriction during serialization, deserialization or String conversion, a `MaxDepthReachedException` is thrown. This usually happens when a circular reference exists in the NBT structure. The NBT specification does not allow circular references, as there is no tag to represent this.