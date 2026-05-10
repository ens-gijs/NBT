package io.github.ensgijs.nbt.mca.util;

/**
 * An immutable Minecraft style 2D point where the dimensions are X and Z.
 * <p>Because this class is immutable (x and z cannot be changed), you don't need to worry about
 * cloning it or copying it - if you need the same value somewhere else, just use the same instance!</p>
 */
public class IntPointXZ {
    public static final IntPointXZ ZERO_XZ = new IntPointXZ(0, 0);

    protected final int x;
    protected final int z;

    /** Shorthand way of constructing a new  {@link IntPointXZ}. */
    public static IntPointXZ XZ(int x, int z) {
        return new IntPointXZ(x, z);
    }

    public IntPointXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public IntPointXZ multiply(int multiplier) {
        return new IntPointXZ(x * multiplier, z * multiplier);
    }

    public IntPointXZ multiply(IntPointXZ multiplier) {
        return new IntPointXZ(x * multiplier.x, z * multiplier.z);
    }

    public IntPointXZ multiply(IntPointXYZ multiplier) {
        return new IntPointXZ(x * multiplier.x, z * multiplier.z);
    }

    public IntPointXZ divide(int denominator) {
        return new IntPointXZ(x / denominator, z / denominator);
    }

    public IntPointXZ divide(IntPointXZ denominator) {
        return new IntPointXZ(x / denominator.x, z / denominator.z);
    }

    public IntPointXZ divide(IntPointXYZ denominator) {
        return new IntPointXZ(x / denominator.x, z / denominator.z);
    }

    public IntPointXZ add(IntPointXZ other) {
        return new IntPointXZ(x + other.x, z + other.z);
    }

    public IntPointXZ add(IntPointXYZ other) {
        return new IntPointXZ(x + other.x, z + other.z);
    }

    public IntPointXZ add(int x, int z) {
        return new IntPointXZ(this.x + x, this.z + z);
    }

    public IntPointXZ subtract(IntPointXZ other) {
        return new IntPointXZ(x - other.x, z - other.z);
    }

    public IntPointXZ subtract(IntPointXYZ other) {
        return new IntPointXZ(x - other.x, z - other.z);
    }

    public IntPointXZ subtract(int x, int z) {
        return new IntPointXZ(this.x - x, this.z - z);
    }

    public IntPointXZ transformBlockToChunk() {
        return new IntPointXZ(x >> 4, z >> 4);
    }

    public IntPointXZ transformChunkToRegion() {
        return new IntPointXZ(x >> 5, z >> 5);
    }

    public IntPointXZ transformBlockToRegion() {
        return new IntPointXZ(x >> 9, z >> 9);
    }

    public IntPointXZ transformRegionToBlock() {
        return new IntPointXZ(x << 9, z << 9);
    }

    public IntPointXZ transformRegionToChunk() {
        return new IntPointXZ(x << 5, z << 5);
    }

    public IntPointXZ transformChunkToBlock() {
        return new IntPointXZ(x << 4, z << 4);
    }

    @Override
    public int hashCode() {
        return x * 31 + z;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IntPointXZ)) return false;
        final IntPointXZ o = (IntPointXZ) other;
        return this.x == o.x && this.z == o.z;
    }

    public boolean equals(int x, int z) {
        return this.x == x && this.z == z;
    }

    @Override
    public String toString() {
        return x + " " + z;
    }

    /**
     * @param format must contain exactly 2 {@code %d} placeholders.
     */
    public String toString(String format) {
        return String.format(format, x, z);
    }

    public static IntPointXZ unpack(long xzLong) {
        int x = (int) (xzLong & 0xFFFFFFFFL);
        int z = (int) ((xzLong >>> 32) & 0xFFFFFFFFL);
        return new IntPointXZ(x, z);
    }

    public static long pack(int x, int z) {
        return (((long) z) << 32) | ((long) x & 0xFFFFFFFFL);
    }

    public static long pack(IntPointXZ xz) {
        return pack(xz.x, xz.z);
    }

    public boolean isZero() {
        return x == 0 && z == 0;
    }
}
