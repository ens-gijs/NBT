package io.github.ensgijs.nbt.tag;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.github.ensgijs.nbt.io.MaxDepthIO;

/**
 * ListTag represents a typed List in the nbt structure.
 * An empty {@link ListTag} will be of type {@link EndTag} (unknown type).
 * The type of an empty untyped {@link ListTag} can be set by using any of the {@code add()}
 * methods or any of the {@code as...List()} methods.
 */
public class ListTag<E extends Tag<?>> extends Tag<List<E>> implements List<E>, Comparable<ListTag<E>>, MaxDepthIO {

	public static final byte ID = 9;

	private Class<?> typeClass = null;

	private ListTag(int initialCapacity) {
		super(createEmptyValue(initialCapacity));
	}

	/**
	 * Creates a new ListTag that uses the given list.
	 * @param usingList List instance to use to back this ListTag. Values are NOT cloned.
	 * @throws NullPointerException if usingList is null or contains null elements.
	 */
	@SuppressWarnings("unchecked")
	public ListTag(List<E> usingList) {
		super(usingList);
		validateContainsNoNullsAndTypeOk(usingList);
		assignTypeClassIfNeeded(usingList);
	}

	protected Collection<? extends E> validateContainsNoNullsAndTypeOk(Collection<? extends E> c) {
		Objects.requireNonNull(c);
		if (c.isEmpty()) return c;
		final Class<?> tagType;
		if (this.typeClass == null || this.typeClass == EndTag.class)
			// If this throws an NPE - we don't allow null elements anyway!
			// This will throw a ClassCastException if the entry doesn't match the erasure.
			tagType = c.iterator().next().getClass();
		else
			tagType = this.typeClass;

		for (E e : c) {
			if (e == null)
				throw new NullPointerException();
			if (!tagType.isAssignableFrom(e.getClass()))
				throw new ClassCastException("list contained " + e.getClass().getName()
						+ " which is not a child type of " + tagType.getName());
		}
		return c;
	}

	protected Collection<? extends E> assignTypeClassIfNeeded(Collection<? extends E> c) {
		if (this.typeClass == null || this.typeClass == EndTag.class) {
			Class<?> tagType = !c.isEmpty() ? c.iterator().next().getClass() : EndTag.class;
			if (!Tag.class.isAssignableFrom(tagType))
				throw new IllegalArgumentException("Type must extend Tag");
			this.typeClass = tagType;
		}
		return c;
	}

	/** {@inheritDoc} */
	@Override
	public byte getID() {
		return ID;
	}

	/**
	 * <p>Creates a non-type-safe ListTag. Its element type will be set after the first
	 * element was added.</p>
	 *
	 * <p>This is an internal helper method for cases where the element type is not known
	 * at construction time. Use {@link #ListTag(Class)} when the type is known.</p>
	 *
	 * @return A new non-type-safe ListTag
	 */
	public static ListTag<?> createUnchecked(Class<?> typeClass) {
		return createUnchecked(typeClass, 3);
	}

	/**
	 * <p>Creates a non-type-safe ListTag. Its element type will be set after the first
	 * element was added.</p>
	 *
	 * <p>This is an internal helper method for cases where the element type is not known
	 * at construction time. Use {@link #ListTag(Class)} when the type is known.</p>
	 *
	 * @return A new non-type-safe ListTag
	 */
	public static ListTag<?> createUnchecked(Class<?> typeClass, int initialCapacity) {
		ListTag<?> list = new ListTag<>(initialCapacity);
		list.typeClass = typeClass;
		return list;
	}

	/**
	 * <p>Creates an empty mutable list to be used as empty value of ListTags.</p>
	 *
	 * @param <T>             Type of the list elements
	 * @param initialCapacity The initial capacity of the returned List
	 * @return An instance of {@link java.util.List} with an initial capacity of 3
	 */
	private static <T> List<T> createEmptyValue(int initialCapacity) {
		return new ArrayList<>(initialCapacity);
	}

	/**
	 * @param typeClass The exact class of the elements
	 * @throws IllegalArgumentException When {@code typeClass} is {@link EndTag}{@code .class}
	 * @throws NullPointerException     When {@code typeClass} is {@code null}
	 */
	public ListTag(Class<? super E> typeClass) throws IllegalArgumentException, NullPointerException {
		this(typeClass, 3);
	}

