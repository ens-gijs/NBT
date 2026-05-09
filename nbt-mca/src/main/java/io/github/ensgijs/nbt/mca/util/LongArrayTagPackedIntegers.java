package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.mca.DataVersion;
import io.github.ensgijs.nbt.mca.TerrainChunk;
import io.github.ensgijs.nbt.tag.LongArrayTag;
import io.github.ensgijs.nbt.util.ArgValidator;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.IntPredicate;

import static io.github.ensgijs.nbt.mca.DataVersion.JAVA_1_16_20W17A;
import static io.github.ensgijs.nbt.mca.DataVersion.UNKNOWN;

/**
 * A packed array of numeric values stored in an array of 64-bit integers. Each value
 * occupies a fixed number of bits, that number of bits is not necessarily a constant, it can be resized as-needed,
 * but each value occupies the same number of bits as all others.
 *
 * <p>This class maintains its values in the packed long[] in the {@link LongArrayTag} given to / created at
 * construction time. This can result in significant memory savings over when working with a large number of
 * packed value arrays but will, of course, use more CPU to get and set values than an int[] would.
 * If you would prefer to work with int[]'s you can use the family of {@link #toArray} and {@link #setFromArray}
 * functions.</p>
 *
 * <p>Negative values are supported by the use of {@link #setValueOffset(int)}, the actual stored data
 * is always GE 0, valueOffset is for your convenience when accessing and modifying the data.
 * See below about heightmaps.</p>
 *
 * <p>This class supports automatic increasing resizes as-needed on any calls which add or modify values
 * and provides {@link #compact()} to compact and shrink the long[] buffer to the minimum required size
 * while respecting {@link #getMinBitsPerValue()}.</p>
 *
 * <p>WARNING: The user is responsible for calling {@link #compact()} before storing the {@link LongArrayTag}
 * in MCA data! Failing to do so may cause Minecraft to fail to properly interpret the values. Getting the
 * tag via {@link #updateHandle()} does this for you.</p>
 *
 * <p>The packing strategy may be changed to facilitate upgrade/downgrade operations by calling
 * {@link #setPackingStrategy(PackingStrategy)}</p>
 *
 * <p>Tip: If you are using this class to create packed ints from scratch it is recommended to add values in
 * descending order (or at least start with larger values rather than smaller ones) to minimize the number
 * of resizes that occur. OR to take advantage of the {@link Builder#initializeForStoring(int)} which will
 * initialize the long[] with sufficient capacity to store values of at least the specified magnitude.</p>
 *
 * <p>Even if you are not creating a new packed buffer from scratch you still should specify a correct
 * {@link Builder#initializeForStoring(int)}. This value can usually be computed from context outside the
 * packed buffer itself such as the palette size minus 1.
 *
 * <h2>About Heightmaps</h2>
 * In the case of heightmap data {@link Builder#initializeForStoring(int)} should be set
 * to the world build height {@code chunk.<determine max sectionY> * 16 + 15} and also set
 * {@link Builder#valueOffset(int)} to {@code chunk.getChunkY() * 16 - 1}
 * 
 * <p>In some cases it is sufficient to only specify {@link Builder#minBitsPerValue(int)}, really there is
 * a single case where this is valid and that is for Heightmaps which should use a minBitsPerValue of 9 and
 * so long as the assumption that the world build height will remain unchanged from about -64 to 320, and
 * that no world height modifying datapacks are used, it will work. Note that 9 bits gives room for the
 * value range of [0..511] - but you'll still want to set {@link Builder#valueOffset(int)} to abstract away
 * the world bottom offset.</p>
 *
 * <p><em>Tip: since we're on the subject of heightmap data - it's not stored as blockY,
 * instead it's stored as number of blocks from world bottom which means that for worlds which bottom out
 * at Y=0 you'll want to set valueOffset = -1 to account for this. The above formula will always work and
 * should be preferred over setting a hard coded -1.</em></p>
 *
 * @see #builder()
 */
public class LongArrayTagPackedIntegers implements TagWrapper<LongArrayTag>, Iterable<Integer>, Cloneable {

    public static final VersionAware<PackingStrategy> MOJANG_PACKING_STRATEGY = new VersionAware<PackingStrategy>()
            // technically, I don't believe long packing was used in any form until JAVA_1_12_2... or was it JAVA_1_13_17W47A
            .register(UNKNOWN.next().id(), PackingStrategy.SPLIT_VALUES_ACROSS_LONGS)
            .register(JAVA_1_16_20W17A.id(), PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS);

    // <editor-fold desc="RemapFunction, PackingStrategy, Builder" defaultstate="collapsed">
    @FunctionalInterface
    public interface RemapFunction {
        int remap(int value);
    }

