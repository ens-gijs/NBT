package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.tag.Tag;

public interface TagWrapper<T extends Tag<?>> {

    /**
     * Provides a reference to the wrapped data tag.
     * May be null for objects which support partial loading such as chunks.
     * @return A reference to the raw Tag this object is based on.
     */
    T getHandle();

    /**
     * Updates the data tag held by this wrapper and returns it.
     * @return A reference to the raw Tag this object is based on.
     */
    T updateHandle();
}
