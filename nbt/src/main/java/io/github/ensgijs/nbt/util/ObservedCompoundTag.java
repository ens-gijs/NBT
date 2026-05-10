package io.github.ensgijs.nbt.util;

import io.github.ensgijs.nbt.tag.*;
import io.github.ensgijs.nbt.io.NamedTag;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Decorates a compound tag to track what keys have been accessed and offers some utility functions like
 * {@link #readKeys()}, {@link #unreadKeys()} and {@link #unreadTagsByRef()}.
 */
public class ObservedCompoundTag extends CompoundTag {
    private final CompoundTag wrappedTag;
    protected final Set<String> readKeys = new HashSet<>();

    public ObservedCompoundTag(CompoundTag wrappedTag) {
        // emptyMap saves byte overhead - we never read/write it anyway
        super(Collections.emptyMap());
        this.wrappedTag = wrappedTag;
    }

    public CompoundTag wrappedTag() {
        return wrappedTag;
    }

    /**
     *  @return unmodifiable set of keys which one of the GET methods has been called with.
     *  It doesn't matter if the CompoundTag contained the key or not - if get was called with a key it's in this set.
     */
    public Set<String> readKeys() {
        return Collections.unmodifiableSet(readKeys);
    }

    /**
     *  @return unmodifiable set of keys which one of the GET methods has NOT been called with.
     */
    public Set<String> unreadKeys() {
        return Collections.unmodifiableSet(wrappedTag.keySet().stream()
                .filter(k -> !readKeys.contains(k))
                .collect(Collectors.toSet()));
    }

    /**
     * @return A new CompoundTag that contains a reference to all the unread keys/entries.
     */
    public CompoundTag unreadTagsByRef() {
        CompoundTag unread = new CompoundTag(wrappedTag.size());
        wrappedTag.forEach((k, v) -> {
            if (!readKeys.contains(k)) {
                unread.put(k, v);
            }
        });
        return unread;
    }

    /** {@inheritDoc} */
    @Override
    public byte getID() {
        return wrappedTag.getID();
    }

    /** {@inheritDoc} */
    public int size() {
        return wrappedTag.size();
    }

    /** {@inheritDoc} */
    public boolean isEmpty() {
        return wrappedTag.isEmpty();
    }

    /** {@inheritDoc} */
    public Tag<?> remove(String key) {
        return wrappedTag.remove(key);
    }

    /** {@inheritDoc} */
    public void clear() {
        wrappedTag.clear();
    }

    /** {@inheritDoc} */
    public boolean containsKey(String key) {
        return wrappedTag.containsKey(key);
    }

    /** {@inheritDoc} */
    public boolean containsValue(Tag<?> value) {
        return wrappedTag.containsValue(value);
    }

    /** {@inheritDoc} */
    public Collection<Tag<?>> values() {
        return wrappedTag.values();
    }

    /** {@inheritDoc} */
    public Set<String> keySet() {
        return wrappedTag.keySet();
    }

    /** {@inheritDoc}
     * @return*/
    @Override
    public Iterator<NamedTag> iterator() {
        return wrappedTag.iterator();
    }

    /** {@inheritDoc}
     * @return*/
    @Override
    public Spliterator<NamedTag> spliterator() {
        return wrappedTag.spliterator();
    }

    /** {@inheritDoc}
     * @return*/
    public Stream<NamedTag> stream() {
        return wrappedTag.stream();
    }

    /** {@inheritDoc} */
    public void forEach(BiConsumer<String, Tag<?>> action) {
        wrappedTag.forEach(action);
    }

    /** {@inheritDoc} */
    public <C extends Tag<?>> C get(String key, Class<C> type) {
        readKeys.add(key);
        return wrappedTag.get(key, type);
    }

    /** {@inheritDoc} */
    public Tag<?> get(String key) {
        readKeys.add(key);
        return wrappedTag.get(key);
    }

    @Override
    public NamedTag getNamedTag(String key) {
        readKeys.add(key);
        return super.getNamedTag(key);
    }

    /** {@inheritDoc} */
    public NumberTag<?> getNumberTag(String key) {
        readKeys.add(key);
        return wrappedTag.getNumberTag(key);
    }

    /** {@inheritDoc} */
    public Number getNumber(String key) {
        readKeys.add(key);
        return wrappedTag.getNumber(key);

    }

    /** {@inheritDoc} */
    public ByteTag getByteTag(String key) {
        readKeys.add(key);
        return wrappedTag.getByteTag(key);
    }

    /** {@inheritDoc} */
    public ShortTag getShortTag(String key) {
        readKeys.add(key);
        return wrappedTag.getShortTag(key);
    }

    /** {@inheritDoc} */
    public IntTag getIntTag(String key) {
        readKeys.add(key);
        return wrappedTag.getIntTag(key);
    }

    /** {@inheritDoc} */
    public LongTag getLongTag(String key) {
        readKeys.add(key);
        return wrappedTag.getLongTag(key);
    }

    /** {@inheritDoc} */
    public FloatTag getFloatTag(String key) {
        readKeys.add(key);
        return wrappedTag.getFloatTag(key);
    }

    /** {@inheritDoc} */
    public DoubleTag getDoubleTag(String key) {
        readKeys.add(key);
        return wrappedTag.getDoubleTag(key);
    }

    /** {@inheritDoc} */
    public StringTag getStringTag(String key) {
        readKeys.add(key);
        return wrappedTag.getStringTag(key);
    }

    /** {@inheritDoc} */
    public ByteArrayTag getByteArrayTag(String key) {
        readKeys.add(key);
        return wrappedTag.getByteArrayTag(key);
    }

    /** {@inheritDoc} */
    public IntArrayTag getIntArrayTag(String key) {
        readKeys.add(key);
        return wrappedTag.getIntArrayTag(key);
    }

    /** {@inheritDoc} */
    public LongArrayTag getLongArrayTag(String key) {
        readKeys.add(key);
        return wrappedTag.getLongArrayTag(key);
    }

    /** {@inheritDoc} */
    public ListTag<?> getListTag(String key) {
        readKeys.add(key);
        return wrappedTag.getListTag(key);
    }

    /** {@inheritDoc} */
    public <R extends ListTag<?>> R getListTagAutoCast(String key) {
        readKeys.add(key);
        return wrappedTag.getListTagAutoCast(key);
    }

    /** {@inheritDoc} */
    public CompoundTag getCompoundTag(String key) {
        readKeys.add(key);
        return wrappedTag.getCompoundTag(key);
    }


    public CompoundTag getOrCreateCompoundTag(String key) {
        readKeys.add(key);
        return wrappedTag.getOrCreateCompoundTag(key);
    }

    @Override
    public ListTag<CompoundTag> getCompoundList(String key) {
        readKeys.add(key);
        return super.getCompoundList(key);
    }

    /** {@inheritDoc} */
    public boolean getBoolean(String key) {
        readKeys.add(key);
        return wrappedTag.getBoolean(key);
    }

    /** {@inheritDoc} */
    public boolean getBoolean(String key, boolean defaultValue) {
        readKeys.add(key);
        return wrappedTag.getBoolean(key, defaultValue);
    }

    /** {@inheritDoc} */
    public byte getByte(String key) {
        readKeys.add(key);
        return wrappedTag.getByte(key);
    }

    /** {@inheritDoc} */
    public byte getByte(String key, byte defaultValue) {
        readKeys.add(key);
        return wrappedTag.getByte(key, defaultValue);
    }

    /** {@inheritDoc} */
    public short getShort(String key) {
        readKeys.add(key);
        return wrappedTag.getShort(key);
    }

    /** {@inheritDoc} */
    public short getShort(String key, short defaultValue) {
        readKeys.add(key);
        return wrappedTag.getShort(key, defaultValue);
    }

    /** {@inheritDoc} */
    public int getInt(String key) {
        readKeys.add(key);
        return wrappedTag.getInt(key);
    }

    /** {@inheritDoc} */
    public int getInt(String key, int defaultValue) {
        readKeys.add(key);
        return wrappedTag.getInt(key, defaultValue);
    }

    /** {@inheritDoc} */
    public long getLong(String key) {
        readKeys.add(key);
        return wrappedTag.getLong(key);
    }

    /** {@inheritDoc} */
    public long getLong(String key, long defaultValue) {
        readKeys.add(key);
        return wrappedTag.getLong(key, defaultValue);
    }

    /** {@inheritDoc} */
    public float getFloat(String key) {
        readKeys.add(key);
        return wrappedTag.getFloat(key);
    }

    /** {@inheritDoc} */
    public float getFloat(String key, float defaultValue) {
        readKeys.add(key);
        return wrappedTag.getFloat(key, defaultValue);
    }

    /** {@inheritDoc} */
    public double getDouble(String key) {
        readKeys.add(key);
        return wrappedTag.getDouble(key);
    }

    /** {@inheritDoc} */
    public double getDouble(String key, double defaultValue) {
        readKeys.add(key);
        return wrappedTag.getDouble(key, defaultValue);
    }

    /** {@inheritDoc} */
    public String getString(String key) {
        readKeys.add(key);
        return wrappedTag.getString(key);
    }

    /** {@inheritDoc} */
    public String getString(String key, String defaultValue) {
        readKeys.add(key);
        return wrappedTag.getString(key, defaultValue);
    }

    /** {@inheritDoc} */
    public byte[] getByteArray(String key) {
        readKeys.add(key);
        return wrappedTag.getByteArray(key);
    }

    /** {@inheritDoc} */
    public int[] getIntArray(String key) {
        readKeys.add(key);
        return wrappedTag.getIntArray(key);
    }

    /** {@inheritDoc} */
    public long[] getLongArray(String key) {
        readKeys.add(key);
        return wrappedTag.getLongArray(key);
    }

    /** {@inheritDoc} */
    public float[] getFloatTagListAsArray(String key) {
        readKeys.add(key);
        return wrappedTag.getFloatTagListAsArray(key);
    }

    /** {@inheritDoc} */
    public double[] getDoubleTagListAsArray(String key) {
        readKeys.add(key);
        return wrappedTag.getDoubleTagListAsArray(key);
    }

    /** {@inheritDoc} */
    public List<String> getStringTagListValues(String key) {
        readKeys.add(key);
        return wrappedTag.getStringTagListValues(key);
    }

    @Override
    public Tag<?> put(NamedTag namedTag) {
        return super.put(namedTag);
    }

    /** {@inheritDoc} */
    public Tag<?> put(String key, Tag<?> tag) {
        return wrappedTag.put(key, tag);
    }

    @Override
    public Tag<?> putIfNotNull(NamedTag namedTag) {
        return super.putIfNotNull(namedTag);
    }

    /** {@inheritDoc} */
    public Tag<?> putIfNotNull(String key, Tag<?> tag) {
        return wrappedTag.putIfNotNull(key, tag);
    }

    /** {@inheritDoc} */
    public Tag<?> putBoolean(String key, boolean value) {
        return wrappedTag.putBoolean(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putByte(String key, byte value) {
        return wrappedTag.putByte(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putShort(String key, short value) {
        return wrappedTag.putShort(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putInt(String key, int value) {
        return wrappedTag.putInt(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putLong(String key, long value) {
        return wrappedTag.putLong(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putFloat(String key, float value) {
        return wrappedTag.putFloat(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putDouble(String key, double value) {
        return wrappedTag.putDouble(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putString(String key, String value) {
        return wrappedTag.putString(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putByteArray(String key, byte[] value) {
        return wrappedTag.putByteArray(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putIntArray(String key, int[] value) {
        return wrappedTag.putIntArray(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putLongArray(String key, long[] value) {
        return wrappedTag.putLongArray(key, value);
    }

    /** {@inheritDoc} */
    public Tag<?> putFloatArrayAsTagList(String key, float... values) {
        return wrappedTag.putFloatArrayAsTagList(key, values);
    }

    /** {@inheritDoc} */
    public Tag<?> putDoubleArrayAsTagList(String key, double... values) {
        return wrappedTag.putDoubleArrayAsTagList(key, values);
    }

    /** {@inheritDoc} */
    public Tag<?> putStringsAsTagList(String key, List<String> values) {
        return wrappedTag.putStringsAsTagList(key, values);
    }

    /** {@inheritDoc} */
    @Override
    public String valueToString(int maxDepth) {
        return wrappedTag.valueToString(maxDepth);

    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        return wrappedTag.equals(other);
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(CompoundTag o) {
        return wrappedTag.compareTo(o);
    }

    /** {@inheritDoc} */
    @Override
    public CompoundTag clone() {
        return wrappedTag.clone();
    }
}
