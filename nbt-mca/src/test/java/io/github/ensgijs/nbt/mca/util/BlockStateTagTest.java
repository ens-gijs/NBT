package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.io.TextNbtParser;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.IntTag;
import io.github.ensgijs.nbt.tag.StringTag;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class BlockStateTagTest extends NbtTestCase {

    public void testNameConstructor() {
        BlockStateTag bs = new BlockStateTag("stone");
        assertEquals("{Name:stone}", bs.toString());
        assertThrows(NullPointerException.class, () -> new BlockStateTag((String) null));
    }

    public void testNamePropertiesConstructor() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{vertical_direction:down,waterlogged:\"false\"}}", bs.toString());

        bs = new BlockStateTag("stone", Collections.emptyMap());
        assertEquals("{Name:stone}", bs.toString());

        assertThrows(NullPointerException.class, () -> new BlockStateTag("stone", null));
    }

    public void testCompoundTagConstructor() {
        CompoundTag rawTag = TextNbtParser.parseInline("""
            {
              Properties: {
                vertical_direction: down,
                thickness: middle,
                waterlogged: \"false\"
              },
              Name: "minecraft:pointed_dripstone"
            }""");

        BlockStateTag bs = new BlockStateTag(rawTag);
        assertEquals("minecraft:pointed_dripstone", bs.getName());
        assertEquals("down", bs.get("vertical_direction"));
        assertSame(rawTag, bs.getHandle());
        assertSame(rawTag, bs.updateHandle());
    }

    public void testSetPropertiesByMap() {
        BlockStateTag bs = new BlockStateTag("minecraft:pointed_dripstone");
        bs.setProperties(Map.of("waterlogged", false, "vertical_direction", "down"));
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{vertical_direction:down,waterlogged:\"false\"}}", bs.toString());

        bs.setProperties(Map.of("waterlogged", true, "thickness", new StringTag("tip")));
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{thickness:tip,waterlogged:\"true\"}}", bs.toString());

        bs.setProperties(Collections.emptyMap());
        assertEquals("{Name:\"minecraft:pointed_dripstone\"}", bs.toString());
    }

    public void testSetPropertiesByCompoundTag() {
        BlockStateTag bs = new BlockStateTag("minecraft:pointed_dripstone");
        bs.setProperties(Map.of("waterlogged", false, "vertical_direction", "down"));
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{vertical_direction:down,waterlogged:\"false\"}}", bs.toString());

        bs.setProperties((CompoundTag) null);
        assertEquals("{Name:\"minecraft:pointed_dripstone\"}", bs.toString());

        CompoundTag newProp = new CompoundTag();
        newProp.putString("waterlogged", "true");
        newProp.putInt("vale", 42);
        bs.setProperties(newProp);
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{vale:42,waterlogged:\"true\"}}", bs.toString());
        assertSame(newProp, bs.getHandle().get("Properties"));
        bs.setProperties(newProp);
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{vale:42,waterlogged:\"true\"}}", bs.toString());
        assertSame(newProp, bs.getHandle().get("Properties"));

        bs.setProperties(new CompoundTag());
        assertEquals("{Name:\"minecraft:pointed_dripstone\"}", bs.toString());
        bs.put("vale", 99);
        assertEquals(42, newProp.getInt("vale"));
    }

    public void testSetName() {
        BlockStateTag bs = new BlockStateTag("stone");
        bs.setName("diamond_block");
        assertEquals("diamond_block", bs.getName());
        assertEquals("diamond_block", bs.getHandle().getString("Name"));
    }

    public void testIsEmpty() {
        BlockStateTag bs = new BlockStateTag("stone");
        assertTrue(bs.isEmpty());
        bs.put("facing", "west");
        assertFalse(bs.isEmpty());
    }

    public void testHasProperty() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertTrue(bs.hasProperty("waterlogged"));
        assertFalse(bs.hasProperty("facing"));
    }

    public void testGet() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertEquals("false", bs.get("waterlogged"));
        assertNull(bs.get("facing"));
    }

    public void testPut() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        bs.put("waterlogged", true);
        assertEquals("true", bs.get("waterlogged"));
        bs.put("facing", new StringTag("north"));
        assertEquals("north", bs.get("facing"));
        bs.put("value", 420L);
        assertEquals("420", bs.get("value"));
        assertEquals("420", bs.put("value", null));
        assertFalse(bs.hasProperty("value"));
    }

    public void testRemoveByKey() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertEquals("false", bs.remove("waterlogged"));
        assertNull(bs.remove("waterlogged"));
    }

    public void testRemoveByKeyValue() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertFalse(bs.remove("vertical_direction", "up"));
        assertTrue(bs.remove("vertical_direction", new StringTag("down")));
        assertFalse(bs.remove("waterlogged", true));
        assertTrue(bs.remove("waterlogged", false));
        assertFalse(bs.remove("whatever", "thing"));
        assertEquals("{Name:\"minecraft:pointed_dripstone\"}", bs.toString());
    }

    public void testPutAll() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        bs.putAll(Map.of(
            "thickness", new StringTag("tip"),
            "waterlogged", true
        ));
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{thickness:tip,vertical_direction:down,waterlogged:\"true\"}}",
                bs.toString());
    }

    public void testClear() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        bs.clear();
        assertTrue(bs.isEmpty());
        assertEquals("{Name:\"minecraft:pointed_dripstone\"}", bs.toString());
    }

    public void testGetOrDefault() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertEquals("false", bs.getOrDefault("waterlogged", true));
        assertEquals("42", bs.getOrDefault("life", new IntTag(42)));
        assertNull(bs.getOrDefault("life", null));
    }

    public void testPutIfAbsent() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertEquals("false", bs.putIfAbsent("waterlogged", true));
        assertNull(bs.putIfAbsent("life", new IntTag(42)));
        assertEquals("42", bs.get("life"));
    }

    public void testReplaceIfPresent() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));

        assertNull(bs.replace("whatever", 22));
        assertFalse(bs.hasProperty("whatever"));

        assertEquals("false", bs.replace("waterlogged", true));
        assertEquals("true", bs.get("waterlogged"));
    }

    public void testReplaceIfMatchesExisting() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));

        assertFalse(bs.replace("vertical_direction", "up", "down"));
        assertEquals("down", bs.get("vertical_direction"));
        assertTrue(bs.replace("vertical_direction", "down", "up"));
        assertEquals("up", bs.get("vertical_direction"));

        assertFalse(bs.replace("whatever", "up", "down"));
    }

    public void testComputeIfAbsent() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertThrows(NullPointerException.class, () -> bs.computeIfAbsent("foo", null));

        assertEquals("false", bs.computeIfAbsent("waterlogged", k -> k));
        assertEquals("false", bs.get("waterlogged"));

        assertEquals("middle", bs.computeIfAbsent("thickness", k -> "middle"));
        assertEquals("middle", bs.get("thickness"));
    }

    public void testComputeIfPresent() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        assertThrows(NullPointerException.class, () -> bs.computeIfPresent("waterlogged", null));

        assertEquals("true", bs.computeIfPresent("waterlogged", (k, v) -> "true"));
        assertEquals("true", bs.get("waterlogged"));

        assertNull(bs.computeIfPresent("thickness", (k, v) -> "middle"));
        assertNull(bs.get("thickness"));
    }

    public void testCompute() {
        BlockStateTag bs = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down", "thickness", "middle"));
        assertThrows(NullPointerException.class, () -> bs.computeIfPresent("waterlogged", null));

        assertEquals("tip", bs.compute("thickness", (k, v) -> {
            assertEquals("thickness", k);
            assertEquals("middle", v);
            return "tip";
        }));

        assertNull(bs.compute("thickness", (k, v) -> null));

        assertEquals("something", bs.compute("nothing", (k, v) -> "something"));
        assertEquals("{Name:\"minecraft:pointed_dripstone\",Properties:{nothing:something,vertical_direction:down,waterlogged:\"false\"}}", bs.toString());
    }

    public void testEquals() {
        BlockStateTag bs1 = new BlockStateTag(
                "minecraft:pointed_dripstone",
                Map.of("waterlogged", false, "vertical_direction", "down"));
        BlockStateTag bs2 = new BlockStateTag("minecraft:pointed_dripstone");
        assertNotEquals(bs1, bs2);
        bs2.put("waterlogged", "false");
        bs2.put("vertical_direction", "up");
        assertNotEquals(bs1, bs2);
        bs2.put("vertical_direction", "down");
        assertEquals(bs1, bs2);

        assertNotEquals(null, bs1);
        assertNotEquals(bs1, new Object());
    }
}
