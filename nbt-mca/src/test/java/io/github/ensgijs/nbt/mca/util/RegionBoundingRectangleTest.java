package io.github.ensgijs.nbt.mca.util;

import junit.framework.TestCase;

import java.util.List;

import static io.github.ensgijs.nbt.mca.util.IntPointXZ.XZ;
import static org.junit.Assert.assertThrows;

public class RegionBoundingRectangleTest extends TestCase {

    public void testContainsBlock() {
        RegionBoundingRectangle rbr = new RegionBoundingRectangle(1, -1);  // [512..1024), [-512..0) | [32..64), [-32..0)

        assertEquals(512, rbr.getMinBlockX());
        assertEquals(-512, rbr.getMinBlockZ());
        assertEquals(1024, rbr.getMaxBlockX());
        assertEquals(0, rbr.getMaxBlockZ());
        assertEquals(1, rbr.getWidthRegionXZ());
        assertEquals(32, rbr.getWidthChunkXZ());
        assertEquals(512, rbr.getWidthBlockXZ());

        assertTrue(rbr.containsBlock(512, -512));
        assertTrue(rbr.containsBlock(1023, -1));
        assertFalse(rbr.containsBlock(511, -512));
        assertFalse(rbr.containsBlock(1023, 0));

        rbr = new RegionBoundingRectangle(0, 0, 10);  // [512..1024), [-512..0) | [32..64), [-32..0)

        assertEquals(0, rbr.getMinBlockX());
        assertEquals(0, rbr.getMinBlockZ());
        assertEquals(5120, rbr.getMaxBlockX());
        assertEquals(5120, rbr.getMaxBlockZ());
        assertEquals(10, rbr.getWidthRegionXZ());
        assertEquals(10 * 32, rbr.getWidthChunkXZ());
        assertEquals(10 * 512, rbr.getWidthBlockXZ());
    }

    public void testContainsChunk() {
        RegionBoundingRectangle rbr = new RegionBoundingRectangle(1, -1);  // [512..1024), [-512..0) | [32..64), [-32..0)
        assertTrue(rbr.containsChunk(32, -32));
        assertTrue(rbr.containsChunk(63, -1));
        assertFalse(rbr.containsChunk(31, -32));
        assertFalse(rbr.containsChunk(63, 0));
    }

    public void testContainsRegion() {
        RegionBoundingRectangle rbr = new RegionBoundingRectangle(0, 0, 5);  // [512..1024), [-512..0) | [32..64), [-32..0)
        assertTrue(rbr.containsRegion(0, 0));
        assertTrue(rbr.containsRegion(4, 4));
        assertFalse(rbr.containsRegion(-1, 2));
        assertFalse(rbr.containsRegion(2, 5));
    }

    public void testForChunk() {
        RegionBoundingRectangle rbr = RegionBoundingRectangle.forChunk(42, -7);
        assertEquals(1, rbr.getMinRegionX());
        assertEquals(-1, rbr.getMinRegionZ());
        assertEquals(1, rbr.getWidthRegionXZ());
        assertEquals(1024, rbr.getMaxBlockX());
        assertEquals(512, rbr.getMinBlockX());
        assertEquals(0, rbr.getMaxBlockZ());
        assertEquals(-512, rbr.getMinBlockZ());

        rbr = RegionBoundingRectangle.forChunk(32, -32);
        assertEquals(1, rbr.getMinRegionX());
        assertEquals(-1, rbr.getMinRegionZ());
        assertEquals(1, rbr.getWidthRegionXZ());

        rbr = RegionBoundingRectangle.forChunk(63, -1);
        assertEquals(1, rbr.getMinRegionX());
        assertEquals(-1, rbr.getMinRegionZ());
        assertEquals(1, rbr.getWidthRegionXZ());
    }

