package io.github.ensgijs.nbt.tag;

import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.io.TextNbtHelpers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;

public class TagTest extends NbtTestCase {

    public void testAsTag() {
        assertNull(Tag.asTag(null));

        CompoundTag t2 = new CompoundTag();
        assertSame(t2, Tag.asTag(t2));

        NamedTag t3 = new NamedTag("frank", new StringTag("smith"));
        assertSame(t3.getTag(), Tag.asTag(t3));

        assertEquals(new StringTag("it's magic"), Tag.asTag("it's magic"));

        assertEquals(new ByteTag(false), Tag.asTag(false));
        assertEquals(new ByteTag(true), Tag.asTag(true));


        assertEquals(new ByteTag((byte) 2), Tag.asTag((byte) 2));

        assertEquals(new ShortTag((short) 4), Tag.asTag((short) 4));

        assertEquals(new IntTag(5), Tag.asTag(5));

        assertEquals(new LongTag(6L), Tag.asTag(6L));

        assertEquals(new FloatTag(7.8f), Tag.asTag(7.8f));

        assertEquals(new DoubleTag(8.9), Tag.asTag(8.9));


        byte[] ab = new byte[]{(byte) 3, (byte) 7};
        assertEquals(new ByteArrayTag(ab), Tag.asTag(ab));

        int[] ai = new int[]{11, 13};
        assertEquals(new IntArrayTag(ai), Tag.asTag(ai));

        long[] al = new long[]{17, 19};
        assertEquals(new LongArrayTag(al), Tag.asTag(al));

        final float[] af = new float[]{21.5f, 23.5f};
        ListTag<FloatTag> afTag = (ListTag<FloatTag>) Tag.asTag(af);
        assertEquals(af.length, afTag.size());
        assertEquals(af[0], afTag.get(0).getValue(), 0.001);
        assertEquals(af[1], afTag.get(1).getValue(), 0.001);

        afTag = (ListTag<FloatTag>) Tag.asTag(List.of(21.5f, 23.5f));
        assertEquals(af.length, afTag.size());
        assertEquals(af[0], afTag.get(0).getValue(), 0.001);
        assertEquals(af[1], afTag.get(1).getValue(), 0.001);

        final double[] ad = new double[]{27.4, 29.8};
        ListTag<DoubleTag> dfTag = (ListTag<DoubleTag>) Tag.asTag(ad);
        assertEquals(ad.length, dfTag.size());
        assertEquals(ad[0], dfTag.get(0).getValue(), 0.001);
        assertEquals(ad[1], dfTag.get(1).getValue(), 0.001);

        dfTag = (ListTag<DoubleTag>) Tag.asTag(List.of(27.4, 29.8));
        assertEquals(ad.length, dfTag.size());
        assertEquals(ad[0], dfTag.get(0).getValue(), 0.001);
        assertEquals(ad[1], dfTag.get(1).getValue(), 0.001);

        final String[] as = new String[]{"bill", "bob", "jeb", "val"};
        ListTag<StringTag> asTag = (ListTag<StringTag>) Tag.asTag(as);
        assertEquals(as.length, asTag.size());
        assertEquals(as[0], asTag.get(0).getValue());
        assertEquals(as[1], asTag.get(1).getValue());
        assertEquals(as[2], asTag.get(2).getValue());
        assertEquals(as[3], asTag.get(3).getValue());

        asTag = (ListTag<StringTag>) Tag.asTag(List.of("bill", "bob", "jeb", "val"));
        assertEquals(as.length, asTag.size());
        assertEquals(as[0], asTag.get(0).getValue());
        assertEquals(as[1], asTag.get(1).getValue());
        assertEquals(as[2], asTag.get(2).getValue());
        assertEquals(as[3], asTag.get(3).getValue());

        final Map<String, Object> rawMap = Map.of(
                "key_int", 42,
                "key_float_array", new float[] {12.7f, 99.8f},
                "key_compound", Map.of("one", 1, "str", "thing")
        );

        assertEquals("""
                {
                  key_compound: {one: 1, str: thing},
                  key_float_array: [12.7f, 99.8f],
                  key_int: 42
                }""", TextNbtHelpers.toTextNbt(Tag.asTag(rawMap), true));

        assertThrowsException(() -> Tag.asTag(new Object()), IllegalArgumentException.class);
    }
}