	/**
	 * @param typeClass       The exact class of the elements
	 * @param initialCapacity Initial capacity of list
	 * @throws IllegalArgumentException When {@code typeClass} is {@link EndTag}{@code .class}
	 * @throws NullPointerException     When {@code typeClass} is {@code null}
	 */
	public ListTag(Class<? super E> typeClass, int initialCapacity) throws IllegalArgumentException, NullPointerException {
		super(createEmptyValue(initialCapacity));
		if (typeClass == EndTag.class) {
			throw new IllegalArgumentException("cannot create ListTag with EndTag elements");
		}
		this.typeClass = Objects.requireNonNull(typeClass);
	}

	public Class<?> getTypeClass() {
		return typeClass == null ? EndTag.class : typeClass;
	}

	/** {@inheritDoc} */
	@Override
	public int size() {
		return getValue().size();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEmpty() {
		return getValue().isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public boolean contains(Object o) {
		return getValue().contains(o);
	}

	/** {@inheritDoc} */
	@Override
	public E remove(int index) {
		return getValue().remove(index);
	}

	/** {@inheritDoc} */
	@Override
	public int indexOf(Object o) {
		return getValue().indexOf(o);
	}

	/** {@inheritDoc} */
	@Override
	public int lastIndexOf(Object o) {
		return getValue().lastIndexOf(o);
	}

	/** {@inheritDoc} */
	@Override
	public ListIterator<E> listIterator() {
		return new NullRejectingListIterator<>(getValue().listIterator());
	}

	/** {@inheritDoc} */
	@Override
	public ListIterator<E> listIterator(int index) {
		return new NullRejectingListIterator<>(getValue().listIterator(index));
	}

	/** {@inheritDoc}
	 *
	 * @param fromIndex low endpoint (inclusive) of the subList
	 * @param toIndex high endpoint (exclusive) of the subList
	 * @return a view of the specified range within this list
	 * @throws IndexOutOfBoundsException for an illegal endpoint index value
	 *         ({@code fromIndex < 0 || toIndex > size ||
	 *         fromIndex > toIndex})
	 */
	@Override
	public ListTag<E> subList(int fromIndex, int toIndex) {
		return new ListTag<>(getValue().subList(fromIndex, toIndex));
	}

	/** {@inheritDoc} */
	@Override
	public void clear() {
		getValue().clear();
	}

	/** {@inheritDoc} */
	@Override
	public boolean containsAll(Collection<?> tags) {
		return getValue().containsAll(tags);
	}

	/** {@inheritDoc} */
	@Override
	public Iterator<E> iterator() {
		return listIterator();
	}

	/** {@inheritDoc} */
	@Override
	public Object[] toArray() {
		return getValue().toArray();
	}

	/** {@inheritDoc} */
	@Override
	public <T1> T1[] toArray(T1[] a) {
		return getValue().toArray(a);
	}

	/** {@inheritDoc} */
	@Override
	public Spliterator<E> spliterator() {
		return getValue().spliterator();
	}

	/** {@inheritDoc} */
	@Override
	public Stream<E> stream() {
		return getValue().stream();
	}

	/**
	 * <p>Value cannot be null.</p>
	 * {@inheritDoc}
	 */
	@Override
	public E set(int index, E element) {
		return getValue().set(index, Objects.requireNonNull(element));
	}

	/**
	 * <p>Value cannot be null.</p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(E element) {
		Objects.requireNonNull(element);
		if (getTypeClass() == EndTag.class) {
			typeClass = checkTypeClass(element.getClass());
		} else if (!typeClass.isAssignableFrom(element.getClass())) {
			throw new ClassCastException(
					String.format("cannot add %s to ListTag<%s>",
							element.getClass().getSimpleName(),
							typeClass.getSimpleName()));
		}
		return getValue().add(element);
	}

	/** {@inheritDoc} */
	@Override
	public boolean remove(Object o) {
		return getValue().remove(o);
	}

	/**
	 * <p>Value cannot be null.</p>
	 * {@inheritDoc}
	 */
	@Override
	public void add(int index, E element) {
		Objects.requireNonNull(element);
		if (getTypeClass() == EndTag.class) {
			typeClass = checkTypeClass(element.getClass());
		} else if (!typeClass.isAssignableFrom(element.getClass())) {
			throw new ClassCastException(
					String.format("cannot add %s to ListTag<%s>",
							element.getClass().getSimpleName(),
							typeClass.getSimpleName()));
		}
		getValue().add(index, element);
	}

	/**
	 * <p>Collection must not contain null elements.</p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends E> c) {
		return getValue().addAll(assignTypeClassIfNeeded(validateContainsNoNullsAndTypeOk(c)));
	}

	/**
	 * <p>Collection must not contain null elements.</p>
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return getValue().addAll(index, assignTypeClassIfNeeded(validateContainsNoNullsAndTypeOk(c)));
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeAll(Collection<?> c) {
		return getValue().removeAll(c);
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeIf(Predicate<? super E> filter) {
		return getValue().removeIf(filter);
	}

	/** {@inheritDoc} */
	@Override
	public boolean retainAll(Collection<?> c) {
		return getValue().retainAll(c);
	}

	/** {@inheritDoc} */
	@Override
	public void sort(Comparator<? super E> c) {
		getValue().sort(c);
	}

	public void addBoolean(boolean value) {
		addUnchecked(new ByteTag(value));
	}

	public void addByte(byte value) {
		addUnchecked(new ByteTag(value));
	}

	public void addShort(short value) {
		addUnchecked(new ShortTag(value));
	}

	public void addInt(int value) {
		addUnchecked(new IntTag(value));
	}

	public void addLong(long value) {
		addUnchecked(new LongTag(value));
	}

	public void addFloat(float value) {
		addUnchecked(new FloatTag(value));
	}

	public void addDouble(double value) {
		addUnchecked(new DoubleTag(value));
	}

	public void addString(String value) {
		addUnchecked(new StringTag(value));
	}

	public void addByteArray(byte[] value) {
		addUnchecked(new ByteArrayTag(value));
	}

	public void addIntArray(int[] value) {
		addUnchecked(new IntArrayTag(value));
	}

	public void addLongArray(long[] value) {
		addUnchecked(new LongArrayTag(value));
	}

	/** {@inheritDoc} */
	@Override
	public E get(int index) {
		return getValue().get(index);
	}

	@SuppressWarnings("unchecked")
	public <L extends Tag<?>> ListTag<L> asTypedList(Class<L> type) {
		checkTypeClass(type);
		return (ListTag<L>) this;
	}

	public ListTag<ByteTag> asByteTagList() {
		return asTypedList(ByteTag.class);
	}

	public ListTag<ShortTag> asShortTagList() {
		return asTypedList(ShortTag.class);
	}

	public ListTag<IntTag> asIntTagList() {
		return asTypedList(IntTag.class);
	}

	public ListTag<LongTag> asLongTagList() {
		return asTypedList(LongTag.class);
	}

	public ListTag<FloatTag> asFloatTagList() {
		return asTypedList(FloatTag.class);
	}

	public ListTag<DoubleTag> asDoubleTagList() {
		return asTypedList(DoubleTag.class);
	}

	public ListTag<StringTag> asStringTagList() {
		return asTypedList(StringTag.class);
	}

	public ListTag<ByteArrayTag> asByteArrayTagList() {
		return asTypedList(ByteArrayTag.class);
	}

	public ListTag<IntArrayTag> asIntArrayTagList() {
		return asTypedList(IntArrayTag.class);
	}

	public ListTag<LongArrayTag> asLongArrayTagList() {
		return asTypedList(LongArrayTag.class);
	}

	@SuppressWarnings("unchecked")
	public <T extends Tag<?>> ListTag<ListTag<T>> asListTagList() {
		checkTypeClass(ListTag.class);
		typeClass = ListTag.class;
		return (ListTag<ListTag<T>>) this;
	}

	public ListTag<CompoundTag> asCompoundTagList() {
		return asTypedList(CompoundTag.class);
	}

	public static ListTag<ByteTag> ofBytes(List<Byte> values) {
		var tag = new ListTag<>(ByteTag.class);
		for (var v : values) {
			tag.add(new ByteTag(v));
		}
		return tag;
	}

	public static ListTag<ShortTag> ofShorts(List<Short> values) {
		var tag = new ListTag<>(ShortTag.class);
		for (var v : values) {
			tag.add(new ShortTag(v));
		}
		return tag;
	}

	public static ListTag<IntTag> ofInts(List<Integer> values) {
		var tag = new ListTag<>(IntTag.class);
		for (var v : values) {
			tag.add(new IntTag(v));
		}
		return tag;
	}

	public static ListTag<LongTag> ofLongs(List<Long> values) {
		var tag = new ListTag<>(LongTag.class);
		for (var v : values) {
			tag.add(new LongTag(v));
		}
		return tag;
	}

	public static ListTag<FloatTag> ofFloats(List<Float> values) {
		var tag = new ListTag<>(FloatTag.class);
		for (var v : values) {
			tag.add(new FloatTag(v));
		}
		return tag;
	}

	public static ListTag<DoubleTag> ofDoubles(List<Double> values) {
		var tag = new ListTag<>(DoubleTag.class);
		for (var v : values) {
			tag.add(new DoubleTag(v));
		}
		return tag;
	}

	public static ListTag<StringTag> ofStrings(List<String> values) {
		var tag = new ListTag<>(StringTag.class);
		for (var v : values) {
			tag.add(new StringTag(v));
		}
		return tag;
	}

	/** {@inheritDoc} */
	@Override
	public String valueToString(int maxDepth) {
		StringBuilder sb = new StringBuilder("{\"type\":\"").append(getTypeClass().getSimpleName()).append("\",\"list\":[");
		for (int i = 0; i < size(); i++) {
			sb.append(i > 0 ? "," : "").append(get(i).valueToString(decrementMaxDepth(maxDepth)));
		}
		sb.append("]}");
		return sb.toString();
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!super.equals(other) || size() != ((ListTag<?>) other).size() || getTypeClass() != ((ListTag<?>) other)
				.getTypeClass()) {
			return false;
		}
		for (int i = 0; i < size(); i++) {
			if (!get(i).equals(((ListTag<?>) other).get(i))) {
				return false;
			}
		}
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hash(getTypeClass().hashCode(), getValue().hashCode());
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(ListTag<E> o) {
		int k = Integer.compare(this.size(), o.size());
		if (k != 0) return k;
		k = this.typeClass == o.typeClass ? 0 : this.typeClass.getName().compareTo(o.typeClass.getName());
		for (int i = 0, len = size(); k == 0 && i < len; i++) {
			k = Tag.compare(this.get(i), o.get(i));
		}
		return k;
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public ListTag<E> clone() {
		ListTag<E> copy = new ListTag<>(this.size());
		// assure type safety for clone
		copy.typeClass = typeClass;
		for (E e : getValue()) {
			copy.add((E) e.clone());
		}
		return copy;
	}

	//TODO: make private
	@SuppressWarnings("unchecked")
	public void addUnchecked(Tag<?> tag) {
		if (getTypeClass() != EndTag.class && typeClass != tag.getClass()) {
			throw new IllegalArgumentException(String.format(
					"cannot add %s to ListTag<%s>",
					tag.getClass().getSimpleName(), typeClass.getSimpleName()));
		}
		add((E) tag);
	}

	private Class<?> checkTypeClass(Class<?> clazz) {
		if (getTypeClass() != EndTag.class && typeClass != clazz) {
			throw new ClassCastException(String.format(
					"cannot cast ListTag<%s> to ListTag<%s>",
					typeClass.getSimpleName(), clazz.getSimpleName()));
		}
		return clazz;
	}

	private static class NullRejectingListIterator<E extends Tag<?>> implements ListIterator<E> {
		private final ListIterator<E> iter;
		public NullRejectingListIterator(ListIterator<E> iter) {
			this.iter = iter;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public E next() {
			return iter.next();
		}

		@Override
		public boolean hasPrevious() {
			return iter.hasPrevious();
		}

		@Override
		public E previous() {
			return iter.previous();
		}

		@Override
		public int nextIndex() {
			return iter.nextIndex();
		}

		@Override
		public int previousIndex() {
			return iter.previousIndex();
		}

		@Override
		public void remove() {
			iter.remove();
		}

		@Override
		public void set(E e) {
			iter.set(Objects.requireNonNull(e));
		}

		@Override
		public void add(E e) {
			iter.add(Objects.requireNonNull(e));
		}
	}
}