    public void testWORLD_REGION_BOUNDS() {
        assertEquals(-58594, RegionBoundingRectangle.MAX_WORLD_REGION_BOUNDS.getMinRegionX());
        assertEquals(-58594, RegionBoundingRectangle.MAX_WORLD_REGION_BOUNDS.getMinRegionZ());
        assertEquals(58594, RegionBoundingRectangle.MAX_WORLD_REGION_BOUNDS.getMaxRegionX());
        assertEquals(58594, RegionBoundingRectangle.MAX_WORLD_REGION_BOUNDS.getMaxRegionZ());
        assertTrue(RegionBoundingRectangle.MAX_WORLD_REGION_BOUNDS.containsRegion(-58594, -58594));
        assertTrue(RegionBoundingRectangle.MAX_WORLD_REGION_BOUNDS.containsRegion(58593, 58593));
        assertFalse(RegionBoundingRectangle.MAX_WORLD_REGION_BOUNDS.containsRegion(58594, 58594));
    }

    public void testAsChunkBounds() {
        RegionBoundingRectangle rbr = new RegionBoundingRectangle(4887, 6639);
        assertEquals(32, rbr.getWidthChunkXZ());
        assertEquals(512, rbr.getWidthBlockXZ());
        assertEquals(4887 * 32, rbr.getMinChunkX());
        assertEquals(6639 * 32, rbr.getMinChunkZ());
        assertEquals(4888 * 32, rbr.getMaxChunkX());
        assertEquals(6640 * 32, rbr.getMaxChunkZ());

        ChunkBoundingRectangle cbr = rbr.asChunkBounds();
        assertEquals(32, cbr.getWidthChunkXZ());
        assertEquals(512, cbr.getWidthBlockXZ());
        assertEquals(4887 * 32, cbr.getMinChunkX());
        assertEquals(6639 * 32, cbr.getMinChunkZ());
        assertEquals(4888 * 32, cbr.getMaxChunkX());
        assertEquals(6640 * 32, cbr.getMaxChunkZ());
    }

    public void testAsBlockBounds() {
        RegionBoundingRectangle rbr = new RegionBoundingRectangle(4887, 6639);
        BlockAlignedBoundingRectangle bbr = rbr.asBlockBounds();
        assertEquals(512, bbr.getWidthBlockXZ());
        assertEquals(4887 * 32 * 16, bbr.getMinBlockX());
        assertEquals(6639 * 32 * 16, bbr.getMinBlockZ());
        assertEquals(4888 * 32 * 16, bbr.getMaxBlockX());
        assertEquals(6640 * 32 * 16, bbr.getMaxBlockZ());
    }

    public void testOf() {
        RegionBoundingRectangle br = RegionBoundingRectangle.of(List.of(
                XZ(-5, 4)
        ));
        assertEquals(new RegionBoundingRectangle(-5, 4, 1), br);

        br = RegionBoundingRectangle.of(List.of(
                XZ(-5, 4),
                XZ(-4, -1)
        ));
        assertEquals(new RegionBoundingRectangle(-5, -1, 6), br);
    }

    public void testGrow() {
        RegionBoundingRectangle cbr = new RegionBoundingRectangle(3, 4, 5);
        RegionBoundingRectangle cbr2 = cbr.growRegions(2);
        assertEquals(new RegionBoundingRectangle(3 - 2, 4 - 2, 5 + 2 * 2), cbr2);
        assertEquals(cbr.getCenterX(), cbr2.getCenterX(), 1e-6);
        assertEquals(cbr.getCenterZ(), cbr2.getCenterZ(), 1e-6);

        assertThrows(IllegalArgumentException.class, () -> cbr2.growRegions(-1));
    }

    public void testShrink() {
        RegionBoundingRectangle cbr = new RegionBoundingRectangle(3, 4, 5);
        RegionBoundingRectangle cbr2 = cbr.shrinkRegions(2);
        assertEquals(new RegionBoundingRectangle(3 + 2, 4 + 2, 5 - 2 * 2), cbr2);
        assertEquals(cbr.getCenterX(), cbr2.getCenterX(), 1e-6);
        assertEquals(cbr.getCenterZ(), cbr2.getCenterZ(), 1e-6);

        assertThrows(IllegalArgumentException.class, () -> cbr2.shrinkRegions(-1));
        assertNull(cbr2.shrinkRegions(1));
    }
}
