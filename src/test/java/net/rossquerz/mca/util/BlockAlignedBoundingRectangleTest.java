package net.rossquerz.mca.util;

import junit.framework.TestCase;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class BlockAlignedBoundingRectangleTest extends TestCase {

    public void testMinMaxXZ() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(16, -16, 16);
        assertEquals(16, cbr.getMinX());
        assertEquals(32, cbr.getMaxX());
        assertEquals(-16, cbr.getMinZ());
        assertEquals(0, cbr.getMaxZ());
    }

    public void testContains_int() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(16, -16, 16);
        assertTrue(cbr.contains(16 + 8, -8));
        assertFalse(cbr.contains(-8, 8));

        assertTrue(cbr.contains(16, -1));
        assertFalse(cbr.contains(15, -1));
        assertFalse(cbr.contains(16, 0));

        assertTrue(cbr.contains(16, -16));
        assertFalse(cbr.contains(15, -16));
        assertFalse(cbr.contains(16, -17));

        assertTrue(cbr.contains(31, -1));
        assertFalse(cbr.contains(32, -1));
        assertFalse(cbr.contains(31, 0));

        assertTrue(cbr.contains(31, -16));
        assertFalse(cbr.contains(32, -16));
        assertFalse(cbr.contains(31, -17));
    }

    public void testContains_IntPointXZ() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(0, 0, 16);
        assertTrue(cbr.contains(new IntPointXZ(8, 8)));
        assertTrue(cbr.contains(new IntPointXZ(0, 0)));
        assertTrue(cbr.contains(new IntPointXZ(15, 15)));
        assertFalse(cbr.contains(new IntPointXZ(-1, 7)));
        assertFalse(cbr.contains(new IntPointXZ(16, 7)));
        assertFalse(cbr.contains(new IntPointXZ(7, -1)));
        assertFalse(cbr.contains(new IntPointXZ(7, 16)));
    }

    public void testContains_double() {
        BlockAlignedBoundingRectangle cbr = new BlockAlignedBoundingRectangle(16, -16, 16);
        assertTrue(cbr.contains(16 + 7.5, -7.5));
        assertFalse(cbr.contains(-8.5, 8.5));

        assertTrue(cbr.contains(16.0, -1e-14));
        assertTrue(cbr.contains(16.0, -16.0));
        assertTrue(cbr.contains(32 - 1e-14, -1e-14));
        assertTrue(cbr.contains(32 - 1e-14, -16.0));

        assertFalse(cbr.contains(16 - 1e-14, -7.5));  // off left
        assertFalse(cbr.contains(32.0, -7.5));  // off right
        assertFalse(cbr.contains(16 + 7.5, 0.0));  // off top
        assertFalse(cbr.contains(16 + 7.5, -16.0 - 1e-14));  // off bottom
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
}
