package io.github.ensgijs.nbt.mca;


import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;
import io.github.ensgijs.nbt.util.Mutable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * All implementors of {@link PoiChunkBase} should create a test which inherits this one and add
 * tests to cover any additional functionality added by that concretion.
 */
public abstract class PoiChunkBaseTest<RT extends PoiRecord, T extends PoiChunkBase<RT>> extends ChunkBaseTest<T> {
    protected static final DataVersion DEFAULT_TEST_VERSION = DataVersion.latest();

    protected abstract RT createPoiRecord(int x, int y, int z, String type);

    @Override
    protected CompoundTag createTag(int dataVersion, int chunkX, int chunkZ) {
        final CompoundTag tag = super.createTag(dataVersion, chunkX, chunkZ);
        // annoyingly poi chunks don't record their chunk XZ

        final CompoundTag sectionContainerTag = new CompoundTag();
        CompoundTag sectionTag;
        ListTag<CompoundTag> recordsListTag;
        tag.put("Sections", sectionContainerTag);

        // r.-3.-2.mca
        // chunks -96 -64 to -65 -33
        // blocks -1536 -1024 to -1025 -513
        //
        // within chunk -65 -42
        // blocks -1040 -672 to -1025 -657

        // section marked invalid,
        sectionTag = new CompoundTag();
        recordsListTag = new ListTag<>(CompoundTag.class);
        recordsListTag.add(PoiRecordTest.makeTag(1, "minecraft:cartographer", -1032, 41, -667));
        recordsListTag.add(PoiRecordTest.makeTag(1, "minecraft:shepherd", -1032, 42, -667));
        recordsListTag.add(PoiRecordTest.makeTag(1, "minecraft:toolsmith", -1032, 43, -667));
        sectionTag.putBoolean("Valid", false);
        sectionTag.put("Records", recordsListTag);
        sectionContainerTag.put("2", sectionTag);

        // fully valid
        sectionTag = new CompoundTag();
        recordsListTag = new ListTag<>(CompoundTag.class);
        recordsListTag.add(PoiRecordTest.makeTag(0, "minecraft:home", -1032, 63, -670));
        recordsListTag.add(PoiRecordTest.makeTag(1, "minecraft:cartographer", -1032, 63, -667));
        sectionTag.putBoolean("Valid", true);
        sectionTag.put("Records", recordsListTag);
        sectionContainerTag.put("3", sectionTag);

        // fully valid
        sectionTag = new CompoundTag();
        recordsListTag = new ListTag<>(CompoundTag.class);
        recordsListTag.add(PoiRecordTest.makeTag(0, "minecraft:nether_portal", -1031, 71, -667));
        recordsListTag.add(PoiRecordTest.makeTag(0, "minecraft:nether_portal", -1031, 71, -668));
        recordsListTag.add(PoiRecordTest.makeTag(0, "minecraft:nether_portal", -1031, 72, -667));
        recordsListTag.add(PoiRecordTest.makeTag(0, "minecraft:nether_portal", -1031, 72, -668));
        recordsListTag.add(PoiRecordTest.makeTag(0, "minecraft:nether_portal", -1031, 73, -667));
        recordsListTag.add(PoiRecordTest.makeTag(0, "minecraft:nether_portal", -1031, 73, -668));
        sectionTag.putBoolean("Valid", true);
        sectionTag.put("Records", recordsListTag);
        sectionContainerTag.put("4", sectionTag);

        return tag;
    }

    @Override
    protected void validateAllDataConstructor(T chunk, int expectedChunkX, int expectedChunkZ) {
        assertTrue(chunk.isPoiSectionValid(0));
        assertFalse(chunk.isPoiSectionValid(2));
        assertTrue(chunk.isPoiSectionValid(3));
        assertTrue(chunk.isPoiSectionValid(5));
        assertEquals(11, chunk.size());

        RT record = chunk.getFirst(-1032, 63, -667);
        assertEquals(new PoiRecord(-1032, 63, -667, "minecraft:cartographer"), record);
        assertEquals(1, record.getFreeTickets());  // not part of .equlas check

        List<RT> records = chunk.getAll("minecraft:home");
        assertEquals(1, records.size());
        record = records.get(0);
        assertEquals(new PoiRecord(-1032, 63, -670, "minecraft:home"), record);
        assertEquals(0, record.getFreeTickets());  // not part of .equals check

        records = chunk.getAll("minecraft:nether_portal");
        assertEquals(6, records.size());

        records = chunk.getAll("minecraft:cartographer");
        assertEquals(2, records.size());
    }

    public void testSectionsTagRequired() {
        validateTagRequired(DataVersion.latest(), "Sections");
    }