    public enum PackingStrategy {
        /**
         * Values are never split across longs resulting in unused bits in every backing long if 64 is not
         * evenly divisible by the bits per value
         * <p>While this strategy wastes more bits, it is computationally
         * simpler, uses no floating point math, and is therefore faster.</p>
         * @since {@link io.github.ensgijs.nbt.mca.DataVersion#JAVA_1_16_20W17A}
         */
        NO_SPLIT_VALUES_ACROSS_LONGS,
        // Packing Math (SPLIT_VALUES_ACROSS_LONGS):
        //   L := number of longs required
        //   C := capacity
        //   B := bits per value
        //   64:= bits per long
        //   L =  ceil(C * B / 64d)
        //   B = floor(L * 64d / C)
        //   C = floor(L * 64d / B)
        /**
         * Values are tightly packed and are split across backing longs if 64 is not evenly divisible by the bits
         * per value. This results in a minimum number of unused bits. In fact the only place unused bits may exist
         * when using this strategy is in the last long in the array.
         */
        SPLIT_VALUES_ACROSS_LONGS
    }


    /**
     * The only way to construct a {@link LongArrayTagPackedIntegers} because its constructor is too gnarly to
     * expose directly and would require a lot of "hey those look confusingly similar" overloads.
     */
    public static class Builder {
        private int capacity;
        private int valueOffset = 0;
        private int minBitsPerValue;
        private int initializeForStoring = Integer.MIN_VALUE;
        private PackingStrategy packingStrategy = MOJANG_PACKING_STRATEGY.get(DataVersion.latest().id());

        /** Use {@link #builder()} instead. */
        private Builder() {}

        /**
         * Count of values to be stored, this value cannot be changed once built and
         * is usually one of: 64, 256, 4096.
         */
        public Builder length(int length) {
            ArgValidator.check(length > 0);
            this.capacity = length;
            return this;
        }

        /**
         * Value offset which is applied to all values as they pass through the various store and retrieve functions.
         * <p>In practice this is (currently) only applicable to Heightmap data which always has a negative component.</p>
         */
        public Builder valueOffset(int valueOffset) {
            this.valueOffset = valueOffset;
            return this;
        }

        /**
         * The minimum bits per value used to store data. {@link #compact()} will respect this setting.
         * <p>For all MC versions to date:
         * <ul>
         *     <li>Block palettes must specify 4</li>
         *     <li>Biome palettes must specify 1</li>
         *     <li>Heightmaps must specify 9</li>
         * </ul>
         */
        public Builder minBitsPerValue(int minBitsPerValue) {
            ArgValidator.check(minBitsPerValue >= 0);
            this.minBitsPerValue = minBitsPerValue;
            return this;
        }

        /**
         * Computes an initial bits per value sufficient to hold the specified largestValue (inclusive).
         * Therefor making it possible to add values from {@link #valueOffset} up to this value (both inclusive)
         * without triggering a long[] resize.
         *
         * <p><b>It is important to set this value!</b> This value can usually be computed from context outside the
         * packed buffer itself such as the palette size minus 1. For details about heightmaps see
         * {@link LongArrayTagPackedIntegers} class docs.</p>
         *
         * <p>This value is taken relative to {@link #valueOffset} to compute the number of bits per value required.</p>
         */
        public Builder initializeForStoring(int largestValue) {
            ArgValidator.check(largestValue >= 0);
            this.initializeForStoring = largestValue;
            return this;
        }

        /**
         * The packing strategy controls how bits are packed into the long[].
         * <p>{@link #dataVersion} is generally more useful.</p>
         * Use a static import to keep code lines with calls to this method from being overly long:
         * <pre>{@code
         * import static io.github.ensgijs.nbt.mca.util.LongArrayTagPackedIntegers.PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS;
         * import static io.github.ensgijs.nbt.mca.util.LongArrayTagPackedIntegers.PackingStrategy.SPLIT_VALUES_ACROSS_LONGS;
         * }</pre>
         * @see #dataVersion
         */
        public Builder packingStrategy(PackingStrategy packingStrategy) {
            this.packingStrategy = packingStrategy;
            return this;
        }

        /**
         * Sets the packing strategy based on data version.
         * @param dataVersion if EQ 0 then {@link DataVersion#latest()}.id() is used.
         */
        public Builder dataVersion(int dataVersion) {
            ArgValidator.check(dataVersion >= 0);
            this.packingStrategy = MOJANG_PACKING_STRATEGY.get(
                    dataVersion > 0 ? dataVersion : DataVersion.latest().id());
            return this;
        }

        /**
         * Sets the packing strategy based on data version.
         * @param dataVersion if EQ {@link DataVersion#UNKNOWN} then {@link DataVersion#latest()} is used.
         */
        public Builder dataVersion(DataVersion dataVersion) {
            ArgValidator.check(dataVersion != null);
            return dataVersion(dataVersion.id());
        }

        /**
         * Sets the capacity to match the length of the given values and sets an appropriate initialBitsPerValue.
         *
         * <p>Note: while {@link #initializeForStoring(int)} and {@link #length(int)} will be set for you,
         * it's still important to properly set {@link #minBitsPerValue(int)} and {@link #valueOffset(int)}.</p>
         */
        public LongArrayTagPackedIntegers build(int[] values) {
            capacity = values.length;
            LongArrayTagPackedIntegers packed = build(new LongArrayTag());
            packed.setFromArray(values);
            return packed;
        }

