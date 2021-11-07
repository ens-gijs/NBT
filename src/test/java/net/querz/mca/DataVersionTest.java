package net.querz.mca;


import junit.framework.TestCase;

import java.util.Arrays;

public class DataVersionTest extends TestCase {

    public void testBestForNegativeValue() {
        assertEquals(DataVersion.UNKNOWN, DataVersion.bestFor(-42));
    }

    public void testBestForExactFirst() {
        assertEquals(DataVersion.UNKNOWN, DataVersion.bestFor(0));
    }

    public void testBestForExactArbitrary() {
        assertEquals(DataVersion.JAVA_1_15_0, DataVersion.bestFor(2225));
    }

    public void testBestForBetween() {
        assertEquals(DataVersion.JAVA_1_10_2, DataVersion.bestFor(DataVersion.JAVA_1_11_0.id() - 1));
        assertEquals(DataVersion.JAVA_1_11_0, DataVersion.bestFor(DataVersion.JAVA_1_11_0.id() + 1));
    }

    public void testBestForExactLast() {
        final DataVersion last = DataVersion.values()[DataVersion.values().length - 1];
        assertEquals(last, DataVersion.bestFor(last.id()));
    }

    public void testBestForAfterLast() {
        final DataVersion last = DataVersion.values()[DataVersion.values().length - 1];
        assertEquals(last, DataVersion.bestFor(last.id() + 123));
    }

    public void testToString() {
        assertEquals("2724 (1.17.0)", DataVersion.JAVA_1_17_0.toString());
        assertEquals("UNKNOWN", DataVersion.UNKNOWN.toString());
    }
}
