package io.github.ensgijs.nbt.tag;

import java.util.Arrays;

public class ByteArrayTag extends ArrayTag<byte[]> implements Comparable<ByteArrayTag> {

	public static final byte ID = 7;
	public static final byte[] ZERO_VALUE = new byte[0];

	public ByteArrayTag() {
		super(ZERO_VALUE);
	}

	public ByteArrayTag(byte... value) {
		super(value);
	}

	/** {@inheritDoc} */
	@Override
	public byte getID() {
		return ID;
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object other) {
		return super.equals(other) && Arrays.equals(getValue(), ((ByteArrayTag) other).getValue());
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Arrays.hashCode(getValue());
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(ByteArrayTag other) {
		int k = Integer.compare(length(), other.length());
		if (k != 0) return k;
		return Arrays.compare(getValue(), other.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public ByteArrayTag clone() {
		return new ByteArrayTag(Arrays.copyOf(getValue(), length()));
	}
}