        /** Builds using a new {@link LongArrayTag} as the long[] buffer. */
        public LongArrayTagPackedIntegers build() {
            return build(new LongArrayTag());
        }

        /**
         * Builds using the given {@link LongArrayTag} as the long[] buffer.
         * <p>It's not possible to reliably compute the required packing parameters given a long[] alone.
         * You must specify at least {@link #dataVersion} and ({@link #initializeForStoring(int)} or
         * {@link #minBitsPerValue(int)}).</p>
         */
        public LongArrayTagPackedIntegers build(LongArrayTag tag) {
            ArgValidator.requireValue(tag);
            if (capacity == 0)
                throw new IllegalArgumentException("capacity is required");
            if (packingStrategy == null)
                throw new IllegalArgumentException("packingStrategy or dataVersion is required");
            int initialBitsPerValue = 0;
            if (initializeForStoring != Integer.MIN_VALUE) {
                initialBitsPerValue = calculateBitsRequired(initializeForStoring - valueOffset);
            }
            return new LongArrayTagPackedIntegers(
                    tag, packingStrategy, capacity, minBitsPerValue, initialBitsPerValue, valueOffset);
        }
    }

    /** Creates a new builder. */
    public static Builder builder() {
        return new Builder();
    }
    // </editor-fold>

    /** Number of values stored - <em>not the length of the long array.</em> */
    public final int length;
    /**
     * DO NOT ACCESS DIRECTLY
     * @see #cubeEdgeLength()
     */
    private int cubeEdgeLength;
    /**
     * DO NOT ACCESS DIRECTLY
     * @see #squareEdgeLength()
     */
    private int squareEdgeLength;
    private final LongArrayTag packedBitsTag;
    private int valueOffset;

    private PackingStrategy packingStrategy;
    private long[] packedBits;
    private int minBitsPerValue;
    private int bitsPerValue;
    /** Inclusive bound, does NOT include valueOffset */
    private int currentMaxPackableValue;
    private int noSplitIndicesPerLong;
    private double splitIndicesPerLong;

    /** set to -1 if length does not have an integer cube root */
    public int cubeEdgeLength() {
        if (cubeEdgeLength > 0) {
            return cubeEdgeLength;
        }
        int tmp = (int) Math.round(Math.pow(length, 1/3d));
        return this.cubeEdgeLength = tmp * tmp * tmp == length ? tmp : -1;
    }

    /** set to -1 if length does not have an integer square root */
    public int squareEdgeLength() {
        if (squareEdgeLength > 0) {
            return squareEdgeLength;
        }
        int tmp = (int) Math.round(Math.sqrt(length));
        return this.squareEdgeLength = tmp * tmp == length ? tmp : -1;
    }

    /**
     * @param tag Tag with existing longs. If this tag's {@link LongArrayTag#getValue()#length} is 0 it is initialized
     *            with an appropriately sized long[], otherwise this length is validated based on the other provided arguments.
     * @param packingStrategy Controls how values are packed into the long array.
     *                       Prior to {@link io.github.ensgijs.nbt.mca.DataVersion#JAVA_1_16_20W17A}
     *                       {@link PackingStrategy#SPLIT_VALUES_ACROSS_LONGS} was used, from this version on
     *                       {@link PackingStrategy#NO_SPLIT_VALUES_ACROSS_LONGS} is used.
     * @param length Count of values to be stored, this value cannot be changed and is usually one of: 64, 256, 4096
     * @param minBitsPerValue Minimum bits per value used to store values. Must be GE 1.
     *                        In practice for unchanging things (such as biomes) this is always 1 and for things which
     *                        may change often (such as block palettes) this is always 4. Presumably setting this value
     *                        above 1 reduces frequent resizing.
     * @param initialBitsPerValue Specifies the number of bits-per-value to initialize to / use to decode the initial
     *                            long[] buffer. If minBitsPerValue is GT this value, this value has no effect.
     * @param valueOffset Translates the values passed to / returned from {@link #set(int, int)} and {@link #get(int)}
     *                    by this fixed amount.
     *                    <p>The stored values are always GE 0, however, since height map data is packed and those
     *                    values may represent negative values you can set a valueOffset of (-65 - or more precisely
     *                    {@link TerrainChunk#getChunkY()} * 64 - 1) to interact with this LongArrayTagPackedIntegers
     *                    instance in world coordinates instead of having to calculate those offsets yourself.</p>
     */
    private LongArrayTagPackedIntegers(LongArrayTag tag, PackingStrategy packingStrategy, int length, int minBitsPerValue, int initialBitsPerValue, int valueOffset) {
        ArgValidator.requireValue(tag, "tag");
        ArgValidator.requireValue(packingStrategy, "packingStrategy");
        ArgValidator.check(minBitsPerValue > 0 && minBitsPerValue < 32, "minBitsPerValue must be in range [1..31]");
        this.packingStrategy = packingStrategy;
        this.length = length;
        this.minBitsPerValue = minBitsPerValue;
        this.bitsPerValue = Math.max(minBitsPerValue, initialBitsPerValue);
        this.valueOffset = valueOffset;
        this.packedBitsTag = tag;
        int expectLongCount;
        if (packingStrategy == PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS) {
            expectLongCount = (int) Math.ceil(length / (double) (64 / bitsPerValue));
            this.noSplitIndicesPerLong = 64 / bitsPerValue;
        } else {
            expectLongCount = (int) Math.ceil(bitsPerValue * length / 64d);
            this.splitIndicesPerLong = 64D / bitsPerValue;
        }
        if (tag.getValue().length == 0) {
            tag.setValue(new long[expectLongCount]);
        } else {
            if (expectLongCount != tag.getValue().length) {
                throw new IllegalArgumentException(String.format(
                        "long array tag has %d longs, but expected %d longs",
                        tag.getValue().length, expectLongCount));
            }
        }
        packedBits = tag.getValue();
        this.currentMaxPackableValue = (1 << bitsPerValue) - 1;
    }

