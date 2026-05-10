package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.ByteTag;
import io.github.ensgijs.nbt.tag.ShortTag;
import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.tag.StringTag;

public class NamedTagTest extends NbtTestCase {

	public void testCreate() {
		ByteTag t = new ByteTag();
		NamedTag n = new NamedTag("name", t);
		assertEquals("name", n.getName());
		assertTrue(n.getTag() == t);
	}

	public void testSet() {
		ByteTag t = new ByteTag();
		NamedTag n = new NamedTag("name", t);
		n.setName("blah");
		assertEquals("blah", n.getName());
		ShortTag s = new ShortTag();
		n.setTag(s);
		assertTrue(n.getTag() == s);
	}

	public void testGetEscapedName() {
		NamedTag n = new NamedTag(".Level.TileTicks", new StringTag());
		assertEquals("\".Level.TileTicks\"", n.getEscapedName());
	}

	public void testCompare_integerOrder() {
		NamedTag nt2 = new NamedTag("2", new StringTag());
		NamedTag nt10 = new NamedTag("10", new StringTag());
		assertEquals(-1, NamedTag.compare(nt2, nt10));
		assertEquals(1, NamedTag.compare(nt10, nt2));
	}
}
