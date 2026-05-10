package io.github.ensgijs.nbt.io;

import junit.framework.TestCase;

public class CompressionTypeTest extends TestCase {

	public void testGetFromID() {
		assertEquals(CompressionType.NONE, CompressionType.getFromID(CompressionType.NONE.getID()));
		assertEquals(CompressionType.GZIP, CompressionType.getFromID(CompressionType.GZIP.getID()));
		assertEquals(CompressionType.ZLIB, CompressionType.getFromID(CompressionType.ZLIB.getID()));
		assertNull(CompressionType.getFromID((byte) -1));
	}

	public void testDetectCompressionType() {
		assertEquals(CompressionType.NONE, CompressionType.detect(null));
		assertEquals(CompressionType.NONE, CompressionType.detect(new byte[0]));
		assertEquals(CompressionType.NONE, CompressionType.detect(new byte[]{0x1f}));

		assertEquals(CompressionType.GZIP, CompressionType.detect(new byte[]{0x1f, (byte) 0x8b}));
		assertEquals(CompressionType.GZIP, CompressionType.detect(new byte[]{0x1f, (byte) 0x8b, 0x08}));
		assertEquals(CompressionType.NONE, CompressionType.detect(new byte[]{0x1f, (byte) 0xb8, 0x08}));

		assertEquals(CompressionType.NONE, CompressionType.detect(new byte[]{0x78}));
		assertEquals(CompressionType.ZLIB, CompressionType.detect(new byte[]{0x78, (byte) 0x9C}));
		assertEquals(CompressionType.ZLIB, CompressionType.detect(new byte[]{0x78, (byte) 0x9C, 0x08}));
		assertEquals(CompressionType.NONE, CompressionType.detect(new byte[]{0x78, (byte) 0xc9, 0x08}));
	}
}
