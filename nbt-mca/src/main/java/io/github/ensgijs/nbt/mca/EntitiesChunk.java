package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.mca.entities.Entity;
import io.github.ensgijs.nbt.mca.entities.EntityFactory;

/**
 * Thin default implementation of {@link EntitiesChunkBase}.
 *
 * @see EntitiesChunkBase
 * @see EntityFactory
 * @see McaFileHelpers#MCA_CREATORS
 */
public class EntitiesChunk extends EntitiesChunkBase<Entity> {

    protected EntitiesChunk(int dataVersion) {
        super(dataVersion);
    }

    public EntitiesChunk(CompoundTag data) {
        super(data);
    }

    public EntitiesChunk(CompoundTag data, long loadFlags) {
        super(data, loadFlags);
    }

    public EntitiesChunk() {
        super(DataVersion.latest().id());
    }
}
