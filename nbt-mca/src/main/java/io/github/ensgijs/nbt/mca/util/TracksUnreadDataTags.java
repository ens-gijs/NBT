package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.tag.CompoundTag;

import java.util.*;

public interface TracksUnreadDataTags {
    /**
     * Gets the unmodifiable set of data tag keys which were not read during initialization.
     * @return Nullable - null if LoadFags contained {@link LoadFlags#RAW} - else the unmodifiable set of unread key names
     */
    Set<String> getUnreadDataTagKeys();

    /**
     * Gets a new CompoundTag containing all entries which were not read during initialization.
     * Note that the returned tag values are by reference (linked to the values in the {@link CompoundTag} provided
     * for initialization) so modifying values in the returned tag will also modify the underlying data, however,
     * adding or removing elements directly from the returned tag has no effect.
     * <p>Basically know what you're doing when modifying the returned value - or don't modify it at all.</p>
     * @return NotNull - if LoadFlags specified {@link LoadFlags#RAW} then the raw data is returned - else a new
     * CompoundTag populated, by reference, with values that were not read during initialization.
     */
    CompoundTag getUnreadDataTags();
}
