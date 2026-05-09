# Graph Report - F:\Archive\Programming\Java\ThirdParty\QuerzNbt\.claude\worktrees\relaxed-liskov-0597d1  (2026-05-09)

## Corpus Check
- 170 files · ~369,231 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2545 nodes · 9335 edges · 44 communities detected
- Extraction: 37% EXTRACTED · 63% INFERRED · 0% AMBIGUOUS · INFERRED: 5896 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_Community 2|Community 2]]
- [[_COMMUNITY_Community 3|Community 3]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Community 5|Community 5]]
- [[_COMMUNITY_Community 6|Community 6]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Community 10|Community 10]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Community 13|Community 13]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_Community 15|Community 15]]
- [[_COMMUNITY_Community 16|Community 16]]
- [[_COMMUNITY_Community 17|Community 17]]
- [[_COMMUNITY_Community 18|Community 18]]
- [[_COMMUNITY_Community 19|Community 19]]
- [[_COMMUNITY_Community 20|Community 20]]
- [[_COMMUNITY_Community 21|Community 21]]
- [[_COMMUNITY_Community 22|Community 22]]
- [[_COMMUNITY_Community 23|Community 23]]
- [[_COMMUNITY_Community 24|Community 24]]
- [[_COMMUNITY_Community 25|Community 25]]
- [[_COMMUNITY_Community 26|Community 26]]
- [[_COMMUNITY_Community 27|Community 27]]
- [[_COMMUNITY_Community 28|Community 28]]
- [[_COMMUNITY_Community 29|Community 29]]
- [[_COMMUNITY_Community 30|Community 30]]
- [[_COMMUNITY_Community 31|Community 31]]
- [[_COMMUNITY_Community 32|Community 32]]
- [[_COMMUNITY_Community 33|Community 33]]
- [[_COMMUNITY_Community 34|Community 34]]
- [[_COMMUNITY_Community 35|Community 35]]
- [[_COMMUNITY_Community 36|Community 36]]
- [[_COMMUNITY_Community 37|Community 37]]
- [[_COMMUNITY_Community 38|Community 38]]
- [[_COMMUNITY_Community 39|Community 39]]
- [[_COMMUNITY_Community 40|Community 40]]
- [[_COMMUNITY_Community 41|Community 41]]
- [[_COMMUNITY_Community 42|Community 42]]
- [[_COMMUNITY_Community 43|Community 43]]

## God Nodes (most connected - your core abstractions)
1. `TerrainChunkBase` - 73 edges
2. `ObservedCompoundTag` - 72 edges
3. `CompoundTag` - 71 edges
4. `ListTag` - 68 edges
5. `Entity` - 66 edges
6. `EntityBase` - 64 edges
7. `PalettizedCuboidTest` - 60 edges
8. `latest()` - 57 edges
9. `LongArrayTagPackedIntegers` - 50 edges
10. `LongArrayTagPackedIntegersTest` - 49 edges

## Surprising Connections (you probably didn't know these)
- `DataVersion()` --calls--> `toString()`  [EXTRACTED]
  F:\Archive\Programming\Java\ThirdParty\QuerzNbt\.claude\worktrees\relaxed-liskov-0597d1\nbt-mca\src\main\java\io\github\ensgijs\nbt\mca\DataVersion.java → F:\Archive\Programming\Java\ThirdParty\QuerzNbt\.claude\worktrees\relaxed-liskov-0597d1\nbt-mca\src\main\java\io\github\ensgijs\nbt\mca\DataVersion.java  _Bridges community 4 → community 14_

## Communities

### Community 0 - "Community 0"
Cohesion: 0.02
Nodes (14): ChunkBaseTest, latest(), EntitiesChunk, EntitiesChunkTest, Entity, EntityBase, EntityImplTest, EntityUtil (+6 more)

### Community 1 - "Community 1"
Cohesion: 0.02
Nodes (27): ArrayTag, AutoTypingDemoTest, BigEndianNbtOutputStream, ByteArrayTagTest, NotAnArrayTag, ByteTag, ByteTagTest, CompressionTypeTest (+19 more)

### Community 2 - "Community 2"
Cohesion: 0.03
Nodes (13): BenchmarkBase, PerfTest, BlockStateTag, CompoundTagTest, detect(), McaFileStreamingWriter, ObservedCompoundTag, PoiChunkBase (+5 more)

### Community 3 - "Community 3"
Cohesion: 0.02
Nodes (18): LegacyBiomes, LoadFlags, McaEntitiesFile, ChunkIteratorImpl, McaFileBase, McaFileChunkIteratorTest, McaFileStreamingWriterTest, McaPoiFile (+10 more)

### Community 4 - "Community 4"
Cohesion: 0.04
Nodes (15): ByteArrayTag, DataVersion(), DoubleTag, FloatTag, IntArrayTag, IntTag, LongArrayTag, Builder (+7 more)

### Community 5 - "Community 5"
Cohesion: 0.04
Nodes (14): BlockStateTagTest, find(), EntitiesChunkBaseTest, IllegalEntityTagException, ListTagTest, McaDumper, McaEntitiesFileTest, McaPoiFileTest (+6 more)

### Community 6 - "Community 6"
Cohesion: 0.03
Nodes (15): EntityFactory, EntityCreatorStub, EntityFactoryTest, EntityStub, IntPointXZ, IntPointXZTest, McaFileBaseTest, McaFileHelpers (+7 more)

### Community 7 - "Community 7"
Cohesion: 0.04
Nodes (11): ChunkIterator, IdentityHelper, NullRejectingListIterator, IteratorImpl, Mutable, CursorIterator, entryCount(), PaletteCorruptedException (+3 more)

