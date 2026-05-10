package io.github.ensgijs.nbt.mca.util;

import junit.framework.TestCase;

public class VersionAwareTest extends TestCase {

    public void testSanity() {
        VersionAware<String> va = new VersionAware<>();
        assertNull(va.get(0));
        va.register(0, "Zero");
        assertEquals("Zero", va.get(0));
        assertEquals("Zero", va.get(Integer.MAX_VALUE));
        assertNull(va.get(-1));

        va.register(10, "Ten");
        va.register(100, "Hundred");

        assertEquals("Zero", va.get(0));
        assertEquals("Zero", va.get(1));
        assertEquals("Zero", va.get(9));
        assertEquals("Ten", va.get(10));
        assertEquals("Ten", va.get(11));
        assertEquals("Ten", va.get(99));
        assertEquals("Hundred", va.get(100));
        assertEquals("Hundred", va.get(Integer.MAX_VALUE));
    }
}
