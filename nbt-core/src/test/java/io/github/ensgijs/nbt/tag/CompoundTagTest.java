package io.github.ensgijs.nbt.tag;

import io.github.ensgijs.nbt.io.MaxDepthReachedException;
import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.NbtTestCase;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

public class CompoundTagTest extends NbtTestCase {

	private CompoundTag createCompoundTag() {
		CompoundTag ct = new CompoundTag();
		invokeSetValue(ct, new LinkedHashMap<>());
		ct.put("b", new ByteTag(Byte.MAX_VALUE));
		ct.put("str", new StringTag("foo"));
		ct.put("list", new ListTag<>(ByteTag.class));
		ct.getListTag("list").addByte((byte) 123);
		return ct;
	}

	public void testStringConversion() {
		CompoundTag ct = createCompoundTag();
		assertEquals("{\"type\":\"CompoundTag\"," +
				"\"value\":{" +
				"\"b\":{\"type\":\"ByteTag\",\"value\":127}," +
				"\"list\":{\"type\":\"ListTag\",\"value\":{\"type\":\"ByteTag\",\"list\":[123]}}," +
				"\"str\":{\"type\":\"StringTag\",\"value\":\"foo\"}" +
				"}}", ct.toString());
	}

	public void testEquals() {
		CompoundTag ct = createCompoundTag();
		CompoundTag ct2 = new CompoundTag();
		ct2.putByte("b", Byte.MAX_VALUE);
		ct2.putString("str", "foo");
		ct2.put("list", new ListTag<>(ByteTag.class));
		ct2.getListTag("list").addByte((byte) 123);
		assertEquals(ct, ct2);

		ct2.getListTag("list").asByteTagList().get(0).setValue((byte) 124);
		assertNotEquals(ct, ct2);

		ct2.remove("str");
		assertNotEquals(ct, ct2);

		assertThrowsNoRuntimeException(() -> ct.equals("blah"));
		assertNotEquals("blah", ct);

		assertEquals(ct, ct);
	}

	public void testHashCode() {
		CompoundTag t = new CompoundTag();
		for (int i = 0; i < 256; i++) {
			t.putByte("key_byte" + i, (byte) i);
			t.putShort("key_short" + i, (short) i);
			t.putInt("key_int" + i, i);
			t.putLong("key_long" + i, i);
			t.putFloat("key_float" + i, i * 1.001f);
			t.putDouble("key_double" + i, i * 1.001);
			t.putString("key_string" + i, "value" + i);

			byte[] bArray = new byte[257];
			int[] iArray = new int[257];
			long[] lArray = new long[257];
			for (byte b = -128; b < 127; b++) {
				bArray[b + 128] = b;
				iArray[b + 128] = b;
				lArray[b + 128] = b;
			}
			bArray[256] = (byte) i;
			iArray[256] = i;
			lArray[256] = i;
			t.putByteArray("key_byte_array" + i, bArray);
			t.putIntArray("key_int_array" + i, iArray);
			t.putLongArray("key_long_array" + i, lArray);

			ListTag<StringTag> l = new ListTag<>(StringTag.class);
			for (int j = 0; j < 256; j++) {
				l.addString("value" + j);
			}
			l.addString("value" + i);
			t.put("key_list" + i, l);

			CompoundTag c = new CompoundTag();
			c.putString("key_string" + i, "value" + i);
			t.put("key_child" + i, c);
		}
		CompoundTag t2 = new CompoundTag();
		for (int i = 0; i < 256; i++) {
			t2.putString("key_string" + i, "value" + i);
			t2.putDouble("key_double" + i, i * 1.001);
			t2.putFloat("key_float" + i, i * 1.001f);
			t2.putLong("key_long" + i, i);
			t2.putInt("key_int" + i, i);
			t2.putShort("key_short" + i, (short) i);
			t2.putByte("key_byte" + i, (byte) i);

			byte[] bArray = new byte[257];
			int[] iArray = new int[257];
			long[] lArray = new long[257];
			for (byte b = -128; b < 127; b++) {
				bArray[b + 128] = b;
				iArray[b + 128] = b;
				lArray[b + 128] = b;
			}
			bArray[256] = (byte) i;
			iArray[256] = i;
			lArray[256] = i;
			t2.putByteArray("key_byte_array" + i, bArray);
			t2.putIntArray("key_int_array" + i, iArray);
			t2.putLongArray("key_long_array" + i, lArray);

			ListTag<StringTag> l = new ListTag<>(StringTag.class);
			for (int j = 0; j < 256; j++) {
				l.addString("value" + j);
			}
			l.addString("value" + i);
			t2.put("key_list" + i, l);

			CompoundTag c = new CompoundTag();
			c.putString("key_string" + i, "value" + i);
			t2.put("key_child" + i, c);
		}

		assertEquals(t, t2);
		assertEquals(t.hashCode(), t2.hashCode());

		t.getCompoundTag("key_child1").remove("key_string1");

		assertNotEquals(t, t2);
		assertNotEquals(t.hashCode(), t2.hashCode());
	}