### Community 8 - "Community 8"
Cohesion: 0.05
Nodes (3): McaTestCase, TerrainChunkBase, TerrainSectionBase

### Community 9 - "Community 9"
Cohesion: 0.05
Nodes (2): CompoundTag, CompoundTagIterator

### Community 10 - "Community 10"
Cohesion: 0.06
Nodes (6): BlockAlignedBoundingRectangle, BlockAlignedBoundingRectangleTest, ChunkBoundingRectangle, ChunkBoundingRectangleTest, RegionBoundingRectangle, RegionBoundingRectangleTest

### Community 11 - "Community 11"
Cohesion: 0.06
Nodes (1): ListTag

### Community 12 - "Community 12"
Cohesion: 0.04
Nodes (5): ChunkBase, MappedNamedTag, EntitiesChunkBase, NamedTag, NamedTagTest

### Community 13 - "Community 13"
Cohesion: 0.05
Nodes (9): BinaryNbtHelpers, BinaryNbtTagSorter, TagPointer, UnsafeFastByteArrayIO, BinaryNbtTagSorterJmhBenchmark, BinaryNbtTagSorterTest, TextNbtDeserializer, TextNbtHelpers (+1 more)

### Community 14 - "Community 14"
Cohesion: 0.05
Nodes (22): BinaryNbtDeserializer, BinaryNbtSerializer, compress(), decompress(), finish(), getFromID(), IOExceptionFunction, bestFor() (+14 more)

### Community 15 - "Community 15"
Cohesion: 0.08
Nodes (2): BigEndianNbtInputStream, LittleEndianNbtInputStream

### Community 16 - "Community 16"
Cohesion: 0.14
Nodes (3): StringPointer, StringPointerTest, TextNbtParser

### Community 17 - "Community 17"
Cohesion: 0.06
Nodes (5): SectionBase, SectionedChunkBase, SectionIteratorImpl, TerrainChunk, TerrainSection

### Community 18 - "Community 18"
Cohesion: 0.1
Nodes (2): PoiRecord, PoiRecordTest

### Community 19 - "Community 19"
Cohesion: 0.12
Nodes (2): JsonPrettyPrinter, JsonPrettyPrinterTest

### Community 20 - "Community 20"
Cohesion: 0.09
Nodes (3): ChunkMetaInfo, McaFileChunkIterator, PositionTrackingInputStream

### Community 21 - "Community 21"
Cohesion: 0.13
Nodes (2): NameEvaluator, NbtPath

### Community 22 - "Community 22"
Cohesion: 0.11
Nodes (3): ArgValidator, LapToken, Stopwatch

### Community 23 - "Community 23"
Cohesion: 0.15
Nodes (1): IntPointXYZ

### Community 24 - "Community 24"
Cohesion: 0.29
Nodes (1): StringTag

### Community 25 - "Community 25"
Cohesion: 0.22
Nodes (1): BlockStateIterator

### Community 26 - "Community 26"
Cohesion: 0.48
Nodes (1): Deserializer

### Community 27 - "Community 27"
Cohesion: 0.29
Nodes (1): EndTag

### Community 28 - "Community 28"
Cohesion: 0.29
Nodes (3): Base, ImplA, ImplB

### Community 29 - "Community 29"
Cohesion: 0.33
Nodes (1): VersionLacksSupportException

### Community 30 - "Community 30"
Cohesion: 0.4
Nodes (1): IndexEvaluator

### Community 31 - "Community 31"
Cohesion: 0.5
Nodes (1): NbtInput

### Community 32 - "Community 32"
Cohesion: 0.5
Nodes (1): ExceptionRunnable

### Community 33 - "Community 33"
Cohesion: 0.5
Nodes (1): TracksUnreadDataTags

### Community 34 - "Community 34"
Cohesion: 0.67
Nodes (1): ExceptionBiFunction

### Community 35 - "Community 35"
Cohesion: 0.67
Nodes (1): MaxDepthReachedException

### Community 36 - "Community 36"
Cohesion: 0.67
Nodes (1): SilentIOException

### Community 37 - "Community 37"
Cohesion: 0.67
Nodes (1): Evaluator

### Community 38 - "Community 38"
Cohesion: 0.67
Nodes (1): UnsupportedVersionChangeException

### Community 39 - "Community 39"
Cohesion: 0.67
Nodes (1): DefaultEntityCreator

### Community 40 - "Community 40"
Cohesion: 0.67
Nodes (1): EntityCreator

### Community 41 - "Community 41"
Cohesion: 0.67
Nodes (1): CorruptMcaFileException

### Community 42 - "Community 42"
Cohesion: 0.67
Nodes (1): MoveChunkFlags

### Community 43 - "Community 43"
Cohesion: 0.67
Nodes (1): McaTerrainFileTest

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Entity` connect `Community 0` to `Community 5`?**
  _High betweenness centrality (0.031) - this node is a cross-community bridge._
- **Why does `EntityBase` connect `Community 0` to `Community 1`, `Community 2`?**
  _High betweenness centrality (0.029) - this node is a cross-community bridge._
- **Why does `TerrainChunkBase` connect `Community 8` to `Community 2`, `Community 3`, `Community 4`, `Community 5`, `Community 6`, `Community 7`, `Community 12`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.03 - nodes in this community are weakly interconnected._
- **Should `Community 3` be split into smaller, more focused modules?**
  _Cohesion score 0.02 - nodes in this community are weakly interconnected._