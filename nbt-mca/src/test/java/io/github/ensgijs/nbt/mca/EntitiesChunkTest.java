package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.entities.Entity;
import io.github.ensgijs.nbt.mca.entities.EntityBase;
import io.github.ensgijs.nbt.tag.CompoundTag;

public class EntitiesChunkTest extends EntitiesChunkBaseTest<Entity, EntitiesChunk> {

    @Override
    protected EntitiesChunk createChunk(DataVersion dataVersion) {
        return new EntitiesChunk(dataVersion.id());
    }

    @Override
    protected EntitiesChunk createChunk(CompoundTag tag) {
        return new EntitiesChunk(tag);
    }

    @Override
    protected EntitiesChunk createChunk(CompoundTag tag, long loadFlags) {
        return new EntitiesChunk(tag, loadFlags);
    }

    @Override
    protected Entity createEntity(int dataVersion, String id, double x, double y, double z, float yaw, float pitch) {
        return new EntityBase(dataVersion, id, x, y, z, yaw, pitch);
    }

    @Override
    public void testDataVersion() {
        EntitiesChunk chunk = createChunk(createTag(DataVersion.JAVA_1_17_0.id(), 0, 0));
        assertEquals(DataVersion.JAVA_1_17_0.id(), chunk.getDataVersion());
        assertEquals(DataVersion.JAVA_1_17_0, chunk.getDataVersionEnum());
        chunk.setDataVersion(DataVersion.JAVA_1_17_1.id());
        assertEquals(DataVersion.JAVA_1_17_1.id(), chunk.getDataVersion());
        assertEquals(DataVersion.JAVA_1_17_1, chunk.getDataVersionEnum());
    }

    public void testSetDataVersion_cannotCross_JAVA_1_17_20W45A() {
        EntitiesChunk chunk = createChunk(DataVersion.JAVA_1_16_5);
        assertThrowsException(() -> chunk.setDataVersion(DataVersion.JAVA_1_17_20W45A.id()), UnsupportedVersionChangeException.class);
        assertThrowsNoException(() -> chunk.setDataVersion(DataVersion.JAVA_1_16_0.id()));

        EntitiesChunk chunk2 = createChunk(DataVersion.JAVA_1_17_20W45A);
        assertThrowsNoException(() -> chunk2.setDataVersion(DataVersion.JAVA_1_17_0.id()));
    }
}

