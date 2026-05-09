package io.github.ensgijs.nbt.mca.util;

/**
 * An immutable Minecraft style 3D point.
 * <p>Because this class is immutable (x, y, and z cannot be changed), you don't need to worry about
 * cloning it or copying it - if you need the same value somewhere else, just use the same instance!</p>
 * <p>Use the shorthand way of creating an instance by staticly importing the {@link #XYZ(int, int, int)} method.<br>
 * <pre>{@code import static IntPointXYZ.XYZ;
 * IntPointXYZ xyz = XYZ(1, 2, 3);}</pre>
 */
public class IntPointXYZ extends IntPointXZ {
    public static final IntPointXYZ ZERO_XYZ = new IntPointXYZ(0, 0, 0);
    protected final int y;

    /** Shorthand way of constructing a new  {@link IntPointXYZ}. */
    public static IntPointXYZ XYZ(int x, int y, int z) {
        return new IntPointXYZ(x, y, z);
    }

    public IntPointXYZ(int x, int y, int z) {
        super(x, z);
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public IntPointXYZ multiply(int multiplier) {
        return new IntPointXYZ(x * multiplier, y * multiplier, z * multiplier);
    }

    public IntPointXYZ multiply(IntPointXZ multiplier) {
        return new IntPointXYZ(x * multiplier.x, y, z * multiplier.z);
    }

    public IntPointXYZ multiply(IntPointXYZ multiplier) {
        return new IntPointXYZ(x * multiplier.x, y * multiplier.y, z * multiplier.z);
    }

    public IntPointXYZ divide(int denominator) {
        return new IntPointXYZ(x / denominator, y / denominator, z / denominator);
    }

    public IntPointXYZ divide(IntPointXZ denominator) {
        return new IntPointXYZ(x / denominator.x, y, z / denominator.z);
    }

    public IntPointXYZ divide(IntPointXYZ denominator) {
        return new IntPointXYZ(x / denominator.x, y / denominator.y, z / denominator.z);
    }

    public IntPointXYZ add(IntPointXZ other) {
        return new IntPointXYZ(x + other.x, y, z + other.z);
    }

    public IntPointXYZ add(IntPointXYZ other) {
        return new IntPointXYZ(x + other.x, y + other.y, z + other.z);
    }

    public IntPointXYZ add(int x, int z) {
        return new IntPointXYZ(this.x + x, y, this.z + z);
    }

    public IntPointXYZ add(int x, int y, int z) {
        return new IntPointXYZ(this.x + x, this.y + y, this.z + z);
    }

    public IntPointXYZ subtract(IntPointXZ other) {
        return new IntPointXYZ(x - other.x, y, z - other.z);
    }

    public IntPointXYZ subtract(IntPointXYZ other) {
        return new IntPointXYZ(x - other.x, y - other.y, z - other.z);
    }

    public IntPointXYZ subtract(int x, int z) {
        return new IntPointXYZ(this.x - x, y, this.z - z);
    }

    public IntPointXYZ subtract(int x, int y, int z) {
        return new IntPointXYZ(this.x - x, this.y - y, this.z - z);
    }

    public IntPointXYZ transformBlockToChunkSection() {
        return new IntPointXYZ(x >> 4, y >> 4, z >> 4);
    }

    public IntPointXYZ transformChunkSectionToBlock() {
        return new IntPointXYZ(x << 4, y << 4, z << 4);
    }

    @Override
    public int hashCode() {
        return (y * 31 + x) * 31 + z;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IntPointXYZ)) return false;
        final IntPointXYZ o = (IntPointXYZ) other;
        return this.x == o.x && this.y == o.y && this.z == o.z;
    }

    public boolean equals(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    @Override
    public String toString() {
        return x + " " + y + " " + z;
    }

    /**
     * @param format must contain exactly 3 {@code %d} placeholders.
     */
    public String toString(String format) {
        return String.format(format, x, y, z);
    }

    @Override
    public boolean isZero() {
        return super.isZero() && y == 0;
    }
}
