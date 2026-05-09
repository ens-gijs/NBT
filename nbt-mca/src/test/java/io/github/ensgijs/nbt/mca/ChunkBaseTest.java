package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.util.Mutable;

import java.util.function.Consumer;

// TODO: test non-CompoundTag ctors and getHandle() / updateHandle()
/**
 * All implementors of {@link ChunkBaseTest} should create a test which inherits this one and add
 * tests to cover any additional functionality added by that concretion.
 */
public abstract class ChunkBaseTest<T extends ChunkBase> extends NbtTestCase {
    protected abstract T createChunk(DataVersion dataVersion);
    protected abstract T createChunk(CompoundTag tag);
    protected abstract T createChunk(CompoundTag tag, long loadFlags);

    /**
     * helper equivalent to {@code createChunk(createTag(dataVersion.id()))}
     */
    final protected T createFilledChunk(int chunkX, int chunkZ, DataVersion dataVersion) {
        return createChunk(createTag(dataVersion.id(), chunkX, chunkZ));
    }

    public void testChunkBase_defaultConstructor() {
        T chunk = createChunk(DataVersion.latest());
        int now = (int)(System.currentTimeMillis() / 1000);
        assertEquals(DataVersion.latest().id(), chunk.getDataVersion());
        assertEquals(DataVersion.latest(), chunk.getDataVersionEnum());
        assertTrue(Math.abs(now - chunk.getLastMCAUpdate()) <= 1);
    }

    public void testLastMcaUpdated() {
        T chunk = createChunk(DataVersion.latest());
        chunk.setLastMCAUpdate(1747522);
        assertEquals(1747522, chunk.getLastMCAUpdate());
    }

    public void testDataVersion() {
        T chunk = createChunk(createTag(DataVersion.JAVA_1_16_0.id(), 0, 0));
        assertEquals(DataVersion.JAVA_1_16_0.id(), chunk.getDataVersion());
        assertEquals(DataVersion.JAVA_1_16_0, chunk.getDataVersionEnum());
        chunk.setDataVersion(DataVersion.JAVA_1_16_1.id());
        assertEquals(DataVersion.JAVA_1_16_1.id(), chunk.getDataVersion());
        assertEquals(DataVersion.JAVA_1_16_1, chunk.getDataVersionEnum());
        chunk.setDataVersion(DataVersion.JAVA_1_16_2);
        assertEquals(DataVersion.JAVA_1_16_2.id(), chunk.getDataVersion());
        assertEquals(DataVersion.JAVA_1_16_2, chunk.getDataVersionEnum());
    }

    /**
     * Must be overridden to support chunkX chunkZ because different chunks store this information differently
     * (or not at all).
     * @param dataVersion set as "DataVersion" in returned tag IFF GT 0
     */
    protected CompoundTag createTag(int dataVersion, int chunkX, int chunkZ) {
        CompoundTag tag = new CompoundTag();
        if (dataVersion > 0) tag.putInt("DataVersion", dataVersion);
        return tag;
    }

    /**
     * Override {@link #createTag} and populate it with interesting stuff then validate that stuff
     * is properly loaded when the chunk is instantiated with {@link LoadFlags#LOAD_ALL_DATA}
     */
    protected abstract void validateAllDataConstructor(T chunk, int expectedChunkX, int expectedChunkZ);

    protected void validateTagRequired(DataVersion dataVersion, String tagName) {
        validateTagRequired(dataVersion, tag -> tag.remove(tagName));
    }

    protected void validateTagRequired(DataVersion dataVersion, Consumer<CompoundTag> tagRemover) {
        assertThrowsException(() -> {
            CompoundTag tag = createTag(dataVersion.id(), 0, 0);
            assertNotNull(tag);
            tagRemover.accept(tag);
            createChunk(tag, LoadFlags.LOAD_ALL_DATA);
        }, IllegalArgumentException.class);
    }

    /**
     * @return the resulting chunk
     */
    protected T validateTagNotRequired(DataVersion dataVersion, String tagName) {
        return validateTagNotRequired(dataVersion, tag -> tag.remove(tagName));
    }

    /**
     * @return the resulting chunk
     */
    protected T validateTagNotRequired(DataVersion dataVersion, Consumer<CompoundTag> tagRemover) {
        Mutable<T> out = new Mutable<>();
        assertThrowsNoException(() -> {
            CompoundTag tag = createTag(dataVersion.id(), 0, 0);
            assertNotNull(tag);
            tagRemover.accept(tag);
            out.set(createChunk(tag, LoadFlags.LOAD_ALL_DATA));
        });
        return out.get();
    }

    final public void testConstructor_allData() {
        CompoundTag tag = createTag(DataVersion.JAVA_1_17_1.id(), -4, 7);
        assertNotNull(tag);
        T chunk = createChunk(tag, LoadFlags.LOAD_ALL_DATA);
        assertEquals(DataVersion.JAVA_1_17_1.id(), chunk.getDataVersion());
        assertEquals(DataVersion.JAVA_1_17_1, chunk.getDataVersionEnum());
        assertFalse(chunk.partial);
        assertFalse(chunk.raw);
        assertSame(tag, chunk.getHandle());
        validateAllDataConstructor(chunk, -4, 7);
    }

    final public void testConstructor_raw() {
        CompoundTag tag = createTag(DataVersion.JAVA_1_16_5.id(), 0, 0);
        assertNotNull(tag);
        T chunk = createChunk(tag, LoadFlags.RAW);
        assertEquals(DataVersion.JAVA_1_16_5.id(), chunk.getDataVersion());
        assertEquals(DataVersion.JAVA_1_16_5, chunk.getDataVersionEnum());
        assertFalse(chunk.partial);
        assertTrue(chunk.raw);
        assertSame(tag, chunk.getHandle());
        assertThrowsException(chunk::checkRaw, UnsupportedOperationException.class);
    }

    public void testConstructor_allData_throwsIfDataVersionNotFound() {
        CompoundTag tag = createTag(-1, 0, 0);
        assertNotNull(tag);
        assertThrowsException(() -> createChunk(tag, LoadFlags.LOAD_ALL_DATA), IllegalArgumentException.class);
    }

    public void testConstructor_raw_noThrowIfDataVersionNotFound() {
        CompoundTag tag = createTag(-1, 0, 0);
        assertNotNull(tag);
        assertThrowsNoException(() -> createChunk(tag, LoadFlags.RAW));
    }
}
