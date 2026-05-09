package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.tag.*;
import org.junit.Assert;

import java.util.Arrays;

public class TextNbtParserTest extends NbtTestCase {

	public void testParseTags() {
		Tag<?> t = assertThrowsNoException(() -> new TextNbtParser("{abc: def, blah: 4b, blubb: \"string\", \"foo\": 2s}").parse());
		assertEquals(CompoundTag.class, t.getClass());
		CompoundTag c = (CompoundTag) t;
		assertEquals(4, c.size());
		assertEquals("def", c.getString("abc"));
		assertEquals((byte) 4, c.getByte("blah"));
		assertEquals("string", c.getString("blubb"));
		assertEquals((short) 2, c.getShort("foo"));
		assertFalse(c.containsKey("invalid"));

		// ------------------------------------------------- number tags

		Tag<?> tb = assertThrowsNoException(() -> new TextNbtParser("16b").parse());
		assertEquals(ByteTag.class, tb.getClass());
		assertEquals((byte) 16, ((ByteTag) tb).asByte());

		tb = assertThrowsNoException(() -> new TextNbtParser("16B").parse());
		assertEquals(ByteTag.class, tb.getClass());
		assertEquals((byte) 16, ((ByteTag) tb).asByte());

		assertThrowsException((() -> new TextNbtParser("-129b").parse()), ParseException.class);

		Tag<?> ts = assertThrowsNoException(() -> new TextNbtParser("17s").parse());
		assertEquals(ShortTag.class, ts.getClass());
		assertEquals((short) 17, ((ShortTag) ts).asShort());

		ts = assertThrowsNoException(() -> new TextNbtParser("17S").parse());
		assertEquals(ShortTag.class, ts.getClass());
		assertEquals((short) 17, ((ShortTag) ts).asShort());

		assertThrowsException((() -> new TextNbtParser("-32769s").parse()), ParseException.class);

		Tag<?> ti = assertThrowsNoException(() -> new TextNbtParser("18").parse());
		assertEquals(IntTag.class, ti.getClass());
		assertEquals(18, ((IntTag) ti).asInt());

		assertThrowsException((() -> new TextNbtParser("-2147483649").parse()), ParseException.class);

		Tag<?> tl = assertThrowsNoException(() -> new TextNbtParser("19l").parse());
		assertEquals(LongTag.class, tl.getClass());
		assertEquals(19L, ((LongTag) tl).asLong());

		tl = assertThrowsNoException(() -> new TextNbtParser("19L").parse());
		assertEquals(LongTag.class, tl.getClass());
		assertEquals(19L, ((LongTag) tl).asLong());

		assertThrowsException((() -> new TextNbtParser("-9223372036854775809l").parse()), ParseException.class);

		Tag<?> tf = assertThrowsNoException(() -> new TextNbtParser("20.3f").parse());
		assertEquals(FloatTag.class, tf.getClass());
		assertEquals(20.3f, ((FloatTag) tf).asFloat());

		tf = assertThrowsNoException(() -> new TextNbtParser("20.3F").parse());
		assertEquals(FloatTag.class, tf.getClass());
		assertEquals(20.3f, ((FloatTag) tf).asFloat());

		Tag<?> td = assertThrowsNoException(() -> new TextNbtParser("21.3d").parse());
		assertEquals(DoubleTag.class, td.getClass());
		assertEquals(21.3d, ((DoubleTag) td).asDouble());

		td = assertThrowsNoException(() -> new TextNbtParser("21.3D").parse());
		assertEquals(DoubleTag.class, td.getClass());
		assertEquals(21.3d, ((DoubleTag) td).asDouble());

		td = assertThrowsNoException(() -> new TextNbtParser("21.3").parse());
		assertEquals(DoubleTag.class, td.getClass());
		assertEquals(21.3d, ((DoubleTag) td).asDouble());

		Tag<?> tbo = assertThrowsNoException(() -> new TextNbtParser("true").parse());
		assertEquals(ByteTag.class, tbo.getClass());
		assertEquals((byte) 1, ((ByteTag) tbo).asByte());

		tbo = assertThrowsNoException(() -> new TextNbtParser("false").parse());
		assertEquals(ByteTag.class, tbo.getClass());
		assertEquals((byte) 0, ((ByteTag) tbo).asByte());

		// ------------------------------------------------- arrays

		Tag<?> ba = assertThrowsNoException(() -> new TextNbtParser("[B; -128,0, 127]").parse());
		assertEquals(ByteArrayTag.class, ba.getClass());
		assertEquals(3, ((ByteArrayTag) ba).length());
		assertTrue(Arrays.equals(new byte[]{-128, 0, 127}, ((ByteArrayTag) ba).getValue()));

		Tag<?> ia = assertThrowsNoException(() -> new TextNbtParser("[I; -2147483648, 0,2147483647]").parse());
		assertEquals(IntArrayTag.class, ia.getClass());
		assertEquals(3, ((IntArrayTag) ia).length());
		assertTrue(Arrays.equals(new int[]{-2147483648, 0, 2147483647}, ((IntArrayTag) ia).getValue()));

		Tag<?> la = assertThrowsNoException(() -> new TextNbtParser("[L; -9223372036854775808, 0, 9223372036854775807 ]").parse());
		assertEquals(LongArrayTag.class, la.getClass());
		assertEquals(3, ((LongArrayTag) la).length());
		assertTrue(Arrays.equals(new long[]{-9223372036854775808L, 0, 9223372036854775807L}, ((LongArrayTag) la).getValue()));

		// ------------------------------------------------- invalid arrays

		assertThrowsException((() -> new TextNbtParser("[B; -129]").parse()), ParseException.class);
		assertThrowsException((() -> new TextNbtParser("[I; -2147483649]").parse()), ParseException.class);
		assertThrowsException((() -> new TextNbtParser("[L; -9223372036854775809]").parse()), ParseException.class);
		assertThrowsException((() -> new TextNbtParser("[B; 123b]").parse()), ParseException.class);
		assertThrowsException((() -> new TextNbtParser("[I; 123i]").parse()), ParseException.class);
		assertThrowsException((() -> new TextNbtParser("[L; 123l]").parse()), ParseException.class);
		assertThrowsException((() -> new TextNbtParser("[K; -129]").parse()), ParseException.class);

		// ------------------------------------------------- high level errors

		assertThrowsException(() -> new TextNbtParser("{20:10} {blah:blubb}").parse(), ParseException.class);

		// ------------------------------------------------- string tag

		Tag<?> st = assertThrowsNoException(() -> new TextNbtParser("abc").parse());
		assertEquals(StringTag.class, st.getClass());
		assertEquals("abc", ((StringTag) st).getValue());

		st = assertThrowsNoException(() -> new TextNbtParser("\"abc\"").parse());
		assertEquals(StringTag.class, st.getClass());
		assertEquals("abc", ((StringTag) st).getValue());

		st = assertThrowsNoException(() -> new TextNbtParser("123a").parse());
		assertEquals(StringTag.class, st.getClass());
		assertEquals("123a", ((StringTag) st).getValue());

		// ------------------------------------------------- list tag

		Tag<?> lt = assertThrowsNoException(() -> new TextNbtParser("[abc, \"def\", \"123\" ]").parse());
		assertEquals(ListTag.class, lt.getClass());
		assertEquals(StringTag.class, ((ListTag<?>) lt).getTypeClass());
		assertEquals(3, ((ListTag<?>) lt).size());
		assertEquals("abc", ((ListTag<?>) lt).asStringTagList().get(0).getValue());
		assertEquals("def", ((ListTag<?>) lt).asStringTagList().get(1).getValue());
		assertEquals("123", ((ListTag<?>) lt).asStringTagList().get(2).getValue());

		assertThrowsException(() -> new TextNbtParser("[123, 456").parse(), ParseException.class);
		assertThrowsException(() -> new TextNbtParser("[123, 456d]").parse(), ParseException.class);

		// ------------------------------------------------- compound tag

		Tag<?> ct = assertThrowsNoException(() -> new TextNbtParser("{abc: def,\"key\": 123d, blah: [L;123, 456], blubb: [123, 456]}").parse());
		assertEquals(CompoundTag.class, ct.getClass());
		assertEquals(4, ((CompoundTag) ct).size());
		assertEquals("def", assertThrowsNoException(() -> ((CompoundTag) ct).getString("abc")));
		assertEquals(123D, assertThrowsNoException(() -> ((CompoundTag) ct).getDouble("key")));
		assertTrue(Arrays.equals(new long[]{123, 456}, assertThrowsNoException(() -> ((CompoundTag) ct).getLongArray("blah"))));
		assertEquals(2, assertThrowsNoException(() -> ((CompoundTag) ct).getListTag("blubb")).size());
		assertEquals(IntTag.class, ((CompoundTag) ct).getListTag("blubb").getTypeClass());

		assertThrowsException(() -> new TextNbtParser("{abc: def").parse(), ParseException.class);
		assertThrowsException(() -> new TextNbtParser("{\"\":empty}").parse(), ParseException.class);
		assertThrowsException(() -> new TextNbtParser("{empty:}").parse(), ParseException.class);
	}


