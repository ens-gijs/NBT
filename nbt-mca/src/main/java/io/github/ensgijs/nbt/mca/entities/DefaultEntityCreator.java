package io.github.ensgijs.nbt.mca.entities;

import io.github.ensgijs.nbt.tag.CompoundTag;

/**
 * The default {@link EntityCreator} which creates {@link EntityBase} that can represent any, and all, entities
 * in vanilla Minecraft MCA files.
 */
public class DefaultEntityCreator implements EntityCreator<Entity> {

    /** @see EntityCreator#create(String, CompoundTag, int) */
    @Override
    public Entity create(String normalizedId, CompoundTag tag, int dataVersion) {
        return new EntityBase(tag, dataVersion);
    }
}
