package io.github.ensgijs.nbt.tag;

import io.github.ensgijs.nbt.NbtTestCase;

public class EndTagTest extends NbtTestCase {

	public void testStringConversion() {
		EndTag e = EndTag.INSTANCE;
		assertEquals(0, e.getID());
		assertNull(e.getValue());
		assertEquals("{\"type\":\"" + e.getClass().getSimpleName() + "\",\"value\":\"end\"}", e.toString());
	}

	public void testClone() {
		assertTrue(EndTag.INSTANCE == EndTag.INSTANCE.clone());
	}
}