    public void testValidTagNotRequired() {
        T chunk = validateTagNotRequired(DataVersion.latest(), tag -> {
            assertNotNull(tag.getCompoundTag("Sections").getCompoundTag("2").remove("Valid"));
        });
        assertTrue(chunk.isPoiSectionValid(2));
    }

    @SuppressWarnings("unchecked")
    public void testRecordsTagNotRequired() {
        Mutable<Integer> countRemoved = new Mutable<>();
        T chunk = validateTagNotRequired(DataVersion.latest(), tag -> {
            ListTag<CompoundTag> recordsRemovedTag = (ListTag<CompoundTag>) tag.getCompoundTag("Sections").getCompoundTag("4").remove("Records");
            assertNotNull(recordsRemovedTag);
            countRemoved.set(recordsRemovedTag.size());
        });
        assertTrue(countRemoved.get() > 0);
        assertEquals(createFilledChunk(-65, -42, DEFAULT_TEST_VERSION).size() - countRemoved.get(), chunk.size());
    }

    public void testAdd() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 64, 0, "A"));
        chunk.add(createPoiRecord(0, 64, 1, "B"));
        chunk.add(createPoiRecord(1, 64, 0, "C"));
        assertEquals(3, chunk.size());

        assertThrowsIllegalArgumentException(() -> chunk.add(null));

        assertEquals(3, chunk.size());
        assertEquals("A", chunk.getAll().get(0).getType());
        assertEquals("B", chunk.getAll().get(1).getType());
        assertEquals("C", chunk.getAll().get(2).getType());
    }

    public void testIsEmpty() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        assertTrue(chunk.isEmpty());
        chunk.add(createPoiRecord(0, 64, 0, "A"));
        assertFalse(chunk.isEmpty());
    }

    public void testContains() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 64, 0, "A"));
        chunk.add(createPoiRecord(0, 64, 1, "B"));
        chunk.add(createPoiRecord(1, 64, 0, "C"));
        assertTrue(chunk.contains(createPoiRecord(0, 64, 1, "B")));
        assertFalse(chunk.contains(createPoiRecord(0, 64, 1, "D")));
        assertFalse(chunk.contains(null));
    }

    public void testClear() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 64, 0, "A"));
        chunk.invalidateSection(-3);
        assertFalse(chunk.isPoiSectionValid(-3));
        chunk.clear();
        assertTrue(chunk.isEmpty());
        assertTrue(chunk.isPoiSectionValid(-3));
    }

    public void testSectionInvalidation() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        assertTrue(chunk.isPoiSectionValid(Byte.MIN_VALUE));
        assertTrue(chunk.isPoiSectionValid(Byte.MAX_VALUE));
        chunk.invalidateSection(Byte.MIN_VALUE);
        chunk.invalidateSection(Byte.MAX_VALUE);
        assertFalse(chunk.isPoiSectionValid(Byte.MIN_VALUE));
        assertFalse(chunk.isPoiSectionValid(Byte.MAX_VALUE));
        assertTrue(chunk.isPoiSectionValid(Byte.MIN_VALUE + 1));
        assertTrue(chunk.isPoiSectionValid(Byte.MAX_VALUE - 1));

        assertThrowsIllegalArgumentException(() -> chunk.invalidateSection(Byte.MIN_VALUE - 1));
        assertThrowsIllegalArgumentException(() -> chunk.invalidateSection(Byte.MAX_VALUE + 1));
        assertThrowsIllegalArgumentException(() -> chunk.isPoiSectionValid(Byte.MIN_VALUE - 1));
        assertThrowsIllegalArgumentException(() -> chunk.isPoiSectionValid(Byte.MAX_VALUE + 1));
    }

    public void testGetFirstByXYZ() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(3, 2, 1, "A"));
        chunk.add(createPoiRecord(1, 2, 3, "B"));
        chunk.add(createPoiRecord(1, 2, 3, "C"));
        chunk.add(createPoiRecord(1, 2, -3, "D"));

        assertEquals("B", chunk.getFirst(1, 2, 3).getType());
        assertEquals("D", chunk.getFirst(1, 2, -3).getType());
        assertNull(chunk.getFirst(9, 9, 9));
    }

    public void testGetAllByXYZ() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(3, 2, 1, "A"));
        chunk.add(createPoiRecord(1, 2, 3, "B"));
        chunk.add(createPoiRecord(1, 2, 3, "C"));
        chunk.add(createPoiRecord(1, 2, -3, "D"));

        List<RT> regions = chunk.getAll(1, 2, 3);
        assertEquals(2, regions.size());
        assertEquals("B", regions.get(0).getType());
        assertEquals("C", regions.get(1).getType());

        regions = chunk.getAll(1, 2, -3);
        assertEquals(1, regions.size());
        assertEquals("D", regions.get(0).getType());

        regions = chunk.getAll(9, 9, 9);
        assertTrue(regions.isEmpty());
    }

    public void testGetAllByType() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(3, 2, 1, "A"));
        chunk.add(createPoiRecord(1, 2, 3, "B"));
        chunk.add(createPoiRecord(1, 2, 3, "C"));
        chunk.add(createPoiRecord(1, 2, -3, "A"));

        List<RT> regions = chunk.getAll("A");
        assertEquals(2, regions.size());
        assertEquals("A", regions.get(0).getType());
        assertEquals("A", regions.get(1).getType());
        assertNotSame(regions.get(0), regions.get(1));

        regions = chunk.getAll("X");
        assertTrue(regions.isEmpty());
    }

    public void testRemoveByObject() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(3, 2, 1, "A"));
        RT recordB = createPoiRecord(1, 2, 3, "B");
        chunk.add(recordB);
        chunk.add(createPoiRecord(1, 2, 3, "C"));

        // removes by ref
        assertTrue(chunk.remove(recordB));
        assertEquals(2, chunk.size());

        // not found
        assertFalse(chunk.remove(recordB));
        assertEquals(2, chunk.size());

        // null
        assertFalse(chunk.remove(null));
        assertEquals(2, chunk.size());

        // removes by equality
        assertTrue(chunk.remove(createPoiRecord(1, 2, 3, "C")));
        assertEquals(1, chunk.size());
        assertEquals("A", chunk.getAll().get(0).getType());
    }

    public void testRemoveAllByCollection() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 64, 0, "A"));
        chunk.add(createPoiRecord(0, 64, 1, "B"));
        chunk.add(createPoiRecord(1, 64, 0, "C"));
        chunk.add(createPoiRecord(2, 64, 2, "A"));
        chunk.add(createPoiRecord(0, 65, 0, "B"));

        List<Object> rem = new ArrayList<>(chunk.getAll("A"));
        rem.add(createPoiRecord(0, 64, 1, "B"));
        rem.add(null);
        rem.add(new Object());

        assertTrue(chunk.removeAll(rem));
        assertEquals(2, chunk.size());
        assertEquals("C", chunk.getAll().get(0).getType());
        assertEquals("B", chunk.getAll().get(1).getType());

        assertFalse(chunk.removeAll(rem));
        assertEquals(2, chunk.size());
    }

    public void testRemoveAllByXYZ() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(3, 2, 1, "A"));
        chunk.add(createPoiRecord(1, 2, 3, "B"));
        chunk.add(createPoiRecord(1, 2, 3, "C"));
        chunk.add(createPoiRecord(1, 2, -3, "D"));
        assertTrue(chunk.removeAll(1, 2, 3));
        assertEquals(2, chunk.size());
        List<RT> regions = chunk.getAll();
        assertEquals("A", regions.get(0).getType());
        assertEquals("D", regions.get(1).getType());
    }

    public void testRemoveAllByType() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(3, 2, 1, "A"));
        chunk.add(createPoiRecord(1, 2, 3, "B"));
        chunk.add(createPoiRecord(1, 2, 3, "A"));
        chunk.add(createPoiRecord(1, 2, -3, "D"));
        assertTrue(chunk.removeAll("A"));
        assertEquals(2, chunk.size());
        assertEquals("B", chunk.getAll().get(0).getType());
        assertEquals("D", chunk.getAll().get(1).getType());

        // not found
        assertFalse(chunk.removeAll("X"));
        assertEquals(2, chunk.size());

        // null
        assertFalse(chunk.removeAll((String) null));
        assertEquals(2, chunk.size());

        // empty
        assertFalse(chunk.removeAll(""));
        assertEquals(2, chunk.size());
    }

    public void testRemoveFirstByXYZ() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(3, 2, 1, "A"));
        chunk.add(createPoiRecord(1, 2, 3, "B"));
        chunk.add(createPoiRecord(1, 2, 3, "C"));
        assertEquals(3, chunk.size());
        RT record = chunk.removeFirst(1, 2, 3);
        assertEquals("B", record.getType());
        assertEquals(2, chunk.size());
        // not found
        assertNull(chunk.removeFirst(9, 9, 9));
        assertEquals(2, chunk.size());
    }

    public void testContainsAll() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 64, 0, "A"));
        chunk.add(createPoiRecord(0, -653, 1, "B"));
        chunk.add(createPoiRecord(1, 1587, 0, "C"));
        List<RT> ref = new ArrayList<>();
        assertTrue(chunk.containsAll(ref));
        ref.add(createPoiRecord(0, -653, 1, "B"));
        assertTrue(chunk.containsAll(ref));
        ref.add(createPoiRecord(0, 0, 0, "A"));
        assertFalse(chunk.containsAll(ref));
        ref.remove(0);
        assertFalse(chunk.containsAll(ref));
        ref.clear();
        ref.add(null);
        assertFalse(chunk.containsAll(ref));
        ref.add(createPoiRecord(0, -653, 1, "B"));
        assertFalse(chunk.containsAll(ref));
    }

    public void testAddAll() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 0, 0, "A"));
        chunk.add(createPoiRecord(0, 0, 1, "A"));
        chunk.add(createPoiRecord(0, 0, 2, "A"));

        List<RT> ref = new ArrayList<>();
        assertFalse(chunk.addAll(ref));
        ref.add(createPoiRecord(0, 0, 4, "A"));
        ref.add(createPoiRecord(0, 0, 5, "A"));
        assertTrue(chunk.addAll(ref));
        assertEquals(5, chunk.size());

        ref.clear();
        ref.add(null);
        assertFalse(chunk.addAll(ref));
        ref.add(createPoiRecord(0, 0, 6, "B"));
        assertTrue(chunk.addAll(ref));
        assertEquals(6, chunk.size());
        assertFalse(chunk.stream().anyMatch(Objects::isNull));

        // currently duplicates are not prevented
        // if that changes in the future and this fails then update this test :)
        ref.clear();
        ref.add(createPoiRecord(0, 0, 4, "A"));
        ref.add(createPoiRecord(0, 0, 5, "A"));
        assertTrue(chunk.addAll(ref));
        assertEquals(8, chunk.size());
    }

    public void testRetainAll() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 0, 0, "A"));
        chunk.add(createPoiRecord(0, 0, 1, "A"));
        chunk.add(createPoiRecord(0, 0, 2, "A"));

        List<RT> ref = new ArrayList<>();
        ref.add(createPoiRecord(0, 0, 0, "A"));
        ref.add(createPoiRecord(0, 0, 1, "A"));
        ref.add(createPoiRecord(0, 0, 2, "A"));
        assertFalse(chunk.retainAll(ref));
        assertEquals(3, chunk.size());

        ref.remove(1);
        ref.add(createPoiRecord(0, 0, 9, "B"));
        assertTrue(chunk.retainAll(ref));
        assertEquals(2, chunk.size());
        assertEquals(0, chunk.getAll().get(0).getZ());
        assertEquals(2, chunk.getAll().get(1).getZ());
    }

    public void testSet() {
        T chunk = createChunk(DEFAULT_TEST_VERSION);
        chunk.add(createPoiRecord(0, 0, 0, "A"));
        chunk.invalidateSection(2);

        List<RT> ref = new ArrayList<>();
        ref.add(createPoiRecord(0, 0, 0, "B"));
        ref.add(createPoiRecord(0, 0, 1, "A"));
        chunk.set(ref);
        assertEquals(2, chunk.size());
        assertFalse(chunk.contains(createPoiRecord(0, 0, 0, "A")));
        assertTrue(chunk.contains(createPoiRecord(0, 0, 0, "B")));
        assertTrue(chunk.contains(createPoiRecord(0, 0, 1, "A")));
        assertTrue(chunk.isPoiSectionValid(2));

        chunk.invalidateSection(2);
        chunk.set(null);
        assertEquals(0, chunk.size());
        assertTrue(chunk.isPoiSectionValid(2));
    }

    public void testTypeFilteredIterator() {
        T chunk = createFilledChunk(-65, -42, DEFAULT_TEST_VERSION);
        final int originalSize = chunk.size();
        Iterator<RT> iter = chunk.iterator("minecraft:nether_portal");
        assertNotNull(iter);
        for (int i = 0; i < 6; i++) {
            assertTrue(iter.hasNext());
            assertNotNull(iter.next());
        }
        assertFalse(iter.hasNext());

        assertNotNull(chunk.iterator(""));
        assertNotNull(chunk.iterator(null));
        assertFalse(chunk.iterator("").hasNext());
        assertFalse(chunk.iterator(null).hasNext());
    }

    public void testUpdateHandle() {
        // identity
        CompoundTag expectedTag = createTag(DEFAULT_TEST_VERSION.id(), -65, -42);
        T chunk = createFilledChunk(-65, -42, DEFAULT_TEST_VERSION);
        chunk.getHandle().clear();
        assertEquals(expectedTag, chunk.updateHandle());

        // writes empty section if it's marked invalid
        chunk.invalidateSection(Byte.MAX_VALUE);
        CompoundTag newSection = new CompoundTag();
        newSection.putBoolean("Valid", false);
        newSection.put("Records", new ListTag<>(CompoundTag.class));
        expectedTag.getCompoundTag("Sections").put(Integer.toString(Byte.MAX_VALUE), newSection);
        assertEquals(expectedTag, chunk.updateHandle());
    }
}
