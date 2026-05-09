package io.github.ensgijs.nbt.tag;

import java.util.Arrays;
import java.util.stream.LongStream;

public class LongArrayTag extends ArrayTag<long[]> implements Comparable<LongArrayTag> {

	public static final byte ID = 12;
	public static final long[] ZERO_VALUE = new long[0];

	public LongArrayTag() {
		super(ZERO_VALUE);
	}

	public LongArrayTag(long... value) {
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
		return super.equals(other) && Arrays.equals(getValue(), ((LongArrayTag) other).getValue());
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Arrays.hashCode(getValue());
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(LongArrayTag other) {
		int k = Integer.compare(length(), other.length());
		if (k != 0) return k;
		return Arrays.compare(getValue(), other.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public LongArrayTag clone() {
		return new LongArrayTag(Arrays.copyOf(getValue(), length()));
	}

	public LongStream stream() {
		return Arrays.stream(getValue());
	}
}
