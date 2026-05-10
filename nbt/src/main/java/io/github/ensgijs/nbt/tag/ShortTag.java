package io.github.ensgijs.nbt.tag;

public class ShortTag extends NumberTag<Short> implements Comparable<ShortTag> {

	public static final byte ID = 2;
	public static final short ZERO_VALUE = 0;

	public ShortTag() {
		super(ZERO_VALUE);
	}

	public ShortTag(short value) {
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
	public void setValue(short value) {
		super.setValue(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		return super.equals(other) && asShort() == ((ShortTag) other).asShort();
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(ShortTag other) {
		return getValue().compareTo(other.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public ShortTag clone() {
		return new ShortTag(getValue());
	}
}
