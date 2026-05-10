package io.github.ensgijs.nbt.tag;

public class ByteTag extends NumberTag<Byte> implements Comparable<ByteTag> {

	public static final byte ID = 1;
	public static final byte ZERO_VALUE = 0;

	public ByteTag() {
		super(ZERO_VALUE);
	}

	public ByteTag(byte value) {
		super(value);
	}

	public ByteTag(boolean value) {
		super((byte) (value ? 1 : 0));
	}

	/** {@inheritDoc} */
	@Override
	public byte getID() {
		return ID;
	}

	public boolean asBoolean() {
		// TODO(bug): MC uses `.asByte() != 0` for truthiness - and asBoolean is valid on all NumberTags (even float and double)
		return getValue() > 0;
	}

	/**
	 * Sets the value for this Tag directly.
	 * @param value The value to be set.
	 */
	public void setValue(byte value) {
		super.setValue(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		return super.equals(other) && asByte() == ((ByteTag) other).asByte();
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(ByteTag other) {
		return getValue().compareTo(other.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public ByteTag clone() {
		return new ByteTag(getValue());
	}
}
