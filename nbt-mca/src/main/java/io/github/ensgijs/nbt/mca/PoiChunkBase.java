package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.mca.io.MoveChunkFlags;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.IntArrayTag;
import io.github.ensgijs.nbt.tag.ListTag;
import io.github.ensgijs.nbt.mca.util.ChunkBoundingRectangle;
import io.github.ensgijs.nbt.mca.util.RegionBoundingRectangle;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides all the basic functionality necessary for this type of chunk with abstraction hooks
 * making it easy to extend this class and modify the factory behavior of {@link McaFileHelpers} to create
 * instances of your custom class.
 */
public abstract class PoiChunkBase<T extends PoiRecord> extends ChunkBase implements Collection<T> {
    // private to preserve the ability to change how records are stored to optimize lookups later
    private List<T> records;

    // Valid: True (1) when created by the game, however, if the decoding of POI NBT (from the region file) data fails,
    // and the game then save the region file again, it might save false (0). This key is internally set to true when
    // the POI section is refreshed, and a refresh always happens when the chunk section (with terrain data) at the
    // same coordinates is decoded. To sum up, it is very unlikely to get false.
    protected Map<Integer, Boolean> poiSectionValidity;

    @Override
    protected void initMembers() {
        records = null;
        poiSectionValidity = new HashMap<>();
    }

    protected PoiChunkBase(int dataVersion) {
        super(dataVersion);
        records = new ArrayList<>();
    }

    public PoiChunkBase(CompoundTag data) {
        super(data);
    }

    public PoiChunkBase(CompoundTag data, long loadFlags) {
        super(data, loadFlags);
    }

