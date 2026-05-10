package io.github.ensgijs.nbt.tag;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.ensgijs.nbt.io.MaxDepthIO;
import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.util.ArgValidator;

import static io.github.ensgijs.nbt.tag.StringTag.escapeString;

public class CompoundTag extends Tag<Map<String, Tag<?>>>
		implements Iterable<NamedTag>, Comparable<CompoundTag>, MaxDepthIO {

	public static final byte ID = 10;

	public CompoundTag() {
		super(createEmptyValue());
	}

	public CompoundTag(int initialCapacity) {
		super(new LinkedHashMap<>(initialCapacity));
	}

	/**
	 * @param data passed by ref
	 */
	protected CompoundTag(Map<String, Tag<?>> data) {
		super(data);
	}

	/** {@inheritDoc} */
	@Override
	public byte getID() {
		return ID;
	}

	private static Map<String, Tag<?>> createEmptyValue() {
		return new LinkedHashMap<>(8);
	}

	public int size() {
		return getValue().size();
	}

	public boolean isEmpty() {
		return getValue().isEmpty();
	}

	public Tag<?> remove(String key) {
		return getValue().remove(key);
	}

	public void clear() {
		getValue().clear();
	}

	public boolean containsKey(String key) {
		return getValue().containsKey(key);
	}

	/**
	 * Tests if the given key is present and if so if it is of the specified type.
	 * @param key name to require
	 * @param tagType expected value type, note this can be a super type of the actual value.
	 *                Ex. if the actual type is {@link ByteTag} and the passed tagType is {@link NumberTag},
	 *                this function will return true.
	 * @return true if the given key is found and the value is of the expected type.
	 */
	public boolean containsKey(String key, Class<?> tagType) {
		var v = getValue().get(key);
		return v != null && tagType.isAssignableFrom(v.getClass());
	}

	public boolean containsValue(Tag<?> value) {
		return getValue().containsValue(value);
	}

	public Collection<Tag<?>> values() {
		return getValue().values();
	}

	public Set<String> keySet() {
		return getValue().keySet();
	}

	public Set<Map.Entry<String, Tag<?>>> entrySet() {
		return getValue().entrySet();
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<NamedTag> iterator() {
		return new CompoundTagIterator(getValue().entrySet());
	}

	public Stream<NamedTag> stream() {
		return getValue().entrySet().stream().map(NamedTag::new);
	}

	public void forEach(BiConsumer<String, Tag<?>> action) {
		getValue().forEach(action);
	}

	public <C extends Tag<?>> C get(String key, Class<C> type) {
		Tag<?> t = getValue().get(key);
		if (t != null) {
			return type.cast(t);
		}
		return null;
	}

	public Tag<?> get(String key) {
		return getValue().get(key);
	}

	/**
	 * Gets a NamedTag that references this compound tag's values. Setting {@link NamedTag#setTag(Tag)}
	 * WILL NOT modify the contents of this {@link CompoundTag}, however, modifying the tag itself will.
	 * @return new {@link NamedTag} or null if the key does not exist.
	 */
	public NamedTag getNamedTag(String key) {
		Tag<?> tag = get(key);
		if (tag != null) {
			return new NamedTag(key, tag);
		}
		return null;
	}

	public NumberTag<?> getNumberTag(String key) {
		return (NumberTag<?>) getValue().get(key);
	}

	public Number getNumber(String key) {
		return getNumberTag(key).getValue();
	}

	public ByteTag getByteTag(String key) {
		return get(key, ByteTag.class);
	}

	public ShortTag getShortTag(String key) {
		return get(key, ShortTag.class);
	}

	public IntTag getIntTag(String key) {
		return get(key, IntTag.class);
	}

	public LongTag getLongTag(String key) {
		return get(key, LongTag.class);
	}

	public FloatTag getFloatTag(String key) {
		return get(key, FloatTag.class);
	}

	public DoubleTag getDoubleTag(String key) {
		return get(key, DoubleTag.class);
	}

	public StringTag getStringTag(String key) {
		return get(key, StringTag.class);
	}

	public ByteArrayTag getByteArrayTag(String key) {
		return get(key, ByteArrayTag.class);
	}

	public IntArrayTag getIntArrayTag(String key) {
		return get(key, IntArrayTag.class);
	}

	public LongArrayTag getLongArrayTag(String key) {
		return get(key, LongArrayTag.class);
	}

	/** @see #getCompoundList */
	public ListTag<?> getListTag(String key) {
		return get(key, ListTag.class);
	}

	/**
	 * @return ListTag&lt;&gt; of the type requested by the context in which this method was called.
	 * @throws ClassCastException may be thrown at call site if tag exists but cannot be cast to the necessary type.
	 * @see #getCompoundList
	 */
	@SuppressWarnings("unchecked")
	public <R extends ListTag<?>> R getListTagAutoCast(String key) {
		return (R) get(key, ListTag.class);
	}

	public CompoundTag getCompoundTag(String key) {
		return get(key, CompoundTag.class);
	}

	public CompoundTag getOrCreateCompoundTag(String key) {
		CompoundTag tag = get(key, CompoundTag.class);
		if (tag == null) {
			tag = new CompoundTag();
			put(key, tag);
		}
		return tag;
	}

	/**
	 * @return ListTag&lt;CompoundTag&gt; identified by key
	 * @throws ClassCastException may be thrown at call site if tag exists but is not a ListTag&lt;CompoundTag&gt;
	 */
	@SuppressWarnings("unchecked")
	public ListTag<CompoundTag> getCompoundList(String key) {
		return (ListTag<CompoundTag>) get(key, ListTag.class);
	}

	/** @return the value associated with key or false if there was none or if the tag was not of type {@link ByteTag}. */
	public boolean getBoolean(String key) {
		Tag<?> t = get(key);
		return t instanceof ByteTag && ((ByteTag) t).asByte() > 0;
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public boolean getBoolean(String key, boolean defaultValue) {
		Tag<?> t = get(key);
		return t instanceof ByteTag ? ((ByteTag) t).asByte() > 0 : defaultValue;
	}

	/** @return the value associated with key or {@link ByteTag#ZERO_VALUE} if there was none. */
	public byte getByte(String key) {
		ByteTag t = getByteTag(key);
		return t == null ? ByteTag.ZERO_VALUE : t.asByte();
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public byte getByte(String key, byte defaultValue) {
		ByteTag t = getByteTag(key);
		return t == null ? defaultValue : t.asByte();
	}

	/** @return the value associated with key or {@link ShortTag#ZERO_VALUE} if there was none. */
	public short getShort(String key) {
		ShortTag t = getShortTag(key);
		return t == null ? ShortTag.ZERO_VALUE : t.asShort();
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public short getShort(String key, short defaultValue) {
		ShortTag t = getShortTag(key);
		return t == null ? defaultValue: t.asShort();
	}

	/** @return the value associated with key or {@link IntTag#ZERO_VALUE} if there was none. */
	public int getInt(String key) {
		IntTag t = getIntTag(key);
		return t == null ? IntTag.ZERO_VALUE : t.asInt();
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public int getInt(String key, int defaultValue) {
		IntTag t = getIntTag(key);
		return t == null ? defaultValue : t.asInt();
	}

	/** @return the value associated with key or {@link LongTag#ZERO_VALUE} if there was none. */
	public long getLong(String key) {
		LongTag t = getLongTag(key);
		return t == null ? LongTag.ZERO_VALUE : t.asLong();
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public long getLong(String key, long defaultValue) {
		LongTag t = getLongTag(key);
		return t == null ? defaultValue : t.asLong();
	}

	/** @return the value associated with key or {@link FloatTag#ZERO_VALUE} if there was none. */
	public float getFloat(String key) {
		FloatTag t = getFloatTag(key);
		return t == null ? FloatTag.ZERO_VALUE : t.asFloat();
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public float getFloat(String key, float defaultValue) {
		FloatTag t = getFloatTag(key);
		return t == null ? defaultValue : t.asFloat();
	}

	/** @return the value associated with key or {@link DoubleTag#ZERO_VALUE} if there was none. */
	public double getDouble(String key) {
		DoubleTag t = getDoubleTag(key);
		return t == null ? DoubleTag.ZERO_VALUE : t.asDouble();
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public double getDouble(String key, double defaultValue) {
		DoubleTag t = getDoubleTag(key);
		return t == null ? defaultValue: t.asDouble();
	}

	/** @return the value associated with key or {@link StringTag#ZERO_VALUE} if there was none. */
	public String getString(String key) {
		StringTag t = getStringTag(key);
		return t == null ? StringTag.ZERO_VALUE : t.getValue();
	}

	/** @return the value associated with key or {@code defaultValue} if there was none. */
	public String getString(String key, String defaultValue) {
		StringTag t = getStringTag(key);
		return t == null ? defaultValue: t.getValue();
	}

	/** @return the value associated with key or {@link ByteArrayTag#ZERO_VALUE} if there was none. */
	public byte[] getByteArray(String key) {
		ByteArrayTag t = getByteArrayTag(key);
		return t == null ? ByteArrayTag.ZERO_VALUE : t.getValue();
	}

	/** @return the value associated with key or {@link IntArrayTag#ZERO_VALUE} if there was none. */
	public int[] getIntArray(String key) {
		IntArrayTag t = getIntArrayTag(key);
		return t == null ? IntArrayTag.ZERO_VALUE : t.getValue();
	}

	/** @return the value associated with key or {@link LongArrayTag#ZERO_VALUE} if there was none. */
	public long[] getLongArray(String key) {
		LongArrayTag t = getLongArrayTag(key);
		return t == null ? LongArrayTag.ZERO_VALUE : t.getValue();
	}

	/**
	 * Convenience function to get the values from a {@code ListTag<FloatTag>} as an array of floats.
	 * @param key name of the ListTag
	 * @return null if key does not exist; empty array if key exists but list was empty; array of values otherwise
	 */
	public float[] getFloatTagListAsArray(String key) {
		ListTag<FloatTag> t = getListTagAutoCast(key);
		if (t == null) return null;
		List<FloatTag> floatTagList = t.getValue();
		float[] floats = new float[floatTagList.size()];
		for (int i = 0; i < floats.length; i++) {
			floats[i] = floatTagList.get(i).asFloat();
		}
		return floats;
	}

	/**
	 * Convenience function to get the values from a {@code ListTag<DoubleTag>} as an array of doubles.
	 * @param key name of the ListTag
	 * @return null if key does not exist; empty array if key exists but list was empty; array of values otherwise
	 */
	public double[] getDoubleTagListAsArray(String key) {
		ListTag<DoubleTag> t = getListTagAutoCast(key);
		if (t == null) return null;
		List<DoubleTag> doubleTagList = t.getValue();
		double[] doubles = new double[doubleTagList.size()];
		for (int i = 0; i < doubles.length; i++) {
			doubles[i] = doubleTagList.get(i).asDouble();
		}
		return doubles;
	}

	/**
	 * Convenience function to get the values from a {@code ListTag<StringTag>} as a {@code List<String>}
	 * @see #putStringsAsTagList(String, List)
	 * @param key name of the ListTag
	 * @return null if key does not exist; empty list if key exists but list was empty; list of values otherwise
	 */
	public List<String> getStringTagListValues(String key) {
		ListTag<StringTag> t = getListTagAutoCast(key);
		if (t == null) return null;
		return t.getValue().stream()
				.map(StringTag::getValue)
				.collect(Collectors.toList());
	}

	/** @return the previous value associated with namedTag.getName() or null if there was none. */
	public Tag<?> put(NamedTag namedTag) {
		return put(namedTag.getName(), namedTag.getTag());
	}

	 /** @return the previous value associated with key or null if there was none. */
	public Tag<?> put(String key, Tag<?> tag) {
		return getValue().put(Objects.requireNonNull(key), Objects.requireNonNull(tag));
	}

	/**
	 * Puts all of the provided {@link NamedTag}'s.
	 */
	public void putAll(List<NamedTag> namedTags) {
		for (NamedTag namedTag : namedTags) {
			put(namedTag.getName(), namedTag.getTag());
		}
	}

	/**
	 * Puts the raw value by wrapping it in an appropriate Tag if necessary.
	 * @param key Non-null key.
	 * @param value Any value, passing null will call {@link #remove(String)}.
	 * @return previous value associated with key, or null.
	 * @throws IllegalArgumentException if the type of value is not supported.
	 */
	public Tag<?> putValue(String key, Object value) {
		ArgValidator.requireValue(key, "key");
		if (value == null) {
			return remove(key);
		}
		return put(key, Tag.asTag(value));
	}

	/**
	 * Puts the tag iff {@code namedTag != null && namedTag.getTag() != null}
	 * @return the previous value associated with namedTag.getName() or null if there was none.
	 */
	public Tag<?> putIfNotNull(NamedTag namedTag) {
		if (namedTag == null) return null;
		return putIfNotNull(namedTag.getName(), namedTag.getTag());
	}

	/**
	 * Puts the tag iff {tag != null}
	 * @return the previous value associated with key or null if there was none.
	 */
	public Tag<?> putIfNotNull(String key, Tag<?> tag) {
		if (tag == null) {
			return null;
		}
		return put(key, tag);
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putBoolean(String key, boolean value) {
		return put(key, new ByteTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putByte(String key, byte value) {
		return put(key, new ByteTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putShort(String key, short value) {
		return put(key, new ShortTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putInt(String key, int value) {
		return put(key, new IntTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putLong(String key, long value) {
		return put(key, new LongTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putFloat(String key, float value) {
		return put(key, new FloatTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putDouble(String key, double value) {
		return put(key, new DoubleTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putString(String key, String value) {
		return put(key, new StringTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putByteArray(String key, byte[] value) {
		return put(key, new ByteArrayTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putIntArray(String key, int[] value) {
		return put(key, new IntArrayTag(value));
	}

	/** @return the previous value associated with key or null if there was none. */
	public Tag<?> putLongArray(String key, long[] value) {
		return put(key, new LongArrayTag(value));
	}

	/**
	 * Convenience function to set a ListTag&lt;FloatTag&gt; from an array. If values is null then
	 * the specified key is REMOVED. Provide an empty array to indicate that an empty ListTag is desired.
	 * @param key name of ListTag
	 * @param values values to set (may be one or more floats, or a float[])
	 * @return new ListTag, or null if values was null
	 */
	public Tag<?> putFloatArrayAsTagList(String key, float... values) {
		if (values == null) {
			remove(key);
			return null;
		}
		ListTag<FloatTag> listTag = new ListTag<>(FloatTag.class, values.length);
		for (float v : values) {
			listTag.addFloat(v);
		}
		return put(key, listTag);
	}

	/**
	 * Convenience function to set a ListTag&lt;DoubleTag&gt; from an array. If values is null then
	 * the specified key is REMOVED. Provide an empty array to indicate that an empty ListTag is desired.
	 * @param key name of ListTag
	 * @param values values to set (may be one or more doubles, or a double[])
	 * @return new ListTag, or null if values was null
	 */
	public Tag<?> putDoubleArrayAsTagList(String key, double... values) {
		if (values == null) {
			remove(key);
			return null;
		}
		ListTag<DoubleTag> listTag = new ListTag<>(DoubleTag.class, values.length);
		for (double v : values) {
			listTag.addDouble(v);
		}
		return put(key, listTag);
	}

	/**
	 * Convenience function to set a {@code ListTag<StringTag>} from a {@code List<String>}. If values is null then
	 * the specified key is REMOVED. Provide an empty List to indicate that an empty ListTag is desired.
	 * @see #getStringTagListValues(String)
	 * @see Arrays#asList(Object[])
	 * @param key name of ListTag
	 * @param values values to set
	 * @return new ListTag, or null if values was null
	 */
	public Tag<?> putStringsAsTagList(String key, List<String> values) {
		if (values == null) {
			remove(key);
			return null;
		}
		ListTag<StringTag> listTag = new ListTag<>(StringTag.class, values.size());
		for (String v : values) {
			listTag.addString(v);
		}
		return put(key, listTag);
	}

	/** {@inheritDoc} */
	@Override
	public String valueToString(int maxDepth) {
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		Iterator<Map.Entry<String, Tag<?>>> iter = getValue().entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.iterator();
		while (iter.hasNext()) {
			Map.Entry<String, Tag<?>> e = iter.next();
			sb.append(first ? "" : ",")
					.append(escapeString(e.getKey(), false)).append(":")
					.append(e.getValue().toString(decrementMaxDepth(maxDepth)));
			first = false;
		}
		sb.append("}");
		return sb.toString();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other) || size() != ((CompoundTag) other).size()) {
			return false;
		}
		for (Map.Entry<String, Tag<?>> e : getValue().entrySet()) {
			Tag<?> v;
			if ((v = ((CompoundTag) other).get(e.getKey())) == null || !e.getValue().equals(v)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Compares this compound tag to another one.
	 * <p>Comparison sequence:</p>
	 * <ul>
	 *     <li>key set length</li>
	 *     <li>key names</li>
	 *     <li>tag value compare result (tag type before value comparison)</li>
	 * </ul>
	 */
	@Override
	public int compareTo(CompoundTag o) {
		final var thisMap = getValue();
		final var otherMap = o.getValue();
		int k = Integer.compare(size(), otherMap.size());
		if (k != 0) return k;
		if (!thisMap.keySet().containsAll(otherMap.keySet())) {
			ArrayList<String> keys1 = new ArrayList<>(thisMap.keySet());
			ArrayList<String> keys2 = new ArrayList<>(otherMap.keySet());
			keys1.sort(String::compareTo);
			keys2.sort(String::compareTo);
			k = Arrays.compare(keys1.toArray(new String[0]), keys2.toArray(new String[0]));
			if (k != 0) return k;
			for (String key: keys1) {
				k = Tag.compare(thisMap.get(key), otherMap.get(key));
				if (k != 0)
					return k;
			}
		}
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public CompoundTag clone() {
		// Choose initial capacity based on default load factor (0.75) so all entries fit in map without resizing
		CompoundTag copy = new CompoundTag((int) Math.ceil(getValue().size() / 0.75f));
		for (Map.Entry<String, Tag<?>> e : getValue().entrySet()) {
			copy.put(e.getKey(), e.getValue().clone());
		}
		return copy;
	}

    private static class CompoundTagIterator implements Iterator<NamedTag> {
		private final Iterator<Map.Entry<String, Tag<?>>> iterator;

		CompoundTagIterator(Set<Map.Entry<String, Tag<?>>> set) {
			this.iterator = set.iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public NamedTag next() {
			return new MappedNamedTag(iterator.next());
		}

		@Override
		public void remove() {
			iterator.remove();
		}
	}

	private static class MappedNamedTag extends NamedTag {
		private final Map.Entry<String, Tag<?>> entry;
		public MappedNamedTag(Map.Entry<String, Tag<?>> entry) {
			this.entry = entry;
		}

		public void setName(String name) {
			throw new UnsupportedOperationException();
		}

		public void setTag(Tag<?> tag) {
			ArgValidator.requireValue(tag, "tag");
			entry.setValue(tag);
		}

		public String getName() {
			return entry.getKey();
		}

		public Tag<?> getTag() {
			return entry.getValue();
		}
	}
}