	public void testClone() {
		CompoundTag ct = createCompoundTag();
		CompoundTag cl = ct.clone();
		assertEquals(ct, cl);
		assertNotSame(ct, cl);
		assertNotSame(ct.get("list"), cl.get("list"));
		assertNotSame(invokeGetValue(ct), invokeGetValue(cl));
	}

	public void testClear() {
		CompoundTag cclear = new CompoundTag();
		cclear.putString("test", "blah");
		assertEquals(1, cclear.size());
		cclear.clear();
		assertEquals(0, cclear.size());
	}

	public void testSerializeDeserialize() {
		CompoundTag ct = createCompoundTag();
		byte[] data = serialize(ct);
		assertArrayEquals(
				new byte[]{
						0x0a, 0x00, 0x00, 0x01, 0x00, 0x01, 0x62, 0x7f,
						0x08, 0x00, 0x03, 0x73, 0x74, 0x72, 0x00, 0x03,
						0x66, 0x6f, 0x6f, 0x09, 0x00, 0x04, 0x6c, 0x69,
						0x73, 0x74, 0x01, 0x00, 0x00, 0x00, 0x01, 0x7b,
						0x00
				}, data);
		CompoundTag tt = (CompoundTag) deserialize(data);
		assertEquals(ct, tt);
	}

	public void testSerializeDeserializeSorted() {
		CompoundTag ct = createCompoundTag();
		byte[] data = serialize(ct, true);
		assertArrayEquals(
				new byte[]{
						0x0a, 0x00, 0x00, 0x01, 0x00, 0x01, 0x62, 0x7f,
						0x09, 0x00, 0x04, 0x6c, 0x69, 0x73, 0x74, 0x01,
						0x00, 0x00, 0x00, 0x01, 0x7b, 0x08, 0x00, 0x03,
						0x73, 0x74, 0x72, 0x00, 0x03, 0x66, 0x6f, 0x6f,
						0x00
				}, data);
		CompoundTag tt = (CompoundTag) deserialize(data);
		assertEquals(ct, tt);
	}

