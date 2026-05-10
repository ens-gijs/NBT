package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;

public class TerrainChunkTest extends TerrainChunkBaseTest<TerrainChunk> {
    @Override
    protected TerrainChunk createChunk(DataVersion dataVersion) {
        return TerrainChunk.newChunk(dataVersion.id());
    }

    @Override
    protected TerrainChunk createChunk(CompoundTag tag) {
        return new TerrainChunk(tag);
    }

    @Override
    protected TerrainChunk createChunk(CompoundTag tag, long loadFlags) {
        return new TerrainChunk(tag, loadFlags);
    }

    @Override
    protected CompoundTag createTag(int dataVersion, int chunkX, int chunkZ) {
        final CompoundTag tag = super.createTag(dataVersion, chunkX, chunkZ);
        if (dataVersion < DataVersion.JAVA_1_18_21W39A.id()) {
            tag.put("Level", new CompoundTag());
        }
        if (dataVersion < 0) return tag;
        // TODO: there should probably be a ctor that sets this up for library users to help create new, empty, chunks
        TerrainChunkBase.X_POS_PATH.get(dataVersion).put(tag, chunkX);
        TerrainChunkBase.Z_POS_PATH.get(dataVersion).put(tag, chunkZ);
        TerrainChunkBase.INHABITED_TIME_TICKS_PATH.get(dataVersion).put(tag, 42L);
        TerrainChunkBase.POST_PROCESSING_PATH.get(dataVersion).putTag(tag, new ListTag<ListTag<?>>(ListTag.class));
        TerrainChunkBase.STATUS_PATH.get(dataVersion).put(tag, "empty");
        TerrainChunkBase.TILE_ENTITIES_PATH.get(dataVersion).putTag(tag, new ListTag<>(CompoundTag.class));
        TerrainChunkBase.TILE_TICKS_PATH.get(dataVersion).putTag(tag, new ListTag<>(CompoundTag.class));
        TerrainChunkBase.LIQUID_TICKS_PATH.get(dataVersion).putTag(tag, new ListTag<>(CompoundTag.class));
        TerrainChunkBase.IS_LIGHT_ON_PATH.get(dataVersion).put(tag, true);
        TerrainChunkBase.SECTIONS_PATH.get(dataVersion).putTag(tag, new ListTag<>(CompoundTag.class));
        CompoundTag structuresTag = new CompoundTag();
        TerrainChunkBase.STRUCTURES_PATH.get(dataVersion).putTag(tag, structuresTag);
        TerrainChunkBase.STRUCTURES_REFERENCES_PATH.get(dataVersion).putTag(structuresTag, new CompoundTag());
        TerrainChunkBase.STRUCTURES_STARTS_PATH.get(dataVersion).putTag(structuresTag, new CompoundTag());
        if (TerrainChunkBase.Y_POS_PATH.get(dataVersion) != null) {
            TerrainChunkBase.Y_POS_PATH.get(dataVersion).put(tag, TerrainChunkBase.DEFAULT_WORLD_BOTTOM_Y_POS.get(dataVersion));
        }
        return tag;
    }

    @Override
    protected void validateAllDataConstructor(TerrainChunk chunk, int expectedChunkX, int expectedChunkZ) {
        // TODO override createTag and put interesting stuff in the chunk and validate it here
        assertEquals(expectedChunkX, chunk.getChunkX());
        assertEquals(expectedChunkZ, chunk.getChunkZ());
    }
}