    protected LongArrayTagPackedIntegers(LongArrayTagPackedIntegers other) {
        this.length = other.length;
        this.valueOffset = other.valueOffset;
        this.minBitsPerValue = other.minBitsPerValue;
        this.bitsPerValue = other.bitsPerValue;
        this.currentMaxPackableValue = other.currentMaxPackableValue;
        this.noSplitIndicesPerLong = other.noSplitIndicesPerLong;
        this.splitIndicesPerLong = other.splitIndicesPerLong;
        this.packingStrategy = other.packingStrategy;
        this.packedBitsTag = other.packedBitsTag.clone();
        this.packedBits = this.packedBitsTag.getValue();
    }

    @Override
    public LongArrayTagPackedIntegers clone() {
        return new LongArrayTagPackedIntegers(this);
    }

    /**
     * The long array tag which is being used to store the packed values.
     * <p>Note: this is the same tag instance that was passed to the constructor and will contain the current
     * longs[]. However, the user is responsible for calling {@link #compact()} before storing this tag in
     * MCA data!</p>
     * <p>Generally, if you are getting the tag to store it <b>use {@link #updateHandle()} instead</b>.</p>
     */
    @Override
    public LongArrayTag getHandle() {
        return packedBitsTag;
    }

    /**
     * Calls {@link #compact()} and returns the {@link LongArrayTag}.
     * <p>Note this is the same {@link LongArrayTag} instance given to, or created by, {@link Builder#build}.</p>
     */
    @Override
    public LongArrayTag updateHandle() {
        return compact();
    }

    /** Returns the actual longs array - modifying the values in this array will modify the stored values. */
    public long[] longs() {
        return packedBits;
    }

    /**
     * The packing strategy controls how bits are packed into longs. Changing the packing strategy results
     * in the backing long array being recomputed to use the new strategy.
     */
    public PackingStrategy getPackingStrategy() {
        return packingStrategy;
    }

    /**
     * The packing strategy controls how bits are packed into longs. Changing the packing strategy results
     * in the backing long array being recomputed to use the new strategy.
     */
    public void setPackingStrategy(PackingStrategy packingStrategy) {
        ArgValidator.requireValue(packingStrategy);
        resize(bitsPerValue, packingStrategy);
    }

    /**
     * Gets the current number of bits per value actually used. This may be less than {@link #getMinBitsPerValue()}.
     * <p>Note, returns 0 iff all stored raw values are zero (which means they are all EQ {@link #getValueOffset()}).</p>
     * @see #shouldCompact()
     */
    public int getActualUsedBitsPerValue() {
        int maxValue = 0;
        for (int i = 0; i < length; i++) {
            maxValue = Math.max(maxValue, getRaw(i));
        }
        return calculateBitsRequired(maxValue);
    }

    /** The minimum bits per value used to store data. {@link #compact()} will respect this setting. */
    public int getMinBitsPerValue() {
        return minBitsPerValue;
    }

    /**
     * The minimum bits per value used to store data. {@link #compact()} will respect this setting.
     * <p>If the provided value is larger than the current bits per value the longs array is resized to accommodate.</p>
     * <p>This method never calls {@link #compact()} - but if you are decreasing this value you probably should.</p>
     */
    public void setMinBitsPerValue(int minBitsPerValue) {
        ArgValidator.check(minBitsPerValue > 0 && minBitsPerValue < 32, "minBitsPerValue must be in range [1..31]");
        this.minBitsPerValue = minBitsPerValue;
        if (minBitsPerValue > bitsPerValue) {
            resize(minBitsPerValue, packingStrategy);
        }
    }

    /** Value offset which is applied to all values as they pass through the various store and retrieve functions. */
    public int getValueOffset() {
        return valueOffset;
    }

    /** Value offset which is applied to all values as they pass through the various store and retrieve functions. */
    public void setValueOffset(int valueOffset) {
        this.valueOffset = valueOffset;
    }

    /** The current bits per value used to store data. */
    public int getBitsPerValue() {
        return bitsPerValue;
    }

