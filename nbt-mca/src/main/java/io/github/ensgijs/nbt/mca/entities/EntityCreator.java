package io.github.ensgijs.nbt.mca.entities;


import io.github.ensgijs.nbt.tag.CompoundTag;

/**
 * Intended for use by {@link EntityFactory} to create custom entity classes when parsing NBT tags.
 */
@FunctionalInterface
public interface EntityCreator<T extends Entity> {

    /**
     * @param normalizedId normalized entity id, with no "minecraft:" prefix and all UPPER CASE.
     *                     Note this is always the result of {@link EntityFactory#normalizeAndRemapId(String)} and may
     *                     not match the id tag found in the nbt data when reading from old chunks.
     * @param tag tag containing entity data
     * @param dataVersion data version of chunk / tag
     * @return NOT NULL, if an implementer cannot process the given tag it should throw an
     * {@link IllegalEntityTagException}
     * @throws IllegalEntityTagException Thrown if this creator cannot return a result.
     */
    T create(String normalizedId, CompoundTag tag, int dataVersion);
}