    @Override
    protected void initReferences(long loadFlags) {
        if ((loadFlags & LoadFlags.POI_RECORDS) != 0) {
            records = new ArrayList<>();
            CompoundTag sectionsTag = data.getCompoundTag("Sections");
            if (sectionsTag == null) {
                throw new IllegalArgumentException("Sections tag not found!");
            }
            for (NamedTag sectionTag : sectionsTag) {
                int sectionY = Integer.parseInt(sectionTag.getName());
                boolean valid = ((CompoundTag) sectionTag.getTag()).getBoolean("Valid", true);
                poiSectionValidity.put(sectionY, valid);
                ListTag<CompoundTag> recordTags = ((CompoundTag) sectionTag.getTag()).getListTagAutoCast("Records");
                if (recordTags != null) {
                    for (CompoundTag recordTag : recordTags) {
                        T record = createPoiRecord(recordTag);
                        if (sectionY != record.getSectionY()) {
                            poiSectionValidity.put(sectionY, false);
                        }
                        records.add(record);
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    public String getMcaType() {
        return "poi";
    }

    @Override
    public boolean moveChunkImplemented() {
        return records != null || data != null;
    }

    @Override
    public boolean moveChunkHasFullVersionSupport() {
        return records != null || data != null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean moveChunk(int newChunkX, int newChunkZ, long moveChunkFlags, boolean force) {
        if (!moveChunkImplemented())
            throw new UnsupportedOperationException("Missing the data required to move this chunk!");
        if (!RegionBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.containsChunk(chunkX, chunkZ)) {
            throw new IllegalArgumentException("Chunk XZ must be within the maximum world bounds.");
        }
        // remember poi chunk nbt doesn't contain XZ location
        this.chunkX = newChunkX;
        this.chunkZ = newChunkZ;
        if (fixPoiLocations(moveChunkFlags)) {
            if ((moveChunkFlags & MoveChunkFlags.AUTOMATICALLY_UPDATE_HANDLE) > 0) {
                updateHandle();
            }
            return true;
        }
        return false;
    }

    public boolean fixPoiLocations(long moveChunkFlags) {
        if (!moveChunkImplemented())
            throw new UnsupportedOperationException("Missing the data required to move this chunk!");
        if (this.chunkX == NO_CHUNK_COORD_SENTINEL || this.chunkZ == NO_CHUNK_COORD_SENTINEL) {
            throw new IllegalStateException("Chunk XZ not known");
        }
        boolean changed = false;
        final ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(chunkX, chunkZ);
        if (!raw && records != null) {
            for (T entity : records) {
                if (!cbr.containsBlock(entity.getX(), entity.getZ())) {
                    entity.setX(cbr.relocateX(entity.getX()));
                    entity.setZ(cbr.relocateZ(entity.getZ()));
                    changed = true;
                }
            }
        } else {  // fix raw data
            if (data == null) {
                throw new UnsupportedOperationException(
                        "Cannot fix POI locations when RELEASE_CHUNK_DATA_TAG was set and POI_RECORDS was not set.");
            }
            CompoundTag sectionsTag = data.getCompoundTag("Sections");
            if (sectionsTag == null) {
                throw new IllegalArgumentException("Sections tag not found!");
            }
            for (NamedTag sectionTag : sectionsTag) {
                ListTag<CompoundTag> recordTags = ((CompoundTag) sectionTag.getTag()).getListTagAutoCast("Records");
                if (recordTags != null) {
                    for (CompoundTag recordTag : recordTags) {
                        IntArrayTag posTag = recordTag.getIntArrayTag("pos");
                        int[] pos = posTag.getValue();  // by ref
                        int x = pos[0];
                        int z = pos[2];
                        if (!cbr.containsBlock(x, z)) {
                            pos[0] = cbr.relocateX(x);
                            pos[2] = cbr.relocateZ(z);
                            changed = true;
                            // Don't need to call recordTag.getIntArrayTag("Pos").setValue(pos);
                        }
                    }
                }
            }
        }
        return changed;
    }

    /**
     * Called from {@link #initReferences(long)}. Exists to provide a hook for custom implementations to override to
     * add support for modded poi's, etc. without having to implement {@link #initReferences(long)} logic fully.
     */
    protected abstract T createPoiRecord(CompoundTag recordTag);

    @Override
    public boolean add(T record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        return records.add(record);
    }

    /**
     * Gets the first poi record found with the exact xyz given
     * @param x world block x
     * @param y world block y
     * @param z world block z
     * @return poi record if found, otherwise null
     */
    public T getFirst(final int x, final int y, final int z) {
        return records.stream().filter(r -> r.matches(x, y, z)).findFirst().orElse(null);
    }

    /**
     * Gets a shallow COPY of the set of poi records in this chunk.
     * Modifications to the list will have no affect on this chunk, but modifying items in that list will.
     * <p>However, you can {@link #getAll()} modify the returned list, then call {@link #set(Collection)}
     * with your modified list to update the records in this chunk.</p>
     */
    public List<T> getAll() {
        // don't return actual records list, retain the freedom to make it something other than a list for
        // optimizations later!
        return new ArrayList<>(records);
    }

    /**
     * Gets all poi record found with the exact xyz given. Really there should be only one - but nothing
     * is stopping you from messing it up.
     * @param x world block x
     * @param y world block y
     * @param z world block z
     * @return new list of poi records at the given xyz
     */
    public List<T> getAll(final int x, final int y, final int z) {
        return records.stream().filter(r -> r.matches(x, y, z)).collect(Collectors.toList());
    }

    /**
     * Gets all poi records of the given type
     * @param poiType poi type
     * @return new list of poi records matching the given poi type
     */
    public List<T> getAll(final String poiType) {
        List<T> list = records.stream().filter(r -> r.matches(poiType)).collect(Collectors.toList());
        return list;
    }

    /**
     * Removes the given record from ths poi chunk both by reference and by equality.
     * @param record record to remove
     * @return true if any record was removed
     */
    @Override
    public boolean remove(Object record) {
        if (!(record instanceof PoiRecord)) return false;
        return records.removeIf(r -> r == record || r.equals(record));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return records.removeAll(c);
    }

    /**
     * Removes all records at the given xyz.
     * @param x world block x
     * @param y world block y
     * @param z world block z
     * @return True if any records were removed
     */
    public boolean removeAll(final int x, final int y, final int z) {
        return records.removeIf(r -> r.matches(x, y, z));
    }

    /**
     * Removes all PoiRecords with the given type.
     * @param poiType poi type to remove
     * @return true if any records were removed
     */
    public boolean removeAll(final String poiType) {
        if (poiType == null || poiType.isEmpty()) {
            return false;
        }
        return records.removeIf(r -> r.matches(poiType));
    }

    /**
     * Removes the FIRST PoiRecord at the given xyz.
     * @param x world block x
     * @param y world block y
     * @param z world block z
     * @return Removed PoiRecord or null if no such record
     */
    public T removeFirst(final int x, final int y, final int z) {
        Iterator<T> iter = records.iterator();
        while (iter.hasNext()) {
            T record = iter.next();
            if (record.matches(x, y, z)) {
                iter.remove();
                return record;
            }
        }
        return null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return records.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T r : c) {
            if (r != null) {
                records.add(r);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return records.retainAll(c);
    }

    /**
     * Removes all poi record data from this chunk. This WILL NOT provide any signal to Minecraft that the
     * poi records for this chunk should be recalculated. Calling this function is only the correct action
     * if you have removed all poi blocks from the chunk or if you plan to rebuild the poi records.
     * <p>Also resets all poi chunk section validity flags to indicate "is valid = true".</p>
     */
    @Override
    public void clear() {
        records.clear();
        poiSectionValidity.clear();
    }

    @Override
    public int size() {
        return records.size();
    }

    @Override
    public boolean isEmpty() {
        return records.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return records.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return records.iterator();
    }

    /**
     * Provides an iterator over poi records with the given type. This is a convenience function and does not provide
     * any real optimization v.s. iterating over all elements.
     * @param poiType poi type, if null or empty an empty iterator is returned
     * @return Never null, but may be empty. Does not support {@link Iterator#remove()}
     */
    public Iterator<T> iterator(final String poiType) {
        if (poiType == null || poiType.isEmpty()) {
            return Collections.emptyIterator();
        }
        return records.stream().filter(r -> r.matches(poiType)).iterator();
    }

    public Stream<T> stream() {
        return records.stream();
    }

    @Override
    public Object[] toArray() {
        return records.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return records.toArray(a);
    }

    /**
     * Clears the poi records from this chunk by first calling {@link #clear()}, then repopulates them by
     * taking a shallow copy from the given collection. If the collection is null the affect of this
     * function is the same as {@link #clear()}.
     * @param c collection to shallow copy poi records from, any null entries will be ignored.
     */
    public void set(Collection<T> c) {
        clear();
        if (c != null) {
            addAll(c);
        }
    }

    /**
     * Marks the given subchunk invalid so that Minecraft will recompute POI for it when loaded.
     * @param sectionY subchunk section-y to invalidate
     */
    public void invalidateSection(int sectionY) {
        if (sectionY < Byte.MIN_VALUE || sectionY > Byte.MAX_VALUE)
            throw new IllegalArgumentException("sectionY must be in range [-128..127]");
        poiSectionValidity.put(sectionY, false);
    }

    /**
     * Checks if the given section has been marked invalid either by calling {@link #invalidateSection(int)} or if
     * it was already invalidated in the poi mca file.
     */
    public boolean isPoiSectionValid(int sectionY) {
        if (sectionY < Byte.MIN_VALUE || sectionY > Byte.MAX_VALUE)
            throw new IllegalArgumentException("sectionY must be in range [-128..127]");
        return poiSectionValidity.getOrDefault(sectionY, true);
    }

    /**
     * Checks if the given poi record resides in a section that has been marked invalid either by calling
     * {@link #invalidateSection(int)} or was already invalidated in the poi mca file.
     */
    public boolean isPoiSectionValid(PoiRecord record) {
        return record == null || poiSectionValidity.getOrDefault(record.getSectionY(), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompoundTag updateHandle() {
        if (raw) return data;
        super.updateHandle();
        Map<Integer, List<T>> sectionedLists = records.stream().collect(Collectors.groupingBy(PoiRecord::getSectionY));
        // ensure that all invalidated sections are in sectionedLists so we can just do one processing pass
        for (int sectionY : poiSectionValidity.keySet()) {
            if (!sectionedLists.containsKey(sectionY)) {
                sectionedLists.put(sectionY, Collections.emptyList());
            }
        }

        CompoundTag sectionContainerTag = new CompoundTag(sectionedLists.size());
        data.put("Sections", sectionContainerTag);
        for (Map.Entry<Integer, List<T>> entry : sectionedLists.entrySet()) {
            CompoundTag sectionTag = new CompoundTag();
            List<T> sectionRecords = entry.getValue();
            boolean isValid = poiSectionValidity.getOrDefault(entry.getKey(), true);
            if (!isValid || !sectionRecords.isEmpty()) {
                sectionContainerTag.put(Integer.toString(entry.getKey()), sectionTag);
                ListTag<CompoundTag> recordsTag = new ListTag<>(CompoundTag.class, sectionRecords.size());
                sectionTag.putBoolean("Valid", isValid);
                sectionTag.put("Records", recordsTag);
                for (PoiRecord record : sectionRecords) {
                    recordsTag.add(record.updateHandle());
                }
            }
        }
        return data;
    }
}
