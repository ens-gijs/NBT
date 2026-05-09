package io.github.ensgijs.nbt.mca.util;

import junit.framework.TestCase;

import java.util.List;

import static io.github.ensgijs.nbt.mca.util.IntPointXZ.XZ;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class BlockAlignedBoundingRectangleTest extends TestCase {

    public void testMinMaxXZ() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(16, -16, 16);
        assertEquals(16, cbr.getMinBlockX());
        assertEquals(32, cbr.getMaxBlockX());
        assertEquals(-16, cbr.getMinBlockZ());
        assertEquals(0, cbr.getMaxBlockZ());
    }

    public void testContains_int() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(16, -16, 16);
        assertTrue(cbr.containsBlock(16 + 8, -8));
        assertFalse(cbr.containsBlock(-8, 8));

        assertTrue(cbr.containsBlock(16, -1));
        assertFalse(cbr.containsBlock(15, -1));
        assertFalse(cbr.containsBlock(16, 0));

        assertTrue(cbr.containsBlock(16, -16));
        assertFalse(cbr.containsBlock(15, -16));
        assertFalse(cbr.containsBlock(16, -17));

        assertTrue(cbr.containsBlock(31, -1));
        assertFalse(cbr.containsBlock(32, -1));
        assertFalse(cbr.containsBlock(31, 0));

        assertTrue(cbr.containsBlock(31, -16));
        assertFalse(cbr.containsBlock(32, -16));
        assertFalse(cbr.containsBlock(31, -17));
    }

    public void testContains_IntPointXZ() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(0, 0, 16);
        assertTrue(cbr.containsBlock(new IntPointXZ(8, 8)));
        assertTrue(cbr.containsBlock(new IntPointXZ(0, 0)));
        assertTrue(cbr.containsBlock(new IntPointXZ(15, 15)));
        assertFalse(cbr.containsBlock(new IntPointXZ(-1, 7)));
        assertFalse(cbr.containsBlock(new IntPointXZ(16, 7)));
        assertFalse(cbr.containsBlock(new IntPointXZ(7, -1)));
        assertFalse(cbr.containsBlock(new IntPointXZ(7, 16)));
    }

    public void testContains_double() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(16, -16, 16);
        assertTrue(cbr.containsBlock(16 + 7.5, -7.5));
        assertFalse(cbr.containsBlock(-8.5, 8.5));

        assertTrue(cbr.containsBlock(16.0, -1e-14));
        assertTrue(cbr.containsBlock(16.0, -16.0));
        assertTrue(cbr.containsBlock(32 - 1e-14, -1e-14));
        assertTrue(cbr.containsBlock(32 - 1e-14, -16.0));

        assertFalse(cbr.containsBlock(16 - 1e-14, -7.5));  // off left
        assertFalse(cbr.containsBlock(32.0, -7.5));  // off right
        assertFalse(cbr.containsBlock(16 + 7.5, 0.0));  // off top
        assertFalse(cbr.containsBlock(16 + 7.5, -16.0 - 1e-14));  // off bottom
    }

    public void testConstrain() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(0, 0, 16);
        assertFalse(cbr.constrain(null));
        assertFalse(cbr.constrain(new int[] {1,2,3,4}));

        // edge to edge but all in bounds
        int[] bb = new int[] {0, 0, 0, 15, 99, 15};
        assertTrue(cbr.constrain(bb));
        assertArrayEquals(new int[] {0, 0, 0, 15, 99, 15}, bb);

        // 1x1 on max bounds
        bb = new int[] {15, 0, 15, 15, 99, 15};
        assertTrue(cbr.constrain(bb));
        assertArrayEquals(new int[] {15, 0, 15, 15, 99, 15}, bb);

        // bb encases cbr
        bb = new int[] {-8, 0, -8, 20, 99, 20};
        assertTrue(cbr.constrain(bb));
        assertArrayEquals(new int[] {0, 0, 0, 15, 99, 15}, bb);

        // bb outside cbr entirely
        assertFalse(cbr.constrain(new int[] {16, 0, 0, 20, 0, 0}));  // to the right
        assertFalse(cbr.constrain(new int[] {-5, 0, 0, -1, 0, 0}));  // to the left
        assertFalse(cbr.constrain(new int[] {0, 0, 16, 0, 0, 20}));  // above
        assertFalse(cbr.constrain(new int[] {0, 0, -5, 0, 0, -1}));  // below

        // bb has one corner in bounds
        bb = new int[] {-8, 0, -8, 8, 0, 8};
        assertTrue(cbr.constrain(bb));
        assertArrayEquals(new int[] {0, 0, 0, 8, 0, 8}, bb);

        bb = new int[] {8, 0, 8, 20, 0, 20};
        assertTrue(cbr.constrain(bb));
        assertArrayEquals(new int[] {8, 0, 8, 15, 0, 15}, bb);

        bb = new int[] {-8, 0, 8, 8, 0, 28};
        assertTrue(cbr.constrain(bb));
        assertArrayEquals(new int[] {0, 0, 8, 8, 0, 15}, bb);

        bb = new int[] {8, 0, -8, 20, 0, 8};
        assertTrue(cbr.constrain(bb));
        assertArrayEquals(new int[] {8, 0, 0, 15, 0, 8}, bb);

        // throws if bound min-max are out of order
        assertThrows(IllegalArgumentException.class, () -> cbr.constrain(new int[] {8, 0, 8, 4, 0, 4}));
        assertThrows(IllegalArgumentException.class, () -> cbr.constrain(new int[] {8, 0, 8, -4, 0, -4}));
    }

    public void testMAX_WORLD_BORDER_BOUNDS() {
        assertEquals(-1874999, ChunkBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.getMinChunkX());
        assertEquals(-1874999, ChunkBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.getMinChunkZ());
        assertEquals(1874999, ChunkBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.getMaxChunkX());
        assertEquals(1874999, ChunkBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.getMaxChunkZ());
        assertTrue(ChunkBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.containsChunk(-1874999, -1874999));
        assertTrue(ChunkBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.containsChunk(1874998, 1874998));
        assertFalse(ChunkBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.containsChunk(1874999, 1874999));
    }

    public void testOf() {
        BlockAlignedBoundingRectangle bbr = BlockAlignedBoundingRectangle.of(List.of(
                XZ(-5, 4)
        ));
        assertEquals(new BlockAlignedBoundingRectangle(-5, 4, 1), bbr);

        bbr = BlockAlignedBoundingRectangle.of(List.of(
                XZ(-5, 4),
                XZ(-4, -1)
        ));
        assertEquals(new BlockAlignedBoundingRectangle(-5, -1, 6), bbr);
    }

    public void testGrow() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(3, 4, 5);
        BlockAlignedBoundingRectangle cbr2 = cbr.grow(2);
        assertEquals(new BlockAlignedBoundingRectangle(3 - 2, 4 - 2, 5 + 2 * 2), cbr2);
        assertEquals(cbr.getCenterX(), cbr2.getCenterX(), 1e-6);
        assertEquals(cbr.getCenterZ(), cbr2.getCenterZ(), 1e-6);

        assertThrows(IllegalArgumentException.class, () -> cbr2.grow(-1));
    }

    public void testShrink() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(3, 4, 5);
        BlockAlignedBoundingRectangle cbr2 = cbr.shrink(2);
        assertEquals(new BlockAlignedBoundingRectangle(3 + 2, 4 + 2, 5 - 2 * 2), cbr2);
        assertEquals(cbr.getCenterX(), cbr2.getCenterX(), 1e-6);
        assertEquals(cbr.getCenterZ(), cbr2.getCenterZ(), 1e-6);

        assertThrows(IllegalArgumentException.class, () -> cbr2.shrink(-1));
        assertNull(cbr2.shrink(1));
    }
}
