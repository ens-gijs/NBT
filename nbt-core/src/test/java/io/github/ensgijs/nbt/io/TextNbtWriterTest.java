package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.tag.*;

import java.util.LinkedHashMap;

public class TextNbtWriterTest extends NbtTestCase {

	public void testToTextNbt_Tag_noPrettyPrint() {
		// write number tags

		assertEquals("127b", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new ByteTag(Byte.MAX_VALUE))));
		assertEquals("-32768s", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new ShortTag(Short.MIN_VALUE))));
		assertEquals("-2147483648", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new IntTag(Integer.MIN_VALUE))));
		assertEquals("-9223372036854775808l", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new LongTag(Long.MIN_VALUE))));
		assertEquals("123.456f", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new FloatTag(123.456F))));
		assertEquals("123.456d", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new DoubleTag(123.456D))));

		// write array tags

		assertEquals("[B;-128,0,127]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new ByteArrayTag(new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE}), false)));
		assertEquals("[I;-2147483648,0,2147483647]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new IntArrayTag(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}), false)));
		assertEquals("[L;-9223372036854775808,0,9223372036854775807]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new LongArrayTag(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE}), false)));

		// write string tag

//		assertEquals("\"abc\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag("abc"))));
		assertEquals("abc", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag("abc"))));
		assertEquals("\"123\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag("123"))));
		assertEquals("\"123.456\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag("123.456"))));
		assertEquals("\"-123\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag("-123"))));
		assertEquals("\"-1.23e14\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag("-1.23e14"))));
		assertEquals("\"äöü\\\\\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag("äöü\\"))));
		assertEquals("\"\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new StringTag(""))));

		// write list tag

		ListTag<StringTag> lt = new ListTag<>(StringTag.class);
		lt.addString("blah");
		lt.addString("blubb");
		lt.addString("123");
		lt.addString("");
		assertEquals("[blah,blubb,\"123\",\"\"]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(lt, false)));
//		assertEquals("[\"blah\",\"blubb\",\"123\",\"\"]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(lt, false)));

		// write compound tag
		CompoundTag ct = new CompoundTag();
		invokeSetValue(ct, new LinkedHashMap<>());
		ct.putString("key", "value");
		ct.putByte("byte", Byte.MAX_VALUE);
		ct.putByteArray("array", new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE});
		ListTag<StringTag> clt = new ListTag<>(StringTag.class);
		clt.addString("foo");
		clt.addString("bar");
		ct.put("list", clt);
//		String ctExpectedUnsorted = "{key:\"value\",byte:127b,array:[B;-128,0,127],list:[\"foo\",\"bar\"]}";
//		String ctExpectedSorted = "{array:[B;-128,0,127],byte:127b,key:\"value\",list:[\"foo\",\"bar\"]}";
		String ctExpectedUnsorted = "{key:value,byte:127b,array:[B;-128,0,127],list:[foo,bar]}";
		String ctExpectedSorted = "{array:[B;-128,0,127],byte:127b,key:value,list:[foo,bar]}";
		assertEquals(ctExpectedUnsorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbtUnsorted(ct, false)));
		assertEquals(ctExpectedSorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(ct, false)));

		// write compound tag containing an empty string value
		CompoundTag ct2 = new CompoundTag();
		ct2.put("empty", new StringTag());
		assertEquals("{empty:\"\"}", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(ct2, false)));
	}

	public void testToTextNbt_Tag_prettyPrintIsDefault() {
		// write compound tag
		CompoundTag ct = new CompoundTag();
		invokeSetValue(ct, new LinkedHashMap<>());
		ct.putString("key", "value");
		ct.putByte("byte", Byte.MAX_VALUE);
		ct.putByteArray("array", new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE});
		ListTag<StringTag> clt = new ListTag<>(StringTag.class);
		clt.addString("foo");
		clt.addString("bar");
		ct.put("list", clt);
//		String ctExpectedUnsorted =
//				"{\n" +
//				"  key: \"value\",\n" +
//				"  byte: 127b,\n" +
//				"  array: [B;\n" +
//				"    -128,\n" +
//				"    0,\n" +
//				"    127\n" +
//				"  ],\n" +
//				"  list: [\n" +
//				"    \"foo\",\n" +
//				"    \"bar\"\n" +
//				"  ]\n" +
//				"}";
//
//		String ctExpectedSorted =
//				"{\n" +
//						"  array: [B;\n" +
//						"    -128,\n" +
//						"    0,\n" +
//						"    127\n" +
//						"  ],\n" +
//						"  byte: 127b,\n" +
//						"  key: \"value\",\n" +
//						"  list: [\n" +
//						"    \"foo\",\n" +
//						"    \"bar\"\n" +
//						"  ]\n" +
//						"}";

		String ctExpectedUnsorted =
				"""
						{
						  key: value,
						  byte: 127b,
						  array: [B; -128, 0, 127],
						  list: [foo, bar]
						}""";

		String ctExpectedSorted =
				"""
						{
						  array: [B; -128, 0, 127],
						  byte: 127b,
						  key: value,
						  list: [foo, bar]
						}""";
		assertEquals(ctExpectedUnsorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbtUnsorted(ct)));
		assertEquals(ctExpectedSorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(ct)));
	}


	public void testToTextNbt_NamedTag_noPrettyPrint() {
		// write number tags

		assertEquals("bb: 127b", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("bb", new ByteTag(Byte.MAX_VALUE)))));
		assertEquals("bb: -32768s", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("bb", new ShortTag(Short.MIN_VALUE)))));
		assertEquals("bb: -2147483648", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("bb", new IntTag(Integer.MIN_VALUE)))));
		assertEquals("bb: -9223372036854775808l", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("bb", new LongTag(Long.MIN_VALUE)))));
		assertEquals("bb: 123.456f", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("bb", new FloatTag(123.456F)))));
		assertEquals("bb: 123.456d", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("bb", new DoubleTag(123.456D)))));

		// write array tags + a space in name

		assertEquals("\"the thing\":[B;-128,0,127]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("the thing", new ByteArrayTag(new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE})), false)));
		assertEquals("\"the thing\":[I;-2147483648,0,2147483647]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("the thing", new IntArrayTag(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE})), false)));
		assertEquals("\"the thing\":[L;-9223372036854775808,0,9223372036854775807]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("the thing", new LongArrayTag(new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE})), false)));

		// write string tag + dot in name

//		assertEquals("\"plugin.setting\":\"abc\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("abc")), false)));
		assertEquals("\"plugin.setting\":abc", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("abc")), false)));
		assertEquals("\"plugin.setting\":\"123\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("123")), false)));
		assertEquals("\"plugin.setting\":\"123.456\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("123.456")), false)));
		assertEquals("\"plugin.setting\":\"-123\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("-123")), false)));
		assertEquals("\"plugin.setting\":\"-1.23e14\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("-1.23e14")), false)));
		assertEquals("\"plugin.setting\":\"äöü\\\\\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("äöü\\")), false)));
		assertEquals("\"plugin.setting\":\"\"", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("plugin.setting", new StringTag("")), false)));

		// write list tag

		ListTag<StringTag> lt = new ListTag<>(StringTag.class);
		lt.addString("blah");
		lt.addString("blubb");
		lt.addString("123");
		lt.addString("");
//		assertEquals("\"foo bar\":[\"blah\",\"blubb\",\"123\",\"\"]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("foo bar", lt), false)));
		assertEquals("\"foo bar\":[blah,blubb,\"123\",\"\"]", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("foo bar", lt), false)));

		// write compound tag + number as name
		CompoundTag ct = new CompoundTag();
		invokeSetValue(ct, new LinkedHashMap<>());
		ct.putString("key", "value");
		ct.putByte("byte", Byte.MAX_VALUE);
		ct.putByteArray("array", new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE});
		ListTag<StringTag> clt = new ListTag<>(StringTag.class);
		clt.addString("foo");
		clt.addString("bar");
		ct.put("list", clt);