    /**
     * The maximum value, inclusive, which can be stored without triggering a resize.
     * The returned value has any offset applied.
     * @see #getValueOffset()
     */
    public int getCurrentMaxPackableValue() {
        return currentMaxPackableValue + valueOffset;
    }

    /** Synonym for {@link #getValueOffset()}. */
    public int getCurrentMinPackableValue() {
        return valueOffset;
    }

    /** Gets the value at the specified index. */
    public int get(int index) {
        return getRaw(index) + valueOffset;
    }

    private int indexOf(int x, int z) {
        int xzSize = squareEdgeLength();
        if (xzSize <= 0)
            throw new IllegalStateException();
        if (x < 0 || z < 0 || x >= xzSize || z >= xzSize)
            throw new IndexOutOfBoundsException();
        return z * xzSize + x;
    }

    private int indexOf(int x, int y, int z) {
        int xyzSize = cubeEdgeLength();
        if (xyzSize <= 0)
            throw new IllegalStateException();
        if (x < 0 || y < 0 ||z < 0 || x >= xyzSize || y >= xyzSize || z >= xyzSize)
            throw new IndexOutOfBoundsException();
        return y * xyzSize * xyzSize + z * xyzSize + x;
    }

    public int get2d(int x, int z) {
        return get(indexOf(x, z));
    }

    public int get3d(int x, int y, int z) {
        return get(indexOf(x, y, z));
    }

    /** Does not apply valueOffset */
    private int getRaw(int index) {
        if (index < 0 || index >= length)
            throw new IndexOutOfBoundsException();
        if (packingStrategy == PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS) {
            int longIndex = index / noSplitIndicesPerLong;
            int startBit = (index % noSplitIndicesPerLong) * bitsPerValue;
            return (int) bitRange(packedBits[longIndex], startBit, startBit + bitsPerValue);
        } else {
            double floatingIndex = index / splitIndicesPerLong;
            int longIndex = (int) floatingIndex;
            int startBit = (int) ((floatingIndex - longIndex) * 64D);
            if (startBit + bitsPerValue > 64) {
                long prev = bitRange(packedBits[longIndex], startBit, 64);
                long next = bitRange(packedBits[longIndex + 1], 0, startBit + bitsPerValue - 64);
                return (int) ((next << 64 - startBit) + prev);
            } else {
                return (int) bitRange(packedBits[longIndex], startBit, startBit + bitsPerValue);
            }
        }
    }

    /** Sets the value at the specified index. */
    public void set(int index, int value) {
        if (index < 0 || index >= length)
            throw new IndexOutOfBoundsException();
        setRaw(index, value - valueOffset);
    }

    public void set2d(int x, int z, int value) {
        set(indexOf(x, z), value);
    }

    public void set3d(int x, int y, int z, int value) {
        set(indexOf(x, y, z), value);
    }

    /** valueOffset should already be removed from the supplied value */
    private void setRaw(int index, int rawValue) {
        if (rawValue < 0)
            throw new IllegalArgumentException("value must be GE " + valueOffset);
        if (rawValue > currentMaxPackableValue) {
            resize(calculateBitsRequired(rawValue), packingStrategy);
        }
        if (packingStrategy == PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS) {
            setNoSplitIndices(index, rawValue, noSplitIndicesPerLong, bitsPerValue, packedBits);
        } else {
            setSplitIndices(index, rawValue, splitIndicesPerLong, bitsPerValue, packedBits);
        }
    }

    /** Sets all values to the zero value and shrinks the longs array if appropriate and if autoShrink is true. */
    public void clear(boolean autoShrink) {
        if (!autoShrink || bitsPerValue == minBitsPerValue) {
            Arrays.fill(packedBits, 0L);
        } else {
            reallocateCapacity(minBitsPerValue);
        }
    }

    /**
     * if bitsPerValue == requiredBitsPerValue does nothing, otherwise creates a new long array and updates
     * tracking fields.
     * <p>WARNING: does NOT enforce minBitsPerValue!</p>
     */
    private void reallocateCapacity(int requiredBitsPerValue) {
        if (bitsPerValue == requiredBitsPerValue)
            return;
        bitsPerValue = requiredBitsPerValue;
        if (packingStrategy == PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS) {
            final int newLength = (int) Math.ceil(length / (double) (64 / bitsPerValue));
            packedBitsTag.setValue(packedBits = new long[newLength]);
            noSplitIndicesPerLong = 64 / bitsPerValue;
            splitIndicesPerLong = 0;
        } else {
            final int newLength = (int) Math.ceil((bitsPerValue * length) / 64d);
            packedBitsTag.setValue(packedBits = new long[newLength]);
            splitIndicesPerLong = 64D / bitsPerValue;
            noSplitIndicesPerLong = 0;
        }
        currentMaxPackableValue = (1 << bitsPerValue) - 1;
    }

    /** True if the given value is found in the current set of values. */
    public boolean contains(int value) {
        value -= valueOffset;
        if (value < 0 || value > currentMaxPackableValue)
            return false;

        for (int i = 0; i < length; i++) {
            if (value == getRaw(i)) {
                return true;
            }
        }
        return false;
    }

