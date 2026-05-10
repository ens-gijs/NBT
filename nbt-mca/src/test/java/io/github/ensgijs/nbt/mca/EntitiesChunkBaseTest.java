package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.mca.entities.Entity;
import io.github.ensgijs.nbt.mca.entities.EntityBase;
import io.github.ensgijs.nbt.mca.entities.EntityFactory;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.MoveChunkFlags;
import io.github.ensgijs.nbt.mca.util.ChunkBoundingRectangle;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class EntitiesChunkBaseTest<ET extends Entity, T extends EntitiesChunkBase<ET>> extends ChunkBaseTest<T> {

    @Override
    protected CompoundTag createTag(int dataVersion, int chunkX, int chunkZ) {
        CompoundTag tag = super.createTag(dataVersion, chunkX, chunkZ);
        ListTag<CompoundTag> entitiesTagList;
        if (dataVersion >= DataVersion.JAVA_1_17_20W45A.id()) {
            tag.putIntArray("Position", new int[]{chunkX, chunkZ});
            tag.put("Entities", entitiesTagList = new ListTag<>(CompoundTag.class));
        } else {
            CompoundTag level = new CompoundTag();
            tag.put("Level", level);
            level.putInt("xPos", chunkX);
            level.putInt("zPos", chunkZ);
            level.put("Entities", entitiesTagList = new ListTag<>(CompoundTag.class));
        }

        if (dataVersion > 0) {
            // Depend on the efficacy of the EntityBase tests to make life easier here
            ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(chunkX, chunkZ);
            entitiesTagList.add(new EntityBase(dataVersion, "minecraft:zombie",
                    cbr.relocateX(5), 68, cbr.relocateZ(7), 42, -5).updateHandle());
            entitiesTagList.add(new EntityBase(dataVersion, "minecraft:skeleton",
                    cbr.relocateX(14), 64, cbr.relocateZ(2), 297, 2.54f).updateHandle());
        }
        return tag;
    }

    protected abstract ET createEntity(int dataVersion, String id, double x, double y, double z, float yaw, float pitch);

    @Override
    protected void validateAllDataConstructor(T chunk, int expectedChunkX, int expectedChunkZ) {
        assertEquals(expectedChunkX, chunk.getChunkX());
        assertEquals(expectedChunkZ, chunk.getChunkZ());
        assertNotNull(chunk.getEntitiesTag());
        ListTag<CompoundTag> entitiesTagList = chunk.getEntitiesTag();
        List<ET> entities = chunk.getEntities();
        assertEquals(2, entitiesTagList.size());
        assertEquals(2, entities.size());
        assertEquals(entitiesTagList.get(0).getString("id"), entities.get(0).getId());
        assertEquals(entitiesTagList.get(1).getString("id"), entities.get(1).getId());
        assertTrue(chunk.moveChunkImplemented());
        assertTrue(chunk.moveChunkHasFullVersionSupport());
    }

    public void testInitReferences_throwsWhenMissingEntitiesTag_MC_GE_1_17() {
        CompoundTag tag = createTag(DataVersion.JAVA_1_17_20W45A.id(), 1, 7);
        assertThrowsNoException(() -> new EntitiesChunk(tag));
        tag.remove("Entities");
        assertThrowsIllegalArgumentException(() -> new EntitiesChunk(tag));
    }

    public void testInitReferences_throwsWhenMissingPositionTag_MC_GE_1_17() {
        CompoundTag tag = createTag(DataVersion.JAVA_1_17_20W45A.id(), 1, 7);
        assertThrowsNoException(() -> new EntitiesChunk(tag));
        tag.remove("Position");
        assertThrowsIllegalArgumentException(() -> new EntitiesChunk(tag));
    }

    public void testInitReferences_throwsWhenReadingPre_MC_1_17() {
        CompoundTag tag = createTag(DataVersion.JAVA_1_16_5.id(), 1, 7);
        assertThrowsUnsupportedOperationException(() -> new EntitiesChunk(tag));
    }

    public void testInitEntities_throwsWhenEntitiesTagIsNull() {
        T chunk = createFilledChunk(2, 8, DataVersion.JAVA_1_17_1);
        chunk.entitiesTag = null;
        assertThrowsException(chunk::initEntities, IllegalStateException.class);
    }

    public void testRawLoad() throws IOException {
        final int dataVersion = DataVersion.JAVA_1_17_1.id();
        CompoundTag mutableTag = createTag(dataVersion, -4, 2);
        CompoundTag originalTag = mutableTag.clone();
        T  chunk = createChunk(mutableTag, LoadFlags.RAW);
        assertEquals(ChunkBase.NO_CHUNK_COORD_SENTINEL, chunk.getChunkX());
        assertEquals(ChunkBase.NO_CHUNK_COORD_SENTINEL, chunk.getChunkZ());
        assertThrowsUnsupportedOperationException(chunk::getEntities);
        assertThrowsUnsupportedOperationException(chunk::getEntitiesTag);
        assertThrowsUnsupportedOperationException(chunk::clearEntities);
        assertThrowsException(() -> chunk.fixEntityLocations(0), IllegalStateException.class);
        assertThrowsUnsupportedOperationException(chunk::iterator);
        assertThrowsUnsupportedOperationException(chunk::spliterator);
        assertThrowsUnsupportedOperationException(chunk::stream);
        assertThrowsUnsupportedOperationException(() -> chunk.forEach((c) -> {throw new RuntimeException();}));

        assertEquals(originalTag, chunk.updateHandle(5, 6));
        assertEquals(ChunkBase.NO_CHUNK_COORD_SENTINEL, chunk.getChunkX());
        assertEquals(ChunkBase.NO_CHUNK_COORD_SENTINEL, chunk.getChunkZ());

        // moving is supported in raw if we have an entities tag
        assertTrue(chunk.moveChunkImplemented());
        assertTrue(chunk.moveChunkHasFullVersionSupport());
        assertThrowsNoException(() -> chunk.moveChunk(1, -2, MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS, true));
        assertEquals(1, chunk.getChunkX());
        assertEquals(-2, chunk.getChunkZ());

        List<ET> entities = new ArrayList<>();
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(-4, 2);
        entities.add(createEntity(dataVersion, "minecraft:sheep", cbr.relocateX(12), 73, cbr.relocateZ(2.3), 12, 0));
        entities.add(createEntity(dataVersion, "minecraft:pig", cbr.relocateX(9.87), 71, cbr.relocateZ(7.998), 220, 20));
        entities.add(createEntity(dataVersion, "minecraft:pig", cbr.relocateX(7.76), 71, cbr.relocateZ(6.123), 77, 0));
        assertThrowsUnsupportedOperationException(() -> chunk.setEntities(entities));

        // you can set an entities tag, but it doesn't make getting the tag or wrapped entities valid
        ListTag<CompoundTag> newEntitiesTag = EntityFactory.toListTag(entities);
        assertThrowsNoException(() -> chunk.setEntitiesTag(newEntitiesTag));
        assertSame(newEntitiesTag, mutableTag.get("Entities"));
        assertThrowsUnsupportedOperationException(chunk::getEntities);
        assertThrowsUnsupportedOperationException(chunk::getEntitiesTag);

        // ... but we can now fix entity locations (because we previously moved the chunk so XZ is known)
        assertTrue(chunk.fixEntityLocations(MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS));
        // System.out.println(TextNbtHelpers.toTextNbt(mutableTag));
    }

    public void testSetEntities() {
        final int dataVersion = DataVersion.JAVA_1_17_1.id();
        T  chunk = createFilledChunk(4, 2, DataVersion.JAVA_1_17_1);
        List<ET> entities = new ArrayList<>();
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(-4, 2);
        entities.add(createEntity(dataVersion, "minecraft:sheep", cbr.relocateX(12), 73, cbr.relocateZ(2.3), 12, 0));
        entities.add(createEntity(dataVersion, "minecraft:pig", cbr.relocateX(9.87), 71, cbr.relocateZ(7.998), 220, 20));
        entities.add(createEntity(dataVersion, "minecraft:pig", cbr.relocateX(7.76), 71, cbr.relocateZ(6.123), 77, 0));
        chunk.setEntities(entities);

        assertSame(entities, chunk.getEntities());
        // setting entities doesn't overwrite entities tag if one exists
        assertEquals(2, chunk.getEntitiesTag().size());
        assertEquals("minecraft:zombie", chunk.getEntitiesTag().get(0).getString("id"));

        chunk.updateHandle();
        assertEquals(3, chunk.getEntitiesTag().size());
        assertEquals("minecraft:sheep", chunk.getEntitiesTag().get(0).getString("id"));
    }

    public void testSetEntities_whenLoadFlagsDidNotReadEntities() {
        final int dataVersion = DataVersion.JAVA_1_17_1.id();
        CompoundTag tag = createTag(dataVersion, -4, 2);
        T  chunk = createChunk(tag, LoadFlags.BIOMES);
        assertEquals(-4, chunk.getChunkX());
        assertEquals(2, chunk.getChunkZ());
        assertNull(chunk.getEntitiesTag());
        List<ET> entities = new ArrayList<>();
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(-4, 2);
        entities.add(createEntity(dataVersion, "minecraft:sheep", cbr.relocateX(12), 73, cbr.relocateZ(2.3), 12, 0));
        entities.add(createEntity(dataVersion, "minecraft:pig", cbr.relocateX(9.87), 71, cbr.relocateZ(7.998), 220, 20));
        entities.add(createEntity(dataVersion, "minecraft:pig", cbr.relocateX(7.76), 71, cbr.relocateZ(6.123), 77, 0));
        chunk.setEntities(entities);

        assertSame(entities, chunk.getEntities());
        assertNull(chunk.getEntitiesTag());
    }

    public void testAreWrappedEntitiesGenerated() {
        T chunk = createFilledChunk(1, 0, DataVersion.latest());
        assertFalse(chunk.areWrappedEntitiesGenerated());
        chunk.getEntities();  // trigger lazy initialization
        assertTrue(chunk.areWrappedEntitiesGenerated());
        chunk.clearEntities();
    }

    public void testClearEntities() {
        T chunk = createFilledChunk(1, 0, DataVersion.latest());
        ListTag<CompoundTag> entitiesTag = chunk.getEntitiesTag();
        assertNotNull(entitiesTag);
        assertFalse(entitiesTag.isEmpty());
        assertFalse(chunk.getEntities().isEmpty());  // trigger lazy initialization too
        chunk.clearEntities();
        assertFalse(chunk.areWrappedEntitiesGenerated());  // wrapped entities were nuked
        assertNotSame(entitiesTag, chunk.getEntitiesTag());  // reference changed
        assertFalse(entitiesTag.isEmpty());  // held ref didn't get cleared
        assertTrue(chunk.getEntities().isEmpty());
        assertTrue(chunk.areWrappedEntitiesGenerated());
    }

    public void testSetEntitiesTagInternal_MC_GE_1_17_20W45A() {
        // normal case - chunk data fully loadedv
        final ListTag<CompoundTag> newEntitiesTag = new ListTag<>(CompoundTag.class);
        T chunk = createFilledChunk(1, 0, DataVersion.JAVA_1_17_20W45A);
        assertFalse(chunk.areWrappedEntitiesGenerated());
        chunk.getEntities();  // trigger lazy initialization
        assertTrue(chunk.areWrappedEntitiesGenerated());
        chunk.setEntitiesTagInternal(newEntitiesTag);
        assertFalse(chunk.areWrappedEntitiesGenerated());  // wrapped entities nuked
        assertSame(newEntitiesTag, chunk.getEntitiesTag());  // reference changed
        assertSame(newEntitiesTag, chunk.updateHandle().getListTag("Entities"));  // given ref now put into handle tag
    }

    public void testSetEntitiesTag_valueCannotBeNull() {
        T chunk = createFilledChunk(1, 0, DataVersion.JAVA_1_17_20W45A);
        assertThrowsIllegalArgumentException(() -> chunk.setEntitiesTag(null));
    }

    public void testMoveChunk() {
        // white box testing - let fixEntityLocations tests cover actual entity relocation validation
        T chunk = createFilledChunk(1, 0, DataVersion.JAVA_1_17_1);
        chunk.moveChunk(-2, 3, MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS);
        assertEquals(-2, chunk.getChunkX());
        assertEquals(3, chunk.getChunkZ());
    }

    public void testMoveChunk_doublePassengers_1_20_4() throws IOException {
        validateMoveChunk_doublePassengers_1_20_4(LoadFlags.LOAD_ALL_DATA);
        validateMoveChunk_doublePassengers_1_20_4(LoadFlags.RAW);
    }
    private void validateMoveChunk_doublePassengers_1_20_4(long loadFlags) throws IOException {
        EntitiesChunk chunk = new EntitiesChunk(
                TextNbtHelpers.readTextNbtFile(getResourceFile("1_20_4/entities/double_passengers.snbt")).getTagAutoCast(),
                loadFlags);
        assertTrue(chunk.moveChunk(10, 10, MoveChunkFlags.AUTOMATICALLY_UPDATE_HANDLE));
//        System.out.println("WROTE " + McaDumper.dumpChunkAsTextNbtAutoFilename(chunk, Paths.get("TESTDBG")).toAbsolutePath());
        CompoundTag expectedTag = TextNbtHelpers.readTextNbtFile(getResourceFile("1_20_4/entities/double_passengers_moveto_10.10_expected.snbt")).getTagAutoCast();
        assertEquals(expectedTag, chunk.getHandle());
    }

    public void testFixEntityLocations() {
        T chunk = createFilledChunk(0, 0, DataVersion.latest());
        // white-box: behavior is different if wrapped entities are generated or not, start with not generated
        assertFalse(chunk.getEntitiesTag().isEmpty());

        final double[] initialPos0 = chunk.getEntitiesTag().get(0).getDoubleTagListAsArray("Pos");
        final double[] initialPos1 = chunk.getEntitiesTag().get(1).getDoubleTagListAsArray("Pos");
        assertFalse(chunk.fixEntityLocations(MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS));
        Assert.assertArrayEquals(initialPos0, chunk.getEntitiesTag().get(0).getDoubleTagListAsArray("Pos"), 1e-6);
        Assert.assertArrayEquals(initialPos1, chunk.getEntitiesTag().get(1).getDoubleTagListAsArray("Pos"), 1e-6);

        chunk.getEntitiesTag().get(0).putDoubleArrayAsTagList("Pos", initialPos0[0] + 16, initialPos0[1], initialPos0[2]);
        chunk.getEntitiesTag().get(1).putDoubleArrayAsTagList("Pos", initialPos1[0], initialPos1[1], initialPos1[2] - 32);
        assertTrue(chunk.fixEntityLocations(MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS));
        Assert.assertArrayEquals(initialPos0, chunk.getEntitiesTag().get(0).getDoubleTagListAsArray("Pos"), 1e-6);
        Assert.assertArrayEquals(initialPos1, chunk.getEntitiesTag().get(1).getDoubleTagListAsArray("Pos"), 1e-6);

        // now test with wrapped entities
        List<ET> entities = chunk.getEntities();
        entities.get(0).setX(initialPos0[0] - 64);
        entities.get(1).setZ(initialPos1[2] + 128);
        assertTrue(chunk.fixEntityLocations(MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS));
        Assert.assertArrayEquals(initialPos0, new double[]{entities.get(0).getX(), entities.get(0).getY(), entities.get(0).getZ()}, 1e-6);
        Assert.assertArrayEquals(initialPos1, new double[]{entities.get(1).getX(), entities.get(1).getY(), entities.get(1).getZ()}, 1e-6);
    }

//    public void testSetEntitiesTag_valueCannotBeNull() {
//        CompoundTag tag = createTag(1, 0, DataVersion.JAVA_1_17_1.id());
//        T chunk = createChunk(tag, LoadFlags.RAW);
//        assertThrowsIllegalArgumentException(() -> chunk.setEntitiesTag(null));
//    }
}
