package io.github.ensgijs.nbt.tag;

import java.util.Arrays;
import java.util.stream.IntStream;

public class IntArrayTag extends ArrayTag<int[]> implements Comparable<IntArrayTag> {

	public static final byte ID = 11;
	public static final int[] ZERO_VALUE = new int[0];

	public IntArrayTag() {
		super(ZERO_VALUE);
	}

	public IntArrayTag(int... value) {
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
		return super.equals(other) && Arrays.equals(getValue(), ((IntArrayTag) other).getValue());
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Arrays.hashCode(getValue());
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(IntArrayTag other) {
		int k = Integer.compare(length(), other.length());
		if (k != 0) return k;
		return Arrays.compare(getValue(), other.getValue());
	}

	/** {@inheritDoc} */
	@Override
	public IntArrayTag clone() {
		return new IntArrayTag(Arrays.copyOf(getValue(), length()));
	}

	public IntStream stream() {
		return Arrays.stream(getValue());
	}
}
