package io.github.ensgijs.nbt.tag;

/**
 * NumberTag is an abstract representation of any {@link Number} tag.
 *
 * @param <T> The array type.
 */
public abstract class NumberTag<T extends Number & Comparable<T>> extends Tag<T> {

	public NumberTag(T value) {
		super(value);
	}

	public byte asByte() {
		return getValue().byteValue();
	}

	public short asShort() {
		return getValue().shortValue();
	}

	public int asInt() {
		return getValue().intValue();
	}

	public long asLong() {
		return getValue().longValue();
	}

	public float asFloat() {
		return getValue().floatValue();
	}

	public double asDouble() {
		return getValue().doubleValue();
	}

	/** {@inheritDoc} */
	@Override
	public String valueToString(int maxDepth) {
		return getValue().toString();
	}
}
