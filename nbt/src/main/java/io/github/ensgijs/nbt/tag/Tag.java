package io.github.ensgijs.nbt.tag;

import io.github.ensgijs.nbt.io.MaxDepthReachedException;
import io.github.ensgijs.nbt.io.NamedTag;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for all NBT tags.
 * 
 * <h2>Nesting</h2>
 * <p>All methods serializing instances or deserializing data track the nesting levels to prevent 
 * circular references or malicious data which could, when deserialized, result in thousands 
 * of instances causing a denial of service.</p>
 * 
 * <p>These methods have a parameter for the maximum nesting depth they are allowed to traverse. A 
 * value of {@code 0} means that only the object itself, but no nested objects may be processed. 
 * If an instance is nested further than allowed, a {@link MaxDepthReachedException} will be thrown.
 * Providing a negative maximum nesting depth will cause an {@code IllegalArgumentException} 
 * to be thrown.</p>
 * 
 * <p>Some methods do not provide a parameter to specify the maximum nesting depth, but instead use 
 * {@link #DEFAULT_MAX_DEPTH}, which is also the maximum used by Minecraft. This is documented for 
 * the respective methods.</p>
 * 
 * <p>If custom NBT tags contain objects other than NBT tags, which can be nested as well, then there 
 * is no guarantee that {@code MaxDepthReachedException}s are thrown for them. The respective class 
 * will document this behavior accordingly.</p>
 * 
 * @param <T> The type of the contained value
 * */
public abstract class Tag<T> implements Cloneable {

	/**
	 * The default maximum depth of the NBT structure.
	 * */
	public static final int DEFAULT_MAX_DEPTH = 512;

	private T value;

	/**
	 * Initializes this Tag with some value. If the value is {@code null}, it will
	 * throw a {@code NullPointerException}
	 * @param value The value to be set for this Tag.
	 */
	public Tag(T value) {
		setValue(value);
	}

	/**
	 * This Tag's ID, usually used for serialization and deserialization.
	 */
	public abstract byte getID();

	/**
	 * The value of this Tag.
	 */
	protected T getValue() {
		return value;
	}

	/**
	 * Sets the value for this Tag directly.
	 * @param value The value to be set.
	 * @throws NullPointerException If the value is null
	 */
	protected void setValue(T value) {
		this.value = checkValue(value);
	}

	/**
	 * Checks if the value {@code value} is {@code null}.
	 * @param value The value to check
	 * @throws NullPointerException If {@code value} was {@code null}
	 * @return The parameter {@code value}
	 */
	protected T checkValue(T value) {
		return Objects.requireNonNull(value);
	}

	/**
	 * Calls {@link Tag#toString(int)} with an initial depth of {@code 0}.
	 * @see Tag#toString(int)
	 * @throws MaxDepthReachedException If the maximum nesting depth is exceeded.
	 */
	@Override
	public final String toString() {
		return toString(DEFAULT_MAX_DEPTH);
	}

	/**
	 * Creates a string representation of this Tag in a valid JSON format.
	 * @param maxDepth The maximum nesting depth.
	 * @return The string representation of this Tag.
	 * @throws MaxDepthReachedException If the maximum nesting depth is exceeded.
	 */
	public String toString(int maxDepth) {
		return "{\"type\":\""+ getClass().getSimpleName() + "\"," +
				"\"value\":" + valueToString(maxDepth) + "}";
	}

	/**
	 * Calls {@link Tag#valueToString(int)} with {@link Tag#DEFAULT_MAX_DEPTH}.
	 * @return The string representation of the value of this Tag.
	 * @throws MaxDepthReachedException If the maximum nesting depth is exceeded.
	 */
	public final String valueToString() {
		return valueToString(DEFAULT_MAX_DEPTH);
	}

	/**
	 * Returns a JSON representation of the value of this Tag.
	 * @param maxDepth The maximum nesting depth.
	 * @return The string representation of the value of this Tag.
	 * @throws MaxDepthReachedException If the maximum nesting depth is exceeded.
	 */
	public abstract String valueToString(int maxDepth);

	/**
	 * Returns whether this Tag and some other Tag are equal.
	 * They are equal if {@code other} is not {@code null} and they are of the same class.
	 * Custom Tag implementations should overwrite this but check the result
	 * of this {@code super}-method while comparing.
	 * @param other The Tag to compare to.
	 * @return {@code true} if they are equal based on the conditions mentioned above.
	 */
	@Override
	public boolean equals(Object other) {
		return other != null && getClass() == other.getClass();
	}

	/**
	 * Calculates the hash code of this Tag. Tags which are equal according to {@link Tag#equals(Object)}
	 * must return an equal hash code.
	 * @return The hash code of this Tag.
	 */
	@Override
	public int hashCode() {
		return value.hashCode();
	}

	/**
	 * Creates a clone of this Tag.
	 * @return A clone of this Tag.
	 * */
	@SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
	public abstract Tag<T> clone();

	@SuppressWarnings("unchecked")
	public static int compare(Tag<?> tag1, Tag<?> tag2) {
		if (tag1 == null && tag2 == null) return 0;
		if (tag1 == null) return -1;
		if (tag2 == null) return 1;
		int k = Integer.compare(tag1.getID(), tag2.getID());
		if (k != 0) return k;
		return switch (tag1.getID()) {
			case ByteTag.ID -> ((ByteTag) tag1).compareTo((ByteTag) tag2);
			case ShortTag.ID -> ((ShortTag) tag1).compareTo((ShortTag) tag2);
			case IntTag.ID -> ((IntTag) tag1).compareTo((IntTag) tag2);
			case LongTag.ID -> ((LongTag) tag1).compareTo((LongTag) tag2);
			case FloatTag.ID -> ((FloatTag) tag1).compareTo((FloatTag) tag2);
			case DoubleTag.ID -> ((DoubleTag) tag1).compareTo((DoubleTag) tag2);
			case ByteArrayTag.ID -> ((ByteArrayTag) tag1).compareTo((ByteArrayTag) tag2);
			case StringTag.ID -> ((StringTag) tag1).compareTo((StringTag) tag2);
			case ListTag.ID -> ((ListTag) tag1).compareTo((ListTag) tag2);
			case CompoundTag.ID -> ((CompoundTag) tag1).compareTo((CompoundTag) tag2);
			case IntArrayTag.ID -> ((IntArrayTag) tag1).compareTo((IntArrayTag) tag2);
			case LongArrayTag.ID -> ((LongArrayTag) tag1).compareTo((LongArrayTag) tag2);
			default -> throw new UnsupportedOperationException("New tag type? ID: " + tag1.getID());
		};
	}

	public static Tag<?> asTag(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Tag<?> v) {
			return v;
		}
		if (value instanceof NamedTag v) {
			return v.getTag();
		}
		if (value instanceof String v) {
			return new StringTag(v);
		}
		if (value instanceof Boolean v) {
			return new ByteTag(v);
		}
		if (value instanceof Number) {
			if (value instanceof Byte v) {
				return new ByteTag(v);
			}
			if (value instanceof Short v) {
				return new ShortTag(v);
			}
			if (value instanceof Integer v) {
				return new IntTag(v);
			}
			if (value instanceof Long v) {
				return new LongTag(v);
			}
			if (value instanceof Float v) {
				return new FloatTag(v);
			}
			if (value instanceof Double v) {
				return new DoubleTag(v);
			}
		}
		if (value instanceof byte[] v) {
			return new ByteArrayTag(v);
		}
		if (value instanceof int[] v) {
			return new IntArrayTag(v);
		}
		if (value instanceof long[] v) {
			return new LongArrayTag(v);
		}
		if (value instanceof float[] values) {
			ListTag<FloatTag> listTag = new ListTag<>(FloatTag.class, values.length);
			for (float v : values) {
				listTag.addFloat(v);
			}
			return listTag;
		}
		if (value instanceof double[] values) {
			ListTag<DoubleTag> listTag = new ListTag<>(DoubleTag.class, values.length);
			for (double v : values) {
				listTag.addDouble(v);
			}
			return listTag;
		}
		if (value instanceof String[] values) {
			ListTag<StringTag> listTag = new ListTag<>(StringTag.class, values.length);
			for (String v : values) {
				listTag.addString(v);
			}
			return listTag;
		}
		if (value instanceof Map<?, ?> m) {
			CompoundTag tag = new CompoundTag(m.size());
			for (var entry : m.entrySet()) {
				tag.putValue((String) entry.getKey(), entry.getValue());
			}
			return tag;
		}
		if (value instanceof Collection<?> c) {
			ListTag listTag = ListTag.createUnchecked(EndTag.class);
			for (Object v : c) {
				var t = asTag(v);
				listTag.add(t);
			}
			return listTag;
		}
		throw new IllegalArgumentException("Could not determine Tag type of " + value.getClass().getSimpleName());
	}
}
