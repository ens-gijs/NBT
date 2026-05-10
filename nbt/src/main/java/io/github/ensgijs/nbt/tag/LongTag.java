package io.github.ensgijs.nbt.tag;

public class LongTag extends NumberTag<Long> implements Comparable<LongTag> {

	public static final byte ID = 4;
	public static final long ZERO_VALUE = 0L;

	public LongTag() {
		super(ZERO_VALUE);
	}

	public LongTag(long value) {
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
	public void setValue(long value) {
		super.setValue(value);
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		return super.equals(other) && asLong() == ((LongTag) other).asLong();
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(LongTag other) {
		return getValue().compareTo(other.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public LongTag clone() {
		return new LongTag(getValue());
	}
}
