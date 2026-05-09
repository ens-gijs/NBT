package io.github.ensgijs.nbt.mca.util;

import junit.framework.TestCase;

public class IntPointXZTest extends TestCase {

    public void testConstructorAndGetters() {
        IntPointXZ xz = new IntPointXZ(42, -64);
        assertEquals(42, xz.getX());
        assertEquals(-64, xz.getZ());
    }

    public void testMultiplyInt() {
        IntPointXZ xz = new IntPointXZ(42, -64);
        xz = xz.multiply(8);
        assertEquals(42 * 8, xz.getX());
        assertEquals(-64 * 8, xz.getZ());
        xz = xz.multiply(-3);
        assertEquals(42 * 8 * -3, xz.getX());
        assertEquals(-64 * 8 * -3, xz.getZ());
    }

    public void testMultiplyIntPointXZ() {
        IntPointXZ xz = new IntPointXZ(42, -64);
        xz = xz.multiply(new IntPointXZ(-2, 4));
        assertEquals(42 * -2, xz.getX());
        assertEquals(-64 * 4, xz.getZ());
    }

    public void testDivideInt() {
        IntPointXZ xz = new IntPointXZ(42, -63);
        xz = xz.divide(8);
        assertEquals(42 / 8, xz.getX());
        assertEquals(-63 / 8, xz.getZ());
    }

    public void testDivideIntPointXZ() {
        IntPointXZ xz = new IntPointXZ(42, -63);
        xz = xz.divide(new IntPointXZ(2, 3));
        assertEquals(42 / 2, xz.getX());
        assertEquals(-63 / 3, xz.getZ());
    }

    public void testAddIntInt() {
        IntPointXZ xz = new IntPointXZ(42, -63);
        xz = xz.add(32, -16);
        assertEquals(42 + 32, xz.getX());
        assertEquals(-63 - 16, xz.getZ());
    }

    public void testAddIntPointXZ() {
        IntPointXZ xz = new IntPointXZ(42, -63);
        xz = xz.add(new IntPointXZ(14, 100));
        assertEquals(42 + 14, xz.getX());
        assertEquals(-63 + 100, xz.getZ());
    }

    public void testSubtractIntInt() {
        IntPointXZ xz = new IntPointXZ(42, -63);
        xz = xz.subtract(32, -16);
        assertEquals(42 - 32, xz.getX());
        assertEquals(-63 + 16, xz.getZ());
    }

    public void testSubtractIntPointXZ() {
        IntPointXZ xz = new IntPointXZ(42, -63);
        xz = xz.subtract(new IntPointXZ(14, 100));
        assertEquals(42 - 14, xz.getX());
        assertEquals(-63 - 100, xz.getZ());
    }

    public void testTransformBlockToChunk() {
        IntPointXZ xz = new IntPointXZ(42, -63);
        xz = xz.transformBlockToChunk();
        assertEquals(2, xz.getX());
        assertEquals(-4, xz.getZ());

        xz = new IntPointXZ(-1, 15);
        xz = xz.transformBlockToChunk();
        assertEquals(-1, xz.getX());
        assertEquals(0, xz.getZ());
    }

    public void testTransformChunkToBlock() {
        IntPointXZ xz = new IntPointXZ(2, -4);
        xz = xz.transformChunkToBlock();
        assertEquals(32, xz.getX());
        assertEquals(-64, xz.getZ());

        xz = new IntPointXZ(-1, 0);
        xz = xz.transformChunkToBlock();
        assertEquals(-16, xz.getX());
        assertEquals(0, xz.getZ());
    }

    public void testTransformChunkToRegion() {
        IntPointXZ xz = new IntPointXZ(0, 31);
        xz = xz.transformChunkToRegion();
        assertEquals(0, xz.getX());
        assertEquals(0, xz.getZ());

        xz = new IntPointXZ(-33, 97);
        xz = xz.transformChunkToRegion();
        assertEquals(-2, xz.getX());
        assertEquals(3, xz.getZ());
    }

    public void testTransformRegionToChunk() {
        IntPointXZ xz = new IntPointXZ(0, 1);
        xz = xz.transformRegionToChunk();
        assertEquals(0, xz.getX());
        assertEquals(32, xz.getZ());

        xz = new IntPointXZ(-1, -2);
        xz = xz.transformRegionToChunk();
        assertEquals(-32, xz.getX());
        assertEquals(-64, xz.getZ());
    }

    public void testTransformBlockToRegion() {
        IntPointXZ xz = new IntPointXZ(0, 512);
        xz = xz.transformBlockToRegion();
        assertEquals(0, xz.getX());
        assertEquals(1, xz.getZ());

        xz = new IntPointXZ(-513, 511);
        xz = xz.transformBlockToRegion();
        assertEquals(-2, xz.getX());
        assertEquals(0, xz.getZ());
    }

    public void testTransformRegionToBlock() {
        IntPointXZ xz = new IntPointXZ(1, -1);
        xz = xz.transformRegionToBlock();
        assertEquals(512, xz.getX());
        assertEquals(-512, xz.getZ());

        xz = new IntPointXZ(0, -2);
        xz = xz.transformRegionToBlock();
        assertEquals(0, xz.getX());
        assertEquals(-1024, xz.getZ());
    }

    public void testEquals() {
        IntPointXZ xz = new IntPointXZ(0, 0);
        assertTrue(xz.equals(xz));
        assertFalse(xz.equals(null));
        assertTrue(xz.equals(new IntPointXZ(0, 0)));
        assertTrue(new IntPointXZ(101, -97).equals(new IntPointXZ(101, -97)));
        assertFalse(new IntPointXZ(101, -97).equals(new IntPointXZ(-97, 101)));
    }

    public void testUnpackLong() {
        assertEquals(new IntPointXZ(-95, -85), IntPointXZ.unpack(-360777252959L));
        assertEquals(new IntPointXZ(-91, -87), IntPointXZ.unpack(-369367187547L));
    }

    public void testPackLong() {
        assertEquals(-360777252959L, IntPointXZ.pack(new IntPointXZ(-95, -85)));
    }
}