//		String ctExpectedUnsorted = "1:{key:\"value\",byte:127b,array:[B;-128,0,127],list:[\"foo\",\"bar\"]}";
//		String ctExpectedSorted = "1:{array:[B;-128,0,127],byte:127b,key:\"value\",list:[\"foo\",\"bar\"]}";
		String ctExpectedUnsorted = "1:{key:value,byte:127b,array:[B;-128,0,127],list:[foo,bar]}";
		String ctExpectedSorted = "1:{array:[B;-128,0,127],byte:127b,key:value,list:[foo,bar]}";
		assertEquals(ctExpectedUnsorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbtUnsorted(new NamedTag("1", ct), false)));
		assertEquals(ctExpectedSorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("1", ct), false)));

		// write compound tag containing an empty string value + float looking name
		CompoundTag ct2 = new CompoundTag();
		ct2.put("empty", new StringTag());
		assertEquals("\"1.5\":{empty:\"\"}", assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("1.5", ct2), false)));
	}

	public void testToTextNbt_NamedTag_prettyPrintIsDefault() {
		// write compound tag
		CompoundTag ct = new CompoundTag();
		invokeSetValue(ct, new LinkedHashMap<>());
		ct.putString("key", "value");
		ct.putByte("byte", Byte.MAX_VALUE);
		ct.putByteArray("array", new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE});
		ListTag<StringTag> clt = new ListTag<>(StringTag.class);
		clt.addString("foo");
		clt.addString("bar");
		ct.put("list", clt);
		String ctExpectedUnsorted =
				"""
						my-name-is: {
						  key: value,
						  byte: 127b,
						  array: [B; -128, 0, 127],
						  list: [foo, bar]
						}""";
		String ctExpectedSorted =
				"""
						my-name-is: {
						  array: [B; -128, 0, 127],
						  byte: 127b,
						  key: value,
						  list: [foo, bar]
						}""";
		assertEquals(ctExpectedUnsorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbtUnsorted(new NamedTag("my-name-is", ct))));
		assertEquals(ctExpectedSorted, assertThrowsNoException(() -> TextNbtHelpers.toTextNbt(new NamedTag("my-name-is", ct))));
	}

	public void testTrueFalseStrings_shouldRemainStrings() {
		CompoundTag ct = new CompoundTag();
		ct.putString("not-a-bool-T", "true");
		ct.putString("not-a-bool-F", "false");
		assertEquals("{not-a-bool-F:\"false\",not-a-bool-T:\"true\"}", TextNbtHelpers.toTextNbt(ct, false));
	}
}