    /** Counts the number of occurrences of the given value. */
    public int count(int value) {
        value -= valueOffset;
        if (value < 0 || value > currentMaxPackableValue)
            return 0;
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (value == getRaw(i)) {
                count ++;
            }
        }
        return count;
    }

    /** Counts the number of times the given tester returns true while being passed the entire set of values. */
    public int count(IntPredicate tester) {
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (tester.test(get(i))) {
                count ++;
            }
        }
        return count;
    }

    /**
     * Returns whether all elements of this packed array match the provided value.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     */
    public boolean allMatch(int value) {
        return length == count(value);
    }

    /**
     * Returns whether all elements of this packed array match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     */
    public boolean allMatch(IntPredicate tester) {
        return length == count(tester);
    }

    /**
     * Replaces all occurrences of oldValue with newValue.
     */
    public void replaceAll(int oldValue, int newValue) {
        if (oldValue == newValue) return;
        oldValue -= valueOffset;
        newValue -= valueOffset;
        if (oldValue < 0)
            throw new IllegalArgumentException("oldValue must be GE " + valueOffset);
        if (newValue < 0)
            throw new IllegalArgumentException("newValue must be GE " + valueOffset);
        for (int i = 0; i < length; i++) {
            int v = getRaw(i);
            if (v == oldValue) {
                setRaw(i, newValue);
            }
        }
    }

    /**
     * Remaps all values. The remapping function is given each value and its return value is assigned to the current
     * index. Return the same value passed if you don't wish to remap it.
     * @param remapFunction remapping function.
     */
    public void remap(RemapFunction remapFunction) {
        ArgValidator.requireValue(remapFunction);
        for (int i = 0; i < length; i++) {
            int oldOffsetValue = get(i);
            int newOffsetValue = remapFunction.remap(oldOffsetValue);
            if (newOffsetValue < valueOffset)
                throw new IllegalArgumentException("remapped value must be GE " + valueOffset);
            if (oldOffsetValue != newOffsetValue) {
                set(i, newOffsetValue);
            }
        }
    }

    /**
     * Remaps all indicated values.
     * @param remapping remapping map.
     */
    public void remap(Map<Integer, Integer> remapping) {
        if (remapping.isEmpty())
            return;
        remap(v -> remapping.getOrDefault(v, v));
    }

    /**
     * Calculates if {@link #compact()} should be called.
     * <p>Note that it is MORE efficient to just call {@link #compact()} than to call this method first.</p>
     */
    public boolean shouldCompact() {
        return bitsPerValue > Math.max(minBitsPerValue, getActualUsedBitsPerValue());
    }

    /**
     * Compacts the long array data to use the minimum number of bits per value required.
     * Obeys {@link #getMinBitsPerValue()}
     * @return tag containing long[]
     */
    public LongArrayTag compact() {
        resize(getActualUsedBitsPerValue(), packingStrategy);
        return packedBitsTag;
    }

    /** Creates a new int[] and populates it by calling {@link #get(int)} successively. */
    public int[] toArray() {
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = get(i);
        }
        return values;
    }

    /**
     * Populates the given array by calling {@link #get(int)} successively.
     * @param array must be exactly {@link #length} in size.
     * @return the same array that was passed as an argument.
     */
    public int[] toArray(int[] array) {
        ArgValidator.check(array.length == length,
                String.format("Expected array to be of length %d but it was %d", length, array.length));
        for (int i = 0; i < length; i++) {
            array[i] = get(i);
        }
        return array;
    }

    /**
     * Populates the given array from startIndex by calling {@link #get(int)} successively.
     * @param array receives values from startIndex to startIndex + capacity - 1
     * @param startIndex the index to start copying values into.
     * @return the same array that was passed as an argument.
     */
    public int[] toArray(int[] array, int startIndex) {
        ArgValidator.check(startIndex >= 0 && (startIndex + length) <= array.length);
        for (int i = 0; i < length; i++) {
            array[i + startIndex] = get(i);
        }
        return array;
    }

    /**
     * Resizes the long[] to exactly hold the range of values given, respecting {@link #getMinBitsPerValue()},
     * checks that all values are in the allowed range (GE {@link #getValueOffset()}), then calls {@link #set}
     * successively for each value.
     * <p>There is never a need to call {@link #compact()} immediately following this call.</p>
     * @param values must be exactly {@link #length} in size.
     * @throws IllegalArgumentException if any value is LT {@link #getValueOffset()}.
     */
    public void setFromArray(int[] values) {
        ArgValidator.check(values.length == length,
                String.format("Expected array to be of length %d but it was %d", length, values.length));
        setFromArray(values, 0);
    }

    /**
     * Resizes the long[] to exactly hold the range of values given, respecting {@link #getMinBitsPerValue()},
     * checks that all values are in the allowed range (GE {@link #getValueOffset()}), then calls {@link #set}
     * successively for each value.
     * <p>There is never a need to call {@link #compact()} immediately following this call.</p>
     * @param values must be at least {@link #length} in size.
     * @param startIndex the index to start copying values from.
     * @throws IllegalArgumentException if any value is LT {@link #getValueOffset()}.
     */
    public void setFromArray(int[] values, int startIndex) {
        ArgValidator.check(startIndex >= 0 && (startIndex + length) <= values.length);
        int maxVal = valueOffset;
        for (int i = 0; i < length; i++) {
            int v = values[i + startIndex];
            if (v < valueOffset)
                throw new IllegalArgumentException(
                        String.format("Values array contained %d which is smaller than the current value offset " +
                                "(minimum allowed value) of %d", v, valueOffset));
            maxVal = Math.max(maxVal, v);
        }
        reallocateCapacity(Math.max(minBitsPerValue, calculateBitsRequired(maxVal - valueOffset)));
        for (int i = 0; i < length; i++) {
            set(i, values[i + startIndex]);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("length=").append(length).append("; ");
        sb.append("packing-strategy=").append(packingStrategy.name()).append("; ");
        sb.append("min-bits-per-value=").append(minBitsPerValue).append("; ");
        sb.append("bits-per-value=").append(bitsPerValue).append("; ");
        sb.append("value-offset=").append(valueOffset).append("; ");
        sb.append("values=[");
        boolean notFirst = false;
        for (int v : this) {
            if (notFirst) {
                sb.append(", ");
            } else {
                notFirst = true;
            }
            sb.append(v);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Renders the values as a 2D grid where 0,0 is in the top left.
     * @throws UnsupportedOperationException if {@link #length} doesn't have an integer square root
     * (isn't the product of n^2)
     */
    public String toString2dGrid() {
        final int rectangleEdgeLength = squareEdgeLength();
        if (rectangleEdgeLength < 0)
            throw new UnsupportedOperationException(
                    "Attempted to format as a 2D grid, but sqrt(count of values) is not an integer!");
        int maxStrLen = 0;
        for (int i = 0; i < length; i++) {
            maxStrLen = Math.max(maxStrLen, Integer.toString(get(i)).length());
        }
        String format = "%" + maxStrLen + "d";
        StringBuilder sb = new StringBuilder();
        int wrap = rectangleEdgeLength - 1;
        for (int i = 0; i < length; i++) {
            sb.append(String.format(format, get(i)));
            if (i % rectangleEdgeLength != wrap) {
                sb.append(' ');
            } else {
                sb.append('\n');
            }
        }
        return sb.toString().stripTrailing();
    }

    /**
     * Renders the values as a 3D grid where each 2D slice is rendered with 0,0 in the top left.
     * @throws UnsupportedOperationException if {@link #length} doesn't have an integer cubic root
     * (isn't the product of n^3)
     */
    public String toString3dGrid() {
        final int cubeEdgeLength = cubeEdgeLength();
        if (cubeEdgeLength < 0)
            throw new UnsupportedOperationException(
                    "Attempted to format as a 3D grid, but cube-root(count of values) is not an integer!");
        int maxStrLen = 0;
        for (int i = 0; i < length; i++) {
            maxStrLen = Math.max(maxStrLen, Integer.toString(get(i)).length());
        }
        String format = "%" + maxStrLen + "d";
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < cubeEdgeLength; y++) {
            int yi = y * cubeEdgeLength * cubeEdgeLength;
            if (y > 0) sb.append('\n');
            sb.append("Y=").append(y);
            for (int z = 0; z < cubeEdgeLength; z++) {
                sb.append('\n');
                int zyi = yi + z * cubeEdgeLength;
                for (int x = 0; x < cubeEdgeLength; x++) {
                    if (x > 0) sb.append(' ');
                    sb.append(String.format(format, get(zyi + x)));
                }
            }
        }
        return sb.toString();
    }

    /**
     * Calls {@link MessageDigest#update} on the given digest with the current long[] data to accumulate a checksum
     * across one or more {@link LongArrayTagPackedIntegers}/.
     * @param digest to be modified with current long[] data.
     * @return given digest.
     */
    public MessageDigest accumulateChecksum(MessageDigest digest) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES * longs().length);
        for (long l : longs()) {
            buffer.putLong(l);
        }
        buffer.position(0);
        digest.update(buffer);
        return digest;
    }

    static void setNoSplitIndices(int index, int value, int noSplitIndicesPerLong, int bitsPerValue, long[] packedBits) {
        int blockStatesIndex = index / noSplitIndicesPerLong;
        int startBit = (index % noSplitIndicesPerLong) * bitsPerValue;
        packedBits[blockStatesIndex] = updateBits(packedBits[blockStatesIndex], value, startBit, startBit + bitsPerValue);
    }

    static void setSplitIndices(int index, int value, double splitIndicesPerLong, int bitsPerValue, long[] packedBits) {
        double floatingIndex = index / splitIndicesPerLong;
        int longIndex = (int) floatingIndex;
        int startBit = (int) ((floatingIndex - longIndex) * 64D);
        if (startBit + bitsPerValue > 64) {
            packedBits[longIndex] = updateBits(packedBits[longIndex], value, startBit, 64);
            packedBits[longIndex + 1] = updateBits(packedBits[longIndex + 1], value, startBit - 64, startBit + bitsPerValue - 64);
        } else {
            packedBits[longIndex] = updateBits(packedBits[longIndex], value, startBit, startBit + bitsPerValue);
        }
    }

    /**
     * Increases or decreases the amount of bits used per value based on the size of the palette.
     * Can also be used to repack the longs with a new packing strategy.
     * <p>Obeys minBitsPerValue</p>
     */
    private void resize(int newBitsPerValue, final PackingStrategy newPackingStrategy) {
        newBitsPerValue = Math.max(minBitsPerValue, newBitsPerValue);
        if (newBitsPerValue == bitsPerValue && newPackingStrategy == packingStrategy)
            return;

        final int newMaxValidValue = (int) Math.pow(2, newBitsPerValue) - 1;
        if (newPackingStrategy == PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS) {
            final int newLength = (int) Math.ceil(length / (double) (64 / newBitsPerValue));
            final long[] newLongs = new long[newLength];
            final int newNoSplitIndicesPerLong = 64 / newBitsPerValue;

            for (int i = 0; i < length; i++) {
                int value = getRaw(i);
                if (value > newMaxValidValue) {
                    throw new IllegalArgumentException(
                            "newBitsPerValue is too small to hold existing value " + value + valueOffset);
                }
                setNoSplitIndices(i, value, newNoSplitIndicesPerLong, newBitsPerValue, newLongs);
            }
            noSplitIndicesPerLong = newNoSplitIndicesPerLong;
            splitIndicesPerLong = 0;
            packedBits = newLongs;
        } else {
            final int newLength = (int) Math.ceil((newBitsPerValue * length) / 64d);
            final long[] newLongs = new long[newLength];
            final double newSplitIndicesPerLong = 64D / newBitsPerValue;
            for (int i = 0; i < length; i++) {
                int value = getRaw(i);
                if (value > newMaxValidValue) {
                    throw new IllegalArgumentException(
                            "newBitsPerValue is too small to hold existing value " + value + valueOffset);
                }
                setSplitIndices(i, value, newSplitIndicesPerLong, newBitsPerValue, newLongs);
            }
            noSplitIndicesPerLong = 0;
            splitIndicesPerLong = newSplitIndicesPerLong;
            packedBits = newLongs;
        }
        bitsPerValue = newBitsPerValue;
        packingStrategy = newPackingStrategy;
        packedBitsTag.setValue(packedBits);
        currentMaxPackableValue = (1 << newBitsPerValue) - 1;
    }

    /** replace i to j bits in n with j - i bits of m */
    static long updateBits(long n, long m, int i, int j) {
        // updateBits(longs[longIndex], value, startBit, startBit + bits)
        long mShifted = i > 0 ? (m & ((1L << j - i) - 1)) << i : (m & ((1L << j - i) - 1)) >>> -i;
        return ((n & ((j > 63 ? 0 : (~0L << j)) | (i < 0 ? 0 : ((1L << i) - 1L)))) | mShifted);
    }

    static long bitRange(long value, int from, int to) {
        // bitRange(longs[longIndex], startBit, startBit + bits)
        int waste = 64 - to;
        return (value << waste) >>> (waste + from);
    }

    /**
     * Calculates the number of bits required to store the given num.
     * <ul>
     *     <li>Ex. num = 7 -> 3 (2^3 = 8)</li>
     *     <li>Ex. num = 2 -> 2</li>
     * </ul>
     *
     * If num is 0 then 0 is returned.
     */
    public static int calculateBitsRequired(int num) {
        if (num < 0) throw new IllegalArgumentException();
        return 32 - Integer.numberOfLeadingZeros(num);
    }

    @Override
    public ListIterator<Integer> iterator() {
        return new IteratorImpl();
    }

    private class IteratorImpl implements ListIterator<Integer> {
        int lastYieldedIndex = -1;
        int i = 0;

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return i < length;
        }

        /** {@inheritDoc} */
        @Override
        public Integer next() {
            if (!hasNext())
                throw new NoSuchElementException();
            return get(lastYieldedIndex = i++);
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasPrevious() {
            return i != 0;
        }

        /** {@inheritDoc} */
        @Override
        public Integer previous() {
            if (!hasPrevious())
                throw new NoSuchElementException();
            return get(lastYieldedIndex = --i);
        }

        /** {@inheritDoc} */
        @Override
        public int nextIndex() {
            return i;
        }

        /** {@inheritDoc} */
        @Override
        public int previousIndex() {
            return i - 1;
        }

        /**
         * Replaces the last element returned by {@link #next} or
         * {@link #previous} with the specified element.
         */
        @Override
        public void set(Integer value) {
            if (lastYieldedIndex < 0)
                throw new IllegalStateException();
            LongArrayTagPackedIntegers.this.set(lastYieldedIndex, value);
        }

        /** Unsupported */
        @Override
        public void add(Integer unsupported) {
            throw new UnsupportedOperationException();
        }

        /** Unsupported */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