	public void testCasting() {
		CompoundTag cc = new CompoundTag();
		assertNull(cc.get("b", ByteTag.class));
		assertNull(cc.get("b"));
		cc.putByte("b", Byte.MAX_VALUE);
		assertEquals(new ByteTag(Byte.MAX_VALUE), cc.getByteTag("b"));
		assertNull(cc.getByteTag("bb"));
		assertEquals(Byte.MAX_VALUE, cc.get("b", ByteTag.class).asByte());
		assertThrowsRuntimeException(() -> cc.getShort("b"), ClassCastException.class);
		assertEquals(0, cc.getByte("bb"));
		assertTrue(cc.getBoolean("b"));
		cc.putByte("b2", (byte) 0);
		assertFalse(cc.getBoolean("b2"));
		cc.putBoolean("b3", false);
		assertEquals(0, cc.getByte("b3"));
		cc.putBoolean("b4", true);
		assertEquals(1, cc.getByte("b4"));

		cc.putShort("s", Short.MAX_VALUE);
		assertEquals(new ShortTag(Short.MAX_VALUE), cc.getShortTag("s"));
		assertNull(cc.getShortTag("ss"));
		assertEquals(Short.MAX_VALUE, cc.get("s", ShortTag.class).asShort());
		assertThrowsRuntimeException(() -> cc.getInt("s"), ClassCastException.class);
		assertEquals(0, cc.getShort("ss"));

		cc.putInt("i", Integer.MAX_VALUE);
		assertEquals(new IntTag(Integer.MAX_VALUE), cc.getIntTag("i"));
		assertNull(cc.getIntTag("ii"));
		assertEquals(Integer.MAX_VALUE, cc.get("i", IntTag.class).asInt());
		assertThrowsRuntimeException(() -> cc.getLong("i"), ClassCastException.class);
		assertEquals(0, cc.getInt("ii"));

		cc.putLong("l", Long.MAX_VALUE);
		assertEquals(new LongTag(Long.MAX_VALUE), cc.getLongTag("l"));
		assertNull(cc.getLongTag("ll"));
		assertEquals(Long.MAX_VALUE, cc.get("l", LongTag.class).asLong());
		assertThrowsRuntimeException(() -> cc.getFloat("l"), ClassCastException.class);
		assertEquals(0, cc.getLong("ll"));

		cc.putFloat("f", Float.MAX_VALUE);
		assertEquals(new FloatTag(Float.MAX_VALUE), cc.getFloatTag("f"));
		assertNull(cc.getFloatTag("ff"));
		assertEquals(Float.MAX_VALUE, cc.get("f", FloatTag.class).asFloat());
		assertThrowsRuntimeException(() -> cc.getDouble("f"), ClassCastException.class);
		assertEquals(0.0F, cc.getFloat("ff"));

		cc.putDouble("d", Double.MAX_VALUE);
		assertEquals(new DoubleTag(Double.MAX_VALUE), cc.getDoubleTag("d"));
		assertNull(cc.getDoubleTag("dd"));
		assertEquals(Double.MAX_VALUE, cc.get("d", DoubleTag.class).asDouble());
		assertThrowsRuntimeException(() -> cc.getString("d"), ClassCastException.class);
		assertEquals(0.0D, cc.getDouble("dd"));

		cc.putString("st", "foo");
		assertEquals(new StringTag("foo"), cc.getStringTag("st"));
		assertNull(cc.getStringTag("stst"));
		assertEquals("foo", cc.get("st", StringTag.class).getValue());
		assertThrowsRuntimeException(() -> cc.getByteArray("st"), ClassCastException.class);
		assertEquals("", cc.getString("stst"));

		cc.putByteArray("ba", new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE});
		assertEquals(new ByteArrayTag(new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE}), cc.getByteArrayTag("ba"));
		assertNull(cc.getByteArrayTag("baba"));
		assertArrayEquals(new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE}, cc.get("ba", ByteArrayTag.class).getValue());
		assertThrowsRuntimeException(() -> cc.getIntArray("ba"), ClassCastException.class);
		assertArrayEquals(new byte[0], cc.getByteArray("baba"));

		cc.putIntArray("ia", new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
		assertEquals(new IntArrayTag(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}), cc.getIntArrayTag("ia"));
		assertNull(cc.getIntArrayTag("iaia"));
		assertArrayEquals(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}, cc.get("ia", IntArrayTag.class).getValue());
		assertThrowsRuntimeException(() -> cc.getLongArray("ia"), ClassCastException.class);
		assertArrayEquals(new int[0], cc.getIntArray("iaia"));

		cc.putLongArray("la", new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE});
		assertEquals(new LongArrayTag(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE}), cc.getLongArrayTag("la"));
		assertNull(cc.getLongArrayTag("lala"));
		assertArrayEquals(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE}, cc.get("la", LongArrayTag.class).getValue());
		assertThrowsRuntimeException(() -> cc.getListTag("la"), ClassCastException.class);
		assertArrayEquals(new long[0], cc.getLongArray("lala"));

		cc.put("li", new ListTag<>(IntTag.class));
		assertEquals(new ListTag<>(IntTag.class), cc.getListTag("li"));
		assertNull(cc.getListTag("lili"));
		assertThrowsRuntimeException(() -> cc.getCompoundTag("li"), ClassCastException.class);

		cc.put("co", new CompoundTag());
		assertEquals(new CompoundTag(), cc.getCompoundTag("co"));
		assertNull(cc.getCompoundTag("coco"));
		assertThrowsRuntimeException(() -> cc.getByte("co"), ClassCastException.class);
	}

	public void testCompareTo() {
		CompoundTag ci = new CompoundTag();
		ci.putInt("one", 1);
		ci.putInt("two", 2);
		CompoundTag co = new CompoundTag();
		co.putInt("three", 3);
		co.putInt("four", 4);
		assertTrue(0 < ci.compareTo(co));
		co.putInt("five", 5);
		assertEquals(-1, ci.compareTo(co));
		co.remove("five");
		co.remove("four");
		assertEquals(1, ci.compareTo(co));
		assertThrowsRuntimeException(() -> ci.compareTo(null), NullPointerException.class);
	}

	public void testMaxDepth() {
		CompoundTag root = new CompoundTag();
		CompoundTag rec = root;
		for (int i = 0; i < Tag.DEFAULT_MAX_DEPTH + 1; i++) {
			CompoundTag c = new CompoundTag();
			rec.put("c" + i, c);
			rec = c;
		}
		assertThrowsRuntimeException(() -> serialize(root), MaxDepthReachedException.class);
		assertThrowsRuntimeException(() -> deserializeFromFile("max_depth_reached.dat"), MaxDepthReachedException.class);
		assertThrowsNoRuntimeException(() -> root.toString(Tag.DEFAULT_MAX_DEPTH + 1));
		assertThrowsRuntimeException(root::toString, MaxDepthReachedException.class);
		assertThrowsRuntimeException(() -> root.valueToString(-1), IllegalArgumentException.class);
	}

	public void testRecursion() {
		CompoundTag recursive = new CompoundTag();
		recursive.put("recursive", recursive);
		assertThrowsRuntimeException(() -> serialize(recursive), MaxDepthReachedException.class);
		assertThrowsRuntimeException(recursive::toString, MaxDepthReachedException.class);
	}

	public void testEntrySet() {
		CompoundTag e = new CompoundTag();
		e.putInt("int", 123);
		for (NamedTag en : e) {
			assertThrowsIllegalArgumentException(() -> en.setTag(null));
			assertThrowsNoRuntimeException(() -> en.setTag(new IntTag(321)));
		}
		assertEquals(1, e.size());
		assertEquals(321, e.getInt("int"));
	}

	public void testContains() {
		CompoundTag ct = createCompoundTag();
		assertEquals(3, ct.size());
		assertTrue(ct.containsKey("b"));
		assertTrue(ct.containsKey("b", ByteTag.class));
		assertTrue(ct.containsKey("b", NumberTag.class));
		assertTrue(ct.containsKey("str"));
		assertTrue(ct.containsKey("str", StringTag.class));
		assertFalse(ct.containsKey("str", ByteTag.class));
		assertTrue(ct.containsKey("list"));
		assertTrue(ct.containsKey("list", ListTag.class));
		assertFalse(ct.containsKey("list", ByteTag.class));
		assertFalse(ct.containsKey("invalid"));
		assertTrue(ct.containsValue(new StringTag("foo")));
		ListTag<ByteTag> l = new ListTag<>(ByteTag.class);
		l.addByte((byte) 123);
		assertTrue(ct.containsValue(l));
		assertTrue(ct.containsValue(new ByteTag(Byte.MAX_VALUE)));
		assertFalse(ct.containsValue(new ByteTag(Byte.MIN_VALUE)));
		assertFalse(ct.containsKey("blah"));
	}

	public void testIterator() {
		CompoundTag ct = createCompoundTag();
		for (Tag<?> t : ct.values()) {
			assertNotNull(t);
		}
		ct.values().remove(new StringTag("foo"));
		assertFalse(ct.containsKey("str"));
		assertThrowsRuntimeException(() -> ct.values().add(new StringTag("foo")), UnsupportedOperationException.class);
		ct.putString("str", "foo");
		for (String k : ct.keySet()) {
			assertNotNull(k);
			assertTrue(ct.containsKey(k));
		}
		ct.keySet().remove("str");
		assertFalse(ct.containsKey("str"));
		assertThrowsRuntimeException(() -> ct.keySet().add("str"), UnsupportedOperationException.class);
		ct.putString("str", "foo");
		for (NamedTag e : ct) {
			assertNotNull(e.getName());
			assertNotNull(e.getTag());
			assertThrowsIllegalArgumentException(() -> e.setTag(null));
			if (e.getName().equals("str")) {
				assertThrowsNoRuntimeException(() -> e.setTag(new StringTag("bar")));
			}
		}
		assertTrue(ct.containsKey("str"));
		assertEquals("bar", ct.getString("str"));
		for (NamedTag e : ct) {
			assertNotNull(e.getName());
			assertNotNull(e.getTag());
			assertThrowsIllegalArgumentException(() -> e.setTag(null));
			if (e.getName().equals("str")) {
				assertThrowsNoRuntimeException(() -> e.setTag(new StringTag("foo")));
			}
		}
		assertTrue(ct.containsKey("str"));
		assertEquals("foo", ct.getString("str"));
		ct.forEach((k, v) -> {
			assertNotNull(k);
			assertNotNull(v);
		});
		assertEquals(3, ct.size());
	}

	public void testStream() {
		CompoundTag ct = createCompoundTag();
		List<String> keys = ct.stream().map(NamedTag::getName).collect(Collectors.toList());
		assertEquals(ct.size(), keys.size());
		assertTrue(keys.containsAll(Arrays.asList("b", "str", "list")));
	}

	public void testPutIfNotNull() {
		CompoundTag ct = new CompoundTag();
		assertEquals(0, ct.size());
		ct.putIfNotNull("foo", new StringTag("bar"));
		ct.putIfNotNull("bar", null);
		assertEquals(1, ct.size());
		assertEquals("bar", ct.getString("foo"));
	}

	public void testDefaultValueGetters() {
		CompoundTag ct = new CompoundTag();
		assertTrue(ct.getBoolean("name", true));
		assertFalse(ct.getBoolean("name", false));
		assertEquals((byte) -7, ct.getByte("name", (byte) -7));
		assertEquals((short) 13456, ct.getShort("name", (short) 13456));
		assertEquals(13456789, ct.getInt("name", 13456789));
		assertEquals(13456789101112L, ct.getLong("name", 13456789101112L));
		assertEquals(1.23456f, ct.getFloat("name", 1.23456f), 0.5e-5f);
		assertEquals(1.234567981019, ct.getDouble("name", 1.234567981019), 0.5e-12f);
		assertEquals("hello world", ct.getString("name", "hello world"));
	}

	public void testPutGetFloatArrayAsTagList() {
		CompoundTag tag = new CompoundTag();
		float[] values = new float[]{-1.1f, 0f, 1.1f};
		tag.putFloatArrayAsTagList("name", values);
		assertTrue(tag.containsKey("name"));
		assertArrayEquals(values, tag.getFloatTagListAsArray("name"), 1e-5f);

		// put singleton
		tag = new CompoundTag();
		tag.putFloatArrayAsTagList("name", 42.7f);
		assertTrue(tag.containsKey("name"));
		assertArrayEquals(new float[]{42.7f}, tag.getFloatTagListAsArray("name"), 1e-5f);

		// put empty should create empty
		values = new float[0];
		tag = new CompoundTag();
		tag.putFloatArrayAsTagList("name", values);
		assertTrue(tag.containsKey("name"));
		assertEquals(0, tag.getFloatTagListAsArray("name").length);

		// put null should remove tag
		values = null;
		tag = new CompoundTag();
		tag.putFloatArrayAsTagList("name", values);
		assertFalse(tag.containsKey("name"));
	}

	public void testPutGetDoubleArrayAsTagList() {
		CompoundTag tag = new CompoundTag();
		double[] values = new double[]{-1.1f, 0f, 1.1f};
		tag.putDoubleArrayAsTagList("name", values);
		assertTrue(tag.containsKey("name"));
		assertArrayEquals(values, tag.getDoubleTagListAsArray("name"), 1e-5f);

		// put singleton
		tag = new CompoundTag();
		tag.putDoubleArrayAsTagList("name", 42.7);
		assertTrue(tag.containsKey("name"));
		assertArrayEquals(new double[]{42.7}, tag.getDoubleTagListAsArray("name"), 1e-5f);

		// put empty should create empty
		values = new double[0];
		tag = new CompoundTag();
		tag.putDoubleArrayAsTagList("name", values);
		assertTrue(tag.containsKey("name"));
		assertEquals(0, tag.getDoubleTagListAsArray("name").length);

		// put null should remove tag
		values = null;
		tag = new CompoundTag();
		tag.putDoubleArrayAsTagList("name", values);
		assertFalse(tag.containsKey("name"));
	}

	public void testPutGetStringsAsTagList() {
		CompoundTag tag = new CompoundTag();
		List<String> values = Arrays.asList("abba", "dabba");

		tag.putStringsAsTagList("name", values);
		assertTrue(tag.containsKey("name"));
		assertEquals(values, tag.getStringTagListValues("name"));

		// put empty should create empty
		values = new ArrayList<>();
		tag = new CompoundTag();
		tag.putStringsAsTagList("name", values);
		assertTrue(tag.containsKey("name"));
		assertEquals(0, tag.getStringTagListValues("name").size());

		// put null should remove tag
		values = null;
		tag = new CompoundTag();
		tag.putStringsAsTagList("name", values);
		assertFalse(tag.containsKey("name"));
	}

	public void testPutValue() {
		CompoundTag t = new CompoundTag();

		t.putInt("nuke-me", 42);
		t.putValue("nuke-me", null);
		assertFalse(t.containsKey("nuke-me"));

		CompoundTag t2 = new CompoundTag();
		t.putValue("key_tag", t2);
		assertSame(t2, t.get("key_tag"));

		NamedTag t3 = new NamedTag("frank", new StringTag("smith"));
		t.putValue("key_tag3", t3);
		assertSame(t3.getTag(), t.get("key_tag3"));

		t.putValue("key_string", "it's magic");
		assertEquals(new StringTag("it's magic"), t.get("key_string"));

		t.putValue("key_bool0", false);
		assertFalse(t.getBoolean("key_bool0"));
		t.putValue("key_bool1", true);
		assertTrue(t.getBoolean("key_bool1"));


		t.putValue("key_byte", (byte) 2);
		assertEquals(new ByteTag((byte) 2), t.get("key_byte"));

		t.putValue("key_short", (short) 4);
		assertEquals(new ShortTag((short) 4), t.get("key_short"));

		t.putValue("key_int", 5);
		assertEquals(new IntTag(5), t.get("key_int"));

		t.putValue("key_long", 6L);
		assertEquals(new LongTag(6L), t.get("key_long"));

		t.putValue("key_float", 7.8f);
		assertEquals(new FloatTag(7.8f), t.get("key_float"));

		t.putValue("key_double", 8.9);
		assertEquals(new DoubleTag(8.9), t.get("key_double"));


		byte[] ab = new byte[]{(byte) 3, (byte) 7};
		t.putValue("key_byte_array", ab);
		assertArrayEquals(ab, t.getByteArray("key_byte_array"));

		int[] ai = new int[]{11, 13};
		t.putValue("key_int_array", ai);
		assertArrayEquals(ai, t.getIntArray("key_int_array"));

		long[] al = new long[]{17, 19};
		t.putValue("key_long_array", al);
		assertArrayEquals(al, t.getLongArray("key_long_array"));

		float[] af = new float[]{21.5f, 23.5f};
		t.putValue("key_float_array", af);
		float[] af2 = t.getFloatTagListAsArray("key_float_array");
		assertEquals(af.length, af2.length);
		assertEquals(af[0], af2[0], 0.001);
		assertEquals(af[1], af2[1], 0.001);

		double[] ad = new double[]{27.4f, 29.8f};
		t.putValue("key_double_array", ad);
		double[] ad2 = t.getDoubleTagListAsArray("key_double_array");
		assertEquals(ad.length, ad2.length);
		assertEquals(ad[0], ad2[0], 0.001);
		assertEquals(ad[1], ad2[1], 0.001);

		String[] as = new String[]{"bill", "bob", "jeb", "val"};
		t.putValue("key_string_array", as);
		assertEquals(List.of(as), t.getStringTagListValues("key_string_array"));

		assertThrowsException(() -> t.putValue("boom", new Object()), IllegalArgumentException.class);
	}


	public void testPutAll() {
		CompoundTag ct = new CompoundTag();

		NamedTag t1 = new NamedTag("hello", new StringTag("world"));
		NamedTag t2 = new NamedTag("goodbye", new IntTag(-7));
		ct.putAll(List.of(t1, t2));
		assertEquals(2, ct.size());
		assertEquals("world", ct.get("hello").getValue());
		assertEquals(-7, ct.get("goodbye").getValue());
	}
}
