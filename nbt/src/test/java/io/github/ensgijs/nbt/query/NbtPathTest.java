package io.github.ensgijs.nbt.query;

import io.github.ensgijs.nbt.tag.*;
import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.io.TextNbtParser;
import org.junit.Assert;

public class NbtPathTest extends NbtTestCase {

    private void assertParse(String path) {
        assertEquals(path, NbtPath.of(path).toString());
    }

    private void assertParse(String expected, String path) {
        assertEquals(expected, NbtPath.of(path).toString());
    }

    private void assertParseThrows(String path) {
        assertThrowsIllegalArgumentException(() -> NbtPath.of(path));
    }

    public void testParsingAndToString() {
        assertParse("a");
        assertParse("a.b");
        assertParse("a[0].b");
        assertParse("a.b[7]");
        assertParse("a[0].b[2].c");
        assertParse("[42]");
        assertParse("[42].thing");
        assertParse("[42][19].thing.whatever.z[0].a.b");
        assertParse("4.baz");  // numbers can be names too

        assertParse("");
        // a leading dot is OK
        assertParse("", ".");
        assertParse("foo", ".foo");
        // but two leading dots aren't
        assertParseThrows("..");
        assertParseThrows("..b");
        assertParseThrows("..bob");

        assertParse("[1]");
        assertParseThrows(".[0]");
        assertParseThrows("a..b");
        assertParseThrows("foo.");
        assertParseThrows("foo.bar.");
        assertParseThrows("foo.bar.[0]");

        assertParseThrows("a[]");
        assertParseThrows("[]");
        assertParseThrows("[z]");
        assertParseThrows("[zap]");
        assertParseThrows("[-2]");
        assertParseThrows("[-]");
        assertParseThrows("a[0");
        assertParseThrows("a[0]b[1]");
        assertParseThrows("a]0");
        assertParseThrows("a]0[");
        assertParseThrows("a[0]]");
    }

    public void testUsageSanity() {
        CompoundTag root = new CompoundTag();
        ListTag<CompoundTag> teamTag = new ListTag<>(CompoundTag.class);

        CompoundTag tmp = new CompoundTag();
        tmp.putString("name", "Bob");
        tmp.putBoolean("boolT", true);
        tmp.putBoolean("boolF", false);
        tmp.putByte("byte", (byte) -42);
        tmp.putShort("short", (short) 12345);
        tmp.putInt("int", 1001);
        tmp.putLong("long", 99988765454785765L);
        tmp.putFloat("float", 0.35f);
        tmp.putDouble("double", 9.78d);
        tmp.putLongArray("history", new long[]{43L, 56L, 89L});
        teamTag.add(tmp);

        tmp = new CompoundTag();
        tmp.putString("name", "Jebbb");
        tmp.putLongArray("history", new long[]{12L, 11L, 15L});
        teamTag.add(tmp);

        root.put("team", teamTag);
        root.putInt("version", 1234);

        assertNull(NbtPath.of("xyz").get(root));  // doesnt exist
        assertNotNull(NbtPath.of("team").get(root));  // exists
        assertNull(NbtPath.of("team[6]").get(root));  // index doesn't exist

        assertSame(tmp, NbtPath.of("[1]").get(teamTag));  // can use a ListTag as root

        // value getters
        assertEquals("Bob", NbtPath.of("team[0].name").getString(root));
        assertTrue(NbtPath.of("team[0].boolT").getBoolean(root));
        assertFalse(NbtPath.of("team[0].boolF").getBoolean(root));
        assertEquals((byte) -42, NbtPath.of("team[0].byte").getByte(root));
        assertEquals((short) 12345, NbtPath.of("team[0].short").getShort(root));
        assertEquals(1001 , NbtPath.of("team[0].int").getInt(root));
        assertEquals(99988765454785765L , NbtPath.of("team[0].long").getLong(root));
        assertEquals(0.35f, NbtPath.of("team[0].float").getFloat(root), 1e-6);
        assertEquals(9.78d, NbtPath.of("team[0].double").getDouble(root), 1e-6);

        // missing value getters
        assertNull(NbtPath.of("team[0].whatever").getString(root));
        assertNull(NbtPath.of("team[0].whatever.something.else").getString(root));
        assertFalse(NbtPath.of("team[0].whatever").getBoolean(root));
        assertFalse(NbtPath.of("team[0].whatever.something.else").getBoolean(root));
        assertEquals((byte) 0, NbtPath.of("team[0].whatever").getByte(root));
        assertEquals((byte) 0, NbtPath.of("team[0].whatever.something.else").getByte(root));
        assertEquals((short) 0, NbtPath.of("team[0].whatever").getShort(root));
        assertEquals((short) 0, NbtPath.of("team[0].whatever.something.else").getShort(root));
        assertEquals(0, NbtPath.of("team[0].whatever").getInt(root));
        assertEquals(0, NbtPath.of("team[0].whatever.something.else").getInt(root));
        assertEquals(0L, NbtPath.of("team[0].whatever").getLong(root));
        assertEquals(0L, NbtPath.of("team[0].whatever.something.else").getLong(root));
        assertEquals(0f, NbtPath.of("team[0].whatever").getFloat(root));
        assertEquals(0f, NbtPath.of("team[0].whatever.something.else").getFloat(root));
        assertEquals(0d, NbtPath.of("team[0].whatever").getDouble(root));
        assertEquals(0d, NbtPath.of("team[0].whatever.something.else").getDouble(root));

        assertEquals(15L, NbtPath.of("team[1].history[2]").getLong(root));  // getting a raw long from a LongArray

        // get - throws when not the right type
        assertThrowsException(() -> NbtPath.of("team[0].long").getString(root), ClassCastException.class);
        assertThrowsException(() -> NbtPath.of("team[0].name").getByte(root), ClassCastException.class);
        assertThrowsException(() -> NbtPath.of("team[0].name").getShort(root), ClassCastException.class);
        assertThrowsException(() -> NbtPath.of("team[0].name").getInt(root), ClassCastException.class);
        assertThrowsException(() -> NbtPath.of("team[0].name").getLong(root), ClassCastException.class);
        assertThrowsException(() -> NbtPath.of("team[0].name").getFloat(root), ClassCastException.class);
        assertThrowsException(() -> NbtPath.of("team[0].name").getDouble(root), ClassCastException.class);


        // size function
        assertEquals(2, NbtPath.of("team").size(root));  // size of list tag
        assertEquals(3, NbtPath.of("team[1].history").size(root));  // size of long array
        assertEquals(0, NbtPath.of("meme").size(root));  // no-existing tag size
        assertEquals(5, NbtPath.of("team[1].name").size(root));  // string size
        assertEquals(2, NbtPath.of("").size(root));  // size of compound tag

        // can't get these sizes
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team[0].byte").size(root));
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team[0].short").size(root));
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team[0].int").size(root));
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team[0].long").size(root));
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team[0].float").size(root));
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team[0].double").size(root));

        // exists function
        assertTrue(NbtPath.of("team").exists(root));
        assertTrue(NbtPath.of("team[1].history").exists(root));
        assertFalse(NbtPath.of("meme").exists(root));
        assertTrue(NbtPath.of("").exists(root));
        assertTrue(NbtPath.of("team[1].history[0]").exists(root));
        assertFalse(NbtPath.of("team[1].history[99]").exists(root));

        assertFalse(NbtPath.of("team[1].abc").exists(root));
        assertFalse(NbtPath.of("team[100].abc").exists(root));
    }

