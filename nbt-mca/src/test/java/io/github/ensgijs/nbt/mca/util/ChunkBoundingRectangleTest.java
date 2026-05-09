package io.github.ensgijs.nbt.mca.util;


import junit.framework.TestCase;

import java.util.List;

import static io.github.ensgijs.nbt.mca.util.IntPointXZ.XZ;
import static org.junit.Assert.assertThrows;

public class ChunkBoundingRectangleTest extends TestCase {

    public void testRelocate_int() {
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(1, -1);  // [16..32), [-16..0)
        assertEquals(16, cbr.getMinBlockX());
        assertEquals(-16, cbr.getMinBlockZ());
        assertEquals(32, cbr.getMaxBlockX());
        assertEquals(0, cbr.getMaxBlockZ());

        assertEquals(1, cbr.getWidthChunkXZ());
        assertEquals(16, cbr.getWidthBlockXZ());

        assertEquals(16, cbr.relocateX(0));
        assertEquals(16 + 3, cbr.relocateX(3));
        assertEquals(16 + 7, cbr.relocateX(16 + 7));
        assertEquals(16 + 15, cbr.relocateX(-1));

        assertEquals(-1, cbr.relocateZ(16 * 53 - 1));
        assertEquals(-15, cbr.relocateZ(16 * -53 - 15));
        assertEquals(-16 + 3, cbr.relocateZ(3));
        assertEquals(-16 + 7, cbr.relocateZ(16 + 7));
        assertEquals(-16 + 15, cbr.relocateZ(-1));

        cbr = new ChunkBoundingRectangle(0, 0, 32);
        assertEquals(0, cbr.getMinBlockX());
        assertEquals(0, cbr.getMinBlockZ());
        assertEquals(512, cbr.getMaxBlockX());
        assertEquals(512, cbr.getMaxBlockZ());
        assertEquals(32, cbr.getWidthChunkXZ());
        assertEquals(512, cbr.getWidthBlockXZ());
    }

    public void testRelocate_double() {
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(0, 1);  // [0..16), [16..32)

        assertEquals(16 - 1e-6, cbr.relocateX(-1e-6), 1e-10);
        assertEquals(16 - 0.5, cbr.relocateX(41 * 16 -0.5), 1e-10);
        assertEquals(16 - 0.5, cbr.relocateX(-41 * 16 -0.5), 1e-10);

        assertEquals(16 + 1e-6, cbr.relocateZ(1e-6), 1e-10);
        assertEquals(16 + 1e-6, cbr.relocateZ(41 * 16 + 1e-6), 1e-10);
        assertEquals(16 + 0.5, cbr.relocateZ(41 * 16 + 0.5), 1e-10);
        assertEquals(16 + 6.789, cbr.relocateZ(-41 * 16 + 6.789), 1e-10);
    }

    public void testContainsChunk() {
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(0, 0, 5);  // [512..1024), [-512..0) | [32..64), [-32..0)
        assertTrue(cbr.containsChunk(0, 0));
        assertTrue(cbr.containsChunk(4, 4));
        assertFalse(cbr.containsChunk(-1, 2));
        assertFalse(cbr.containsChunk(2, 5));
    }

    public void testAsBlockBounds() {
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(4887, 6639);
        assertEquals(16, cbr.getWidthBlockXZ());
        assertEquals(4887 * 16, cbr.getMinBlockX());
        assertEquals(6639 * 16, cbr.getMinBlockZ());
        assertEquals(4888 * 16, cbr.getMaxBlockX());
        assertEquals(6640 * 16, cbr.getMaxBlockZ());

        BlockAlignedBoundingRectangle bbr = cbr.asBlockBounds();
        assertEquals(16, bbr.getWidthBlockXZ());
        assertEquals(4887 * 16, bbr.getMinBlockX());
        assertEquals(6639 * 16, bbr.getMinBlockZ());
        assertEquals(4888 * 16, bbr.getMaxBlockX());
        assertEquals(6640 * 16, bbr.getMaxBlockZ());
    }

    public void testOf() {
        ChunkBoundingRectangle br = ChunkBoundingRectangle.of(List.of(
                XZ(-5, 4)
        ));
        assertEquals(new ChunkBoundingRectangle(-5, 4, 1), br);

        br = ChunkBoundingRectangle.of(List.of(
                XZ(-5, 4),
                XZ(-4, -1)
        ));
        assertEquals(new ChunkBoundingRectangle(-5, -1, 6), br);
    }

    public void testGrow() {
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(3, 4, 5);
        ChunkBoundingRectangle cbr2 = cbr.growChunks(2);
        assertEquals(new ChunkBoundingRectangle(3 - 2, 4 - 2, 5 + 2 * 2), cbr2);
        assertEquals(cbr.getCenterX(), cbr2.getCenterX(), 1e-6);
        assertEquals(cbr.getCenterZ(), cbr2.getCenterZ(), 1e-6);

        assertThrows(IllegalArgumentException.class, () -> cbr2.growChunks(-1));
    }

    public void testShrink() {
        ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(3, 4, 5);
        ChunkBoundingRectangle cbr2 = cbr.shrinkChunks(2);
        assertEquals(new ChunkBoundingRectangle(3 + 2, 4 + 2, 5 - 2 * 2), cbr2);
        assertEquals(cbr.getCenterX(), cbr2.getCenterX(), 1e-6);
        assertEquals(cbr.getCenterZ(), cbr2.getCenterZ(), 1e-6);

        assertThrows(IllegalArgumentException.class, () -> cbr2.shrinkChunks(-1));
        assertNull(cbr2.shrinkChunks(1));
    }
}