	public void testReadNamedTag() {
		// name followed by number
		// literal name
		NamedTag namedTag = assertThrowsNoException(() -> new TextNbtParser("my-value:16b").readTag(99));
		assertEquals("my-value", namedTag.getName());
		assertEquals(new ByteTag((byte) 16), namedTag.getTag());

		// numbers can be names
		namedTag = assertThrowsNoException(() -> new TextNbtParser("10: ten").readTag(99));
		assertEquals("10", namedTag.getName());
		assertEquals("ten", ((StringTag) namedTag.getTag()).getValue());

		// double literals can be names
		namedTag = assertThrowsNoException(() -> new TextNbtParser("1.6: one-point-six").readTag(99));
		assertEquals("1.6", namedTag.getName());
		assertEquals("one-point-six", ((StringTag) namedTag.getTag()).getValue());

		// name followed by bool
		// quoted name with space
		namedTag = assertThrowsNoException(() -> new TextNbtParser("\"some bool\":true").readTag(99));
		assertEquals("some bool", namedTag.getName());
		assertTrue(((ByteTag) namedTag.getTag()).asBoolean());

		// quoted name followed by quoted string
		namedTag = assertThrowsNoException(() -> new TextNbtParser("\"me key\": \"me value\"").readTag(99));
		assertEquals("me key", namedTag.getName());
		assertEquals("me value", ((StringTag) namedTag.getTag()).getValue());

		// name followed by long array
		// literal name containing a dot
		// whitespace around ':'
		namedTag = assertThrowsNoException(() -> new TextNbtParser("mod.params : [L; -9223372036854775808, 0, 9223372036854775807 ]").readTag(99));
		assertEquals("mod.params", namedTag.getName());
		assertEquals(LongArrayTag.class, namedTag.getTag().getClass());
		assertEquals(3, ((LongArrayTag) namedTag.getTag()).length());
		Assert.assertArrayEquals(new long[]{-9223372036854775808L, 0, 9223372036854775807L}, ((LongArrayTag) namedTag.getTag()).getValue());

		// name followed by string array
		namedTag = assertThrowsNoException(() -> new TextNbtParser("my_array: [abc, \"def\", \"123\" ]").readTag(99));
		assertEquals("my_array", namedTag.getName());
		assertEquals(ListTag.class, namedTag.getTag().getClass());
		assertEquals(StringTag.class, ((ListTag<?>) namedTag.getTag()).getTypeClass());
		assertEquals(3, ((ListTag<?>) namedTag.getTag()).size());
		assertEquals("abc", ((ListTag<?>) namedTag.getTag()).asStringTagList().get(0).getValue());
		assertEquals("def", ((ListTag<?>) namedTag.getTag()).asStringTagList().get(1).getValue());
		assertEquals("123", ((ListTag<?>) namedTag.getTag()).asStringTagList().get(2).getValue());

		// name followed by empty list tag
		namedTag = assertThrowsNoException(() -> new TextNbtParser("my-array:[]").readTag(99));
		assertEquals("my-array", namedTag.getName());
		assertEquals(ListTag.class, namedTag.getTag().getClass());
		assertTrue(((ListTag<?>) namedTag.getTag()).isEmpty());

		// name followed by compound tag
		NamedTag namedTagC = assertThrowsNoException(() -> new TextNbtParser("my-object: {abc: def,\"key\": 123d, blah: [L;123, 456], blubb: [123, 456]}").readTag(99));
		assertEquals(CompoundTag.class, namedTagC.getTag().getClass());
		assertEquals(4, ((CompoundTag) namedTagC.getTag()).size());
		assertEquals("def", assertThrowsNoException(() -> ((CompoundTag) namedTagC.getTag()).getString("abc")));
		assertEquals(123D, assertThrowsNoException(() -> ((CompoundTag) namedTagC.getTag()).getDouble("key")));
		Assert.assertArrayEquals(new long[]{123, 456}, assertThrowsNoException(() -> ((CompoundTag) namedTagC.getTag()).getLongArray("blah")));
		assertEquals(2, assertThrowsNoException(() -> ((CompoundTag) namedTagC.getTag()).getListTag("blubb")).size());
		assertEquals(IntTag.class, ((CompoundTag) namedTagC.getTag()).getListTag("blubb").getTypeClass());

		// name followed by empty compound tag
		namedTag = assertThrowsNoException(() -> new TextNbtParser("my-object:{}").readTag(99));
		assertEquals("my-object", namedTag.getName());
		assertEquals(CompoundTag.class, namedTag.getTag().getClass());
		assertTrue(((CompoundTag) namedTag.getTag()).isEmpty());

		// unnamed string literal
		namedTag = assertThrowsNoException(() -> new TextNbtParser("my-value.xyz").readTag(99));
		assertNull(namedTag.getName());
		assertEquals("my-value.xyz", ((StringTag) namedTag.getTag()).getValue());

		// unnamed quoted string
		namedTag = assertThrowsNoException(() -> new TextNbtParser("\"mystring\"").readTag(99));
		assertNull(namedTag.getName());
		assertEquals("mystring", ((StringTag) namedTag.getTag()).getValue());

		// unnamed float literal
		namedTag = assertThrowsNoException(() -> new TextNbtParser("42.5f").readTag(99));
		assertNull(namedTag.getName());
		assertEquals(42.5f, ((FloatTag) namedTag.getTag()).asFloat());

		// empty quoted string name
		namedTag = assertThrowsNoException(() -> new TextNbtParser("\"\":\nnothing").readTag(99));
		assertEquals("", namedTag.getName());
		assertEquals("nothing", ((StringTag) namedTag.getTag()).getValue());

		// no value after name
		assertThrowsException(() -> new TextNbtParser("oof: \n").readTag(99), ParseException.class);
	}

	public void testTrueFalseStrings_shouldRemainStrings() {
		NamedTag namedTag = assertThrowsNoException(() -> new TextNbtParser("\"not-a-bool\":\"true\"").readTag(99));
		assertEquals("not-a-bool", namedTag.getName());
		assertEquals(StringTag.ID, namedTag.getTag().getID());
		assertEquals("true", ((StringTag) namedTag.getTag()).getValue());

		namedTag = assertThrowsNoException(() -> new TextNbtParser("\"not-a-bool\":\"false\"").readTag(99));
		assertEquals("not-a-bool", namedTag.getName());
		assertEquals(StringTag.ID, namedTag.getTag().getID());
		assertEquals("false", ((StringTag) namedTag.getTag()).getValue());
	}
}
