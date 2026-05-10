package io.github.ensgijs.nbt.tag;

public class IntTag extends NumberTag<Integer> implements Comparable<IntTag> {

	public static final byte ID = 3;
	public static final int ZERO_VALUE = 0;

	public IntTag() {
		super(ZERO_VALUE);
	}

	public IntTag(int value) {
		super(value);
	}

	/** {@inheritDoc} */
	@Override
	public byte getID() {
		return ID;
	}

	/**
	 * Sets the value for this Tag directly.
	 * @param value The value to be set.
	 */
	public void setValue(int value) {
		super.setValue(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		return super.equals(other) && asInt() == ((IntTag) other).asInt();
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(IntTag other) {
		return getValue().compareTo(other.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public IntTag clone() {
		return new IntTag(getValue());
	}
}