    public void testPutTag() {
        // throws when create parents is FALSE
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team.alpha").putTag(new CompoundTag(), new StringTag("STOP"), false));

        final CompoundTag root = new CompoundTag();
        // now try with create parents - and verify that no previous tag was replaced
        assertNull(NbtPath.of("team.alpha").putTag(root, new StringTag("GO GO"), true));
        assertEquals("GO GO", NbtPath.of("team.alpha").getString(root));
        // can't treat a compound as a list
        assertThrowsIllegalArgumentException(() -> NbtPath.of("team[0]").getTag(root));

        // check that the previous tag is returned - and the new value is set
        StringTag oldTag = NbtPath.of("team.alpha").putTag(root, new StringTag("Weeee"));
        assertNotNull(oldTag);
        assertEquals("GO GO", oldTag.getValue());
        assertEquals("Weeee", NbtPath.of("team.alpha").getString(root));

        // now test removal
        assertNull(NbtPath.of("team.beta").putTag(root, null));  // doesn't exist - should be FINE
        oldTag = NbtPath.of("team.alpha").putTag(root, null);
        assertNotNull(oldTag);
        assertEquals("Weeee", oldTag.getValue());
        assertNull(NbtPath.of("team.alpha").getTag(root));  // verify it's removed

        root.put("teams", new ListTag<>(CompoundTag.class));
        root.getListTag("teams").asCompoundTagList().add(new CompoundTag());
        // this works because we initialized list element [0] first
        NbtPath.of("teams[0].name").putTag(root, new StringTag("alpha"));
        assertEquals("alpha", NbtPath.of("teams[0].name").getString(root));

        // should create stats for us (not the first case - where we don't create parents)
        assertThrowsIllegalArgumentException(() -> NbtPath.of("teams[0].stats.wins").putTag(root, new IntTag(1)));
        NbtPath.of("teams[0].stats.wins").putTag(root, new IntTag(1), true);

        // list element [1] doesn't exist
        assertThrowsIllegalArgumentException(() -> NbtPath.of("teams[1].name").putTag(root, new StringTag("beta")));

        // cannot add tag list (at this time... i mean really, it doesn't make sense as the index would need to be
        // put in the path string - at that point trying to use this type of solution would just be bad practice)
        assertThrowsIllegalArgumentException(() -> NbtPath.of("zom.lom[0]").putTag(root, new CompoundTag()));

        // can't add a keyed field to a TagList (requires an index)
        assertThrowsUnsupportedOperationException(() -> NbtPath.of("teams.broken").putTag(root, new ByteTag(true)));
        assertThrowsUnsupportedOperationException(() -> NbtPath.of("teams.broken").putTag(root, new ByteTag(true), true));


        NbtPath.of("long").putTag(root, new LongTag(19));
        // can't get a child of a primitive tag
        assertNull(NbtPath.of("long.foo").getTag(root));
        // can't treat a primitive as a list
        assertThrowsIllegalArgumentException(() -> NbtPath.of("long[0]").getTag(root));

        // can't add a child to a primitive tag
        assertThrowsUnsupportedOperationException(() -> NbtPath.of("long.foo.bar").putTag(root, new CompoundTag(), true));
    }

    public void testGetTypedArrayTags() {
        CompoundTag root = TextNbtParser.parseInline("{stuff:{ints:[I;11223344,5,7],bytes:[B;-100,50],longs:[L;2490852214246277761,17616930435]}}");
        Assert.assertArrayEquals(new int[] {11223344, 5, 7}, NbtPath.of("stuff.ints").getIntArray(root));
        Assert.assertArrayEquals(new byte[] {(byte) -100, (byte) 50}, NbtPath.of("stuff.bytes").getByteArray(root));
        Assert.assertArrayEquals(new long[] {2490852214246277761L, 17616930435L}, NbtPath.of("stuff.longs").getLongArray(root));
    }
}
