package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.mca.DataVersion;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;
import io.github.ensgijs.nbt.tag.LongArrayTag;
import io.github.ensgijs.nbt.tag.StringTag;
import io.github.ensgijs.nbt.NbtTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertArrayEquals;

public class PalettizedCuboidTest extends NbtTestCase {

    public void testCalculateBitMask_throwsIllegalArgumentExceptionAppropriately() {
        assertThrowsException(() -> PalettizedCuboid.calculateBitMask(-1), IllegalArgumentException.class);
        assertThrowsException(() -> PalettizedCuboid.calculateBitMask(32), IllegalArgumentException.class);
    }

    public void testCalculateBitMask_typicalUsage() {
        assertEquals(0x1, PalettizedCuboid.calculateBitMask(1));
        assertEquals(0x3, PalettizedCuboid.calculateBitMask(2));
        assertEquals(0x7, PalettizedCuboid.calculateBitMask(3));
        assertEquals(0xf, PalettizedCuboid.calculateBitMask(4));
        assertEquals(0x1f, PalettizedCuboid.calculateBitMask(5));
        assertEquals(0x3f, PalettizedCuboid.calculateBitMask(6));
        assertEquals(0x7f, PalettizedCuboid.calculateBitMask(7));
        assertEquals(0xff, PalettizedCuboid.calculateBitMask(8));
        assertEquals(0xffff, PalettizedCuboid.calculateBitMask(16));
        assertEquals(0x7fffffff, PalettizedCuboid.calculateBitMask(31));
    }

    public void testCalculatePowerOfTwoExponent() {
        assertEquals(0, PalettizedCuboid.calculatePowerOfTwoExponent(0, true));
        assertEquals(0, PalettizedCuboid.calculatePowerOfTwoExponent(1, true));
        assertEquals(1, PalettizedCuboid.calculatePowerOfTwoExponent(2, true));
        assertEquals(2, PalettizedCuboid.calculatePowerOfTwoExponent(4, true));
        assertEquals(3, PalettizedCuboid.calculatePowerOfTwoExponent(8, true));
        assertEquals(4, PalettizedCuboid.calculatePowerOfTwoExponent(16, true));

        assertThrowsException(() -> PalettizedCuboid.calculatePowerOfTwoExponent(7, true), IllegalArgumentException.class);

        assertEquals(0, PalettizedCuboid.calculatePowerOfTwoExponent(0, false));
        assertEquals(0, PalettizedCuboid.calculatePowerOfTwoExponent(1, false));
        assertEquals(1, PalettizedCuboid.calculatePowerOfTwoExponent(2, false));
        assertEquals(2, PalettizedCuboid.calculatePowerOfTwoExponent(4, false));
        assertEquals(3, PalettizedCuboid.calculatePowerOfTwoExponent(8, false));
        assertEquals(4, PalettizedCuboid.calculatePowerOfTwoExponent(16, false));

        assertEquals(2, PalettizedCuboid.calculatePowerOfTwoExponent(3, false));
        assertEquals(3, PalettizedCuboid.calculatePowerOfTwoExponent(5, false));
        assertEquals(3, PalettizedCuboid.calculatePowerOfTwoExponent(7, false));
        assertEquals(4, PalettizedCuboid.calculatePowerOfTwoExponent(9, false));
        assertEquals(4, PalettizedCuboid.calculatePowerOfTwoExponent(15, false));
        assertEquals(5, PalettizedCuboid.calculatePowerOfTwoExponent(17, false));
        assertEquals(6, PalettizedCuboid.calculatePowerOfTwoExponent(60, false));
        assertEquals(7, PalettizedCuboid.calculatePowerOfTwoExponent(66, false));
    }

    public void testCtor_initFilledCuboid() {
        StringTag fillTag = new StringTag("void");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(4, fillTag);
        assertEquals(4 * 4 * 4, cuboid.size());
        assertEquals(4, cuboid.cubeEdgeLength());
        assertEquals(1, cuboid.paletteSize());
        assertTrue(cuboid.packedData.allMatch(0));
        assertNotSame(fillTag, cuboid.palette.get(0));
        assertEquals(fillTag, cuboid.palette.get(0));
    }

    public void testCtr_throwsAppropriatelyWhenGivenNullFill() {
        assertThrowsException(() -> new PalettizedCuboid<>(4, null), NullPointerException.class);
        assertThrowsNoException(() -> new PalettizedCuboid<>(4, StringTag.class, null, true));
    }

    public void testCtor_initFromValueArray_happyCase() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);

        assertEquals(tags.length, cuboid.size());
        assertEquals(2, cuboid.cubeEdgeLength());
        assertEquals(3, cuboid.paletteSize());
        assertEquals(bedrockTag, cuboid.palette.get(0));
        assertNotSame(bedrockTag, cuboid.palette.get(0));
        assertEquals(airTag, cuboid.palette.get(1));
        assertNotSame(airTag, cuboid.palette.get(1));
        assertEquals(stoneTag, cuboid.palette.get(2));
        assertNotSame(stoneTag, cuboid.palette.get(2));
        assertEquals(1, cuboid.packedData.count(0)); // bedrock
        assertEquals(5, cuboid.packedData.count(1)); // air
        assertEquals(2, cuboid.packedData.count(2)); // stone
    }

    public void testCtor_initFromValueArray_notACubicLengthThrows() {
        StringTag airTag = new StringTag("air");
        StringTag[] tags = new StringTag[2 * 2 * 2 + 1];
        Arrays.fill(tags, airTag);
        assertThrowsException(() -> new PalettizedCuboid<>(tags),
                IllegalArgumentException.class,
                s -> s.equals("the cube root of 9 is not an integer!"));
    }

    public void testCtor_initFromValueArray_notAPowerOfTwoCubicLengthThrows() {
        StringTag airTag = new StringTag("air");
        StringTag[] tags = new StringTag[3 * 3 * 3];
        Arrays.fill(tags, airTag);
        assertThrowsException(() -> new PalettizedCuboid<>(tags),
                IllegalArgumentException.class,
                s -> s.equals("3 isn't a power of two!"));
    }

    public void testCtor_initFromValueArray_throwsWhenArrayContainsNulls() {
        StringTag airTag = new StringTag("air");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[7] = null;
        assertThrowsException(() -> new PalettizedCuboid<>(tags),
                IllegalArgumentException.class,
                s -> s.equals("values must not contain nulls!"));

    }

    public void testContains() {
        StringTag airTag = new StringTag("air");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);

        assertTrue(cuboid.contains(airTag));
        assertFalse(cuboid.contains(new StringTag("grass")));
    }

    public void testCountIf() {
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = new StringTag("a");
        tags[1] = new StringTag("b");
        tags[2] = new StringTag("c");
        tags[3] = new StringTag("d");
        tags[4] = new StringTag("e");
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);

        assertEquals(0, cuboid.countIf(new StringTag("lava")::equals));  // exercises shortcut case
        assertEquals(1, cuboid.countIf(airTag::equals));  // exercises singleton case
        assertEquals(7, cuboid.countIf(e -> !e.equals(airTag)));  // exercises hashset case
        assertEquals(4, cuboid.countIf(e -> e.getValue().charAt(0) <= 'c'));  // exercises array list case (4 cuz air)

        assertThrowsException(() -> cuboid.countIf(e -> {e.setValue("boom"); return false;}),
                PalettizedCuboid.PaletteCorruptedException.class);

        assertThrowsException(() -> cuboid.countIf(e -> {cuboid.set(0, new StringTag("zap")); return false;}),
                ConcurrentModificationException.class);
    }

    public void testToArray() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);

        StringTag[] arr = cuboid.toArray();
        assertEquals(cuboid.size(), arr.length);
        assertNotSame(cuboid.palette.get(0), arr[0]);
        assertEquals(cuboid.palette.get(0), arr[0]);
        assertNotSame(bedrockTag, arr[0]);

        assertEquals(bedrockTag, arr[0]);
        assertEquals(airTag, arr[1]);
        assertEquals(airTag, arr[2]);
        assertEquals(airTag, arr[3]);
        assertEquals(airTag, arr[4]);
        assertEquals(airTag, arr[5]);
        assertEquals(stoneTag, arr[6]);
        assertEquals(stoneTag, arr[7]);
        assertNotSame(arr[6], arr[7]);
    }

    public void testToArrayByRef() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);

        StringTag[] arr = cuboid.toArrayByRef();
        assertEquals(cuboid.size(), arr.length);
        assertSame(cuboid.palette.get(0), arr[0]);
        assertNotSame(bedrockTag, arr[0]);

        assertEquals(bedrockTag, arr[0]);
        assertEquals(airTag, arr[1]);
        assertEquals(airTag, arr[2]);
        assertEquals(airTag, arr[3]);
        assertEquals(airTag, arr[4]);
        assertEquals(airTag, arr[5]);
        assertEquals(stoneTag, arr[6]);
        assertEquals(stoneTag, arr[7]);
        assertSame(arr[6], arr[7]);
    }

    public void testReplace() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);
        assertEquals(3, cuboid.palette.size());
        assertEquals(2, cuboid.packedData.get(7));  // validate test assumption

        assertTrue(cuboid.replace(stoneTag, bedrockTag));
        assertNotSame(bedrockTag, cuboid.getByRef(7));
        assertEquals(bedrockTag, cuboid.getByRef(7));
        assertEquals(3, cuboid.countIf(bedrockTag::equals));

        assertEquals(3, cuboid.palette.size());
        assertEquals(1, cuboid.palette.stream().filter(bedrockTag::equals).count());
        assertTrue(cuboid.palette.contains(bedrockTag));
        assertTrue(cuboid.palette.contains(airTag));
        assertFalse(cuboid.palette.contains(stoneTag));
        assertEquals(0, cuboid.packedData.get(7));  // validate data remapped to exiting palette index

        assertFalse(cuboid.replace(stoneTag, bedrockTag));  // check not found case
        assertFalse(cuboid.replace(airTag, airTag));  // check old == new case reports no changes
    }

    public void testReplaceAll() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag lavaTag = new StringTag("lava");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);
        assertEquals(2, cuboid.packedData.get(7));  // validate test assumption

        assertTrue(cuboid.replaceAll(new StringTag[] {stoneTag, bedrockTag}, lavaTag));
        assertNotSame(lavaTag, cuboid.getByRef(7));
        assertEquals(lavaTag, cuboid.getByRef(7));
        assertEquals(3, cuboid.countIf(lavaTag::equals));

        assertEquals(4, cuboid.palette.size());
        assertFalse(cuboid.palette.contains(bedrockTag));
        assertTrue(cuboid.palette.contains(airTag));
        assertFalse(cuboid.palette.contains(stoneTag));
        assertTrue(cuboid.palette.contains(lavaTag));
        assertEquals(3, cuboid.packedData.get(7));  // validate data remapped to exiting palette index

        assertFalse(cuboid.replaceAll(Collections.emptyList(), bedrockTag));  // check empty case
        assertFalse(cuboid.replaceAll(Collections.singleton(stoneTag), bedrockTag));  // check not found case
        assertFalse(cuboid.replaceAll(new StringTag[] {airTag}, airTag));  // check old == new case reports no changes
    }

    public void testReplaceIf() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag lavaTag = new StringTag("lava");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);
        assertEquals(2, cuboid.packedData.get(7));  // validate test assumption

        assertTrue(cuboid.replaceIf(e -> e.getValue().contains("o"), lavaTag));
        assertNotSame(lavaTag, cuboid.getByRef(7));
        assertEquals(lavaTag, cuboid.getByRef(7));
        assertEquals(3, cuboid.countIf(lavaTag::equals));

        assertEquals(4, cuboid.palette.size());
        assertFalse(cuboid.palette.contains(bedrockTag));
        assertTrue(cuboid.palette.contains(airTag));
        assertFalse(cuboid.palette.contains(stoneTag));
        assertTrue(cuboid.palette.contains(lavaTag));
        assertEquals(3, cuboid.packedData.get(7));  // validate data remapped to exiting palette index

        assertFalse(cuboid.replace(stoneTag, bedrockTag));  // check not found case
        assertFalse(cuboid.replaceAll(new StringTag[] {airTag}, airTag));  // check old == new case reports no changes

        assertThrowsException(() -> cuboid.replaceIf(e -> {e.setValue("bad"); return true;}, new StringTag("ok")),
                PalettizedCuboid.PaletteCorruptedException.class);

        assertThrowsException(() -> cuboid.replaceIf(e -> {cuboid.set(0, new StringTag("zap")); return true;}, new StringTag("good")),
                ConcurrentModificationException.class);
    }

    public void testRetainAll() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag lavaTag = new StringTag("lava");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);
        assertEquals(2, cuboid.packedData.get(7));  // validate test assumption

        assertTrue(cuboid.retainAll(new StringTag[] {airTag}, lavaTag));
        assertNotSame(lavaTag, cuboid.getByRef(7));
        assertEquals(lavaTag, cuboid.getByRef(7));
        assertEquals(3, cuboid.countIf(lavaTag::equals));

        assertEquals(4, cuboid.palette.size());
        assertFalse(cuboid.palette.contains(bedrockTag));
        assertTrue(cuboid.palette.contains(airTag));
        assertFalse(cuboid.palette.contains(stoneTag));
        assertTrue(cuboid.palette.contains(lavaTag));
        assertEquals(3, cuboid.packedData.get(7));  // validate data remapped to exiting palette index

        assertFalse(cuboid.replace(stoneTag, bedrockTag));  // check not found case
        assertFalse(cuboid.replaceAll(new StringTag[] {airTag}, airTag));  // check old == new case reports no changes
    }

    public void testFill() {
        StringTag bedrockTag = new StringTag("bedrock");
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag lavaTag = new StringTag("lava");
        StringTag[] tags = new StringTag[2 * 2 * 2];
        Arrays.fill(tags, airTag);
        tags[0] = bedrockTag;
        tags[6] = stoneTag;
        tags[7] = stoneTag;
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(tags);
        cuboid.fill(lavaTag);
        assertEquals(8, cuboid.countIf(lavaTag::equals));
        assertEquals(1, cuboid.paletteSize());
        assertNotSame(lavaTag, cuboid.palette.get(0));
        assertEquals(lavaTag, cuboid.palette.get(0));
    }

    public void testIndexOfXyzLiterals() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(
                16, new StringTag("air"));
        assertEquals(4096, cuboid.size());
        assertEquals(0, cuboid.indexOf(0, 0, 0));
        assertEquals(4095, cuboid.indexOf(15, 15, 15));
        assertEquals(0, cuboid.indexOf(16, 16, 16));

        assertEquals(834, cuboid.indexOf(2, 3, 4));
        assertEquals(cuboid.indexOf(2, 3, 4), cuboid.indexOf(16 + 2, 16 + 3, 16 + 4));
    }

    public void testIndexOfIntPointXYZ() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(
                16, new StringTag("air"));
        assertEquals(4096, cuboid.size());
        assertEquals(0, cuboid.indexOf(new IntPointXYZ(0, 0, 0)));
        assertEquals(4095, cuboid.indexOf(new IntPointXYZ(15, 15, 15)));
        assertEquals(0, cuboid.indexOf(new IntPointXYZ(16, 16, 16)));

        assertEquals(834, cuboid.indexOf(new IntPointXYZ(2, 3, 4)));
        assertEquals(cuboid.indexOf(2, 3, 4), cuboid.indexOf(new IntPointXYZ(16 + 2, 16 + 3, 16 + 4)));
    }

    public void testXyzOf_xyzLiterals() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(
                16, new StringTag("air"));
        assertEquals(new IntPointXYZ(0, 0, 0), cuboid.xyzOf(16, 16, 16));
        assertEquals(new IntPointXYZ(1, 6, 4), cuboid.xyzOf(65, 70, 84));
    }

    public void testXyzOf_index() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(
                16, new StringTag("air"));
        assertEquals(new IntPointXYZ(0, 0, 0), cuboid.xyzOf(0));
        assertEquals(new IntPointXYZ(2, 3, 4), cuboid.xyzOf(834));
    }

    public void testGet() {
        StringTag airTag = new StringTag("air");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, airTag);
        assertEquals(airTag, cuboid.get(0));
        assertNotSame(airTag, cuboid.get(0));
        assertNotSame(cuboid.palette.get(0), cuboid.get(0));
    }

    public void testGetByRef() {
        StringTag airTag = new StringTag("air");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, airTag);
        assertEquals(airTag, cuboid.getByRef(0));
        assertNotSame(airTag, cuboid.getByRef(0));
        assertSame(cuboid.palette.get(0), cuboid.getByRef(10, 11, 12));
    }

    public void testSet() {
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag lavaTag = new StringTag("lava");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, airTag);
        cuboid.set(0, 0, 0, lavaTag);
        cuboid.set(0, 1, 0, lavaTag);
        assertEquals(6, cuboid.countIf(airTag::equals));
        assertEquals(lavaTag, cuboid.get(0, 0, 0));
        assertEquals(lavaTag, cuboid.get(0, 1, 0));
        assertNotSame(lavaTag, cuboid.get(0, 0, 0));
        assertNotSame(lavaTag, cuboid.get(0, 1, 0));

        cuboid.set(0, stoneTag);
        assertEquals(stoneTag, cuboid.get(0, 0, 0));
        // note that using set to replace all of one palette key will NOT remove that key from the palette
        // optimizePalette will clean those up - tested in #testOptimizePalette
    }

    public void testSet_throwsWhenIndexOutOfBounds() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("air"));
        assertThrowsException(() -> cuboid.set(-1, new StringTag("bam")), IndexOutOfBoundsException.class);
        assertThrowsException(() -> cuboid.set(8, new StringTag("bam")), IndexOutOfBoundsException.class);
    }

    public void testSetRange_fullVolume_isTheSameAsFill() {
        StringTag airTag = new StringTag("air");
        StringTag lavaTag = new StringTag("lava");
        StringTag stoneTag = new StringTag("stone");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(4, airTag);
        cuboid.set(0, 0, 0, lavaTag);
        cuboid.set(1, 1, 1, lavaTag);
        cuboid.set(2, 2, 2, stoneTag);
        cuboid.set(3, 3, 3, lavaTag);

        cuboid.set(IntPointXYZ.XYZ(0, 0, 0), stoneTag, IntPointXYZ.XYZ(3, 3, 3));  // exercise the wrapped call at least once
        assertEquals(1, cuboid.paletteSize());
        assertEquals(64, cuboid.countIf(stoneTag::equals));
    }

    public void testSetRange_fullVolume_XZPlainFill() {
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(16, airTag);

        cuboid.set(0, 6, 0, stoneTag, 15, 8, 15);
        assertEquals(16 * 16 * 3, cuboid.countIf(stoneTag::equals));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        while (iter.hasNext()) {
            iter.next();
            int y = iter.currentY();
            if (y < 6 || y > 8) {
                assertEquals(iter.current(), airTag);
            } else {
                assertEquals(iter.current(), stoneTag);
            }
        }
    }

    public void testSetRange_requiresElementArg() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("air"));
        assertThrowsException(() -> cuboid.set(0, 0, 0, null, 0, 0,0), IllegalArgumentException.class);
    }

    public void testSetRange_throwsIfCoordsAreOutOfBounds() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("air"));

        assertThrowsException(() -> cuboid.set(0, 0, -6, new StringTag("air"), 0, 0, 0), IndexOutOfBoundsException.class);
        assertThrowsException(() -> cuboid.set(0, -6, 0, new StringTag("air"), 0, 0, 0), IndexOutOfBoundsException.class);
        assertThrowsException(() -> cuboid.set(-6, 0, 0,  new StringTag("air"), 0, 0, 0), IndexOutOfBoundsException.class);
        assertThrowsException(() -> cuboid.set(0, 0, 0, new StringTag("air"), 3, 0, 0), IndexOutOfBoundsException.class);
        assertThrowsException(() -> cuboid.set(0, 0, 0, new StringTag("air"), 0, 3, 0), IndexOutOfBoundsException.class);
        assertThrowsException(() -> cuboid.set(0, 0, 0,  new StringTag("air"), 0, 0, 3), IndexOutOfBoundsException.class);
    }

    public void testSetRange_fullVolume_singleVoxel() {
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(16, airTag);

        cuboid.set(0, 6, 0, stoneTag, 0, 6, 0);
        assertEquals(1, cuboid.countIf(stoneTag::equals));
        assertEquals(stoneTag, cuboid.get(0, 6, 0));
    }

    public void testSetRange_fullVolume_cuboid() {
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(16, airTag);

        cuboid.set(14, 14, 14, stoneTag, 1, 1, 1);  // require swapping all coords
        assertEquals(14 * 14 * 14, cuboid.countIf(stoneTag::equals));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        assertEquals(airTag, cuboid.get(0, 0, 0));
        assertEquals(airTag, cuboid.get(0, 0, 15));
        assertEquals(airTag, cuboid.get(0, 15, 0));
        assertEquals(airTag, cuboid.get(0, 15, 15));
        assertEquals(airTag, cuboid.get(15, 0, 0));
        assertEquals(airTag, cuboid.get(15, 0, 15));
        assertEquals(airTag, cuboid.get(15, 15, 0));
        assertEquals(airTag, cuboid.get(15, 15, 15));

        assertEquals(stoneTag, cuboid.get(1, 1, 1));
        assertEquals(stoneTag, cuboid.get(1, 1, 14));
        assertEquals(stoneTag, cuboid.get(1, 14, 1));
        assertEquals(stoneTag, cuboid.get(1, 14, 14));
        assertEquals(stoneTag, cuboid.get(14, 1, 1));
        assertEquals(stoneTag, cuboid.get(14, 1, 14));
        assertEquals(stoneTag, cuboid.get(14, 14, 1));
        assertEquals(stoneTag, cuboid.get(14, 14, 14));
    }

    public void testOptimizePalette() {
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag lavaTag = new StringTag("lava");
        StringTag bedrockTag = new StringTag("bedrock");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, airTag);
        cuboid.set(1, 0, 0, stoneTag);
        cuboid.set(1, 1, 0, stoneTag);
        cuboid.set(1, 1, 1, lavaTag);
        cuboid.set(0, 0, 0, bedrockTag);

        assertArrayEquals(new int[] {3, 1, 0, 0, 0, 1, 0, 2}, cuboid.packedData.toArray());

        // test what should be a no-op
        int[] originalData = cuboid.packedData.toArray();
        StringTag[] originalPalette = new StringTag[cuboid.palette.size()];
        for (int i = 0; i < originalPalette.length; i++) {
            originalPalette[i] = cuboid.palette.get(i).clone();
        }
        cuboid.optimizePalette();
        assertArrayEquals(originalData, cuboid.packedData.toArray());
        assertArrayEquals(originalPalette, cuboid.palette.toArray());
        assertEquals(bedrockTag, cuboid.get(0));

        // start mutating
        cuboid.replace(stoneTag, airTag);
        cuboid.set(1, 1, 1, airTag);
        assertEquals(4, cuboid.paletteSize());

        cuboid.optimizePalette();  // id, null, null, id
        assertEquals(2, cuboid.paletteSize());
        assertEquals(airTag, cuboid.palette.get(0));
        assertEquals(bedrockTag, cuboid.palette.get(1));
        assertArrayEquals(new int[] {1, 0, 0, 0, 0, 0, 0, 0}, cuboid.packedData.toArray());

        // test trailing null in palette
        cuboid.set(0, 0, 0, airTag);
        cuboid.optimizePalette();  // id, null
        assertEquals(1, cuboid.paletteSize());
        assertEquals(airTag, cuboid.palette.get(0));
        assertArrayEquals(new int[] {0, 0, 0, 0, 0, 0, 0, 0}, cuboid.packedData.toArray());
    }

    public void testClone() {
        StringTag airTag = new StringTag("air");
        StringTag stoneTag = new StringTag("stone");
        StringTag lavaTag = new StringTag("lava");
        StringTag bedrockTag = new StringTag("bedrock");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, airTag);
        cuboid.set(1, 0, 0, stoneTag);
        cuboid.set(1, 1, 0, stoneTag);
        cuboid.set(1, 1, 1, lavaTag);
        cuboid.set(0, 0, 0, bedrockTag);

        assertEquals(bedrockTag, cuboid.get(0));
        PalettizedCuboid<StringTag> cuboid2 = cuboid.clone();

        assertNotSame(cuboid, cuboid2);
        assertEquals(cuboid.size(), cuboid2.size());
        assertEquals(cuboid.cubeEdgeLength(), cuboid2.cubeEdgeLength());
        assertEquals(cuboid.paletteSize(), cuboid2.paletteSize());

        assertArrayEquals(cuboid.packedData.toArray(), cuboid2.packedData.toArray());
        assertEquals(bedrockTag, cuboid.get(0));
        assertEquals(bedrockTag, cuboid2.get(0));

        assertEquals(cuboid.palette.get(0), cuboid2.palette.get(0));
        assertNotSame(cuboid.palette.get(0), cuboid2.palette.get(0));
    }

    public void testCompoundTag_toAndFrom_moreThanOnePaletteIdInUse() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(4, new StringTag("desert"));
        cuboid.set(0, 0, 0, new StringTag("beach"));
        cuboid.set(0, 1, 0, new StringTag("beach"));
        cuboid.set(1, 1, 0, new StringTag("flower_forest"));
        cuboid.set(1, 0, 1, new StringTag("beach"));
        cuboid.set(0, 2, 2, new StringTag("flower_forest"));
        cuboid.set(2, 3, 3, new StringTag("river"));
        cuboid.set(3, 3, 2, new StringTag("jumaji"));
        cuboid.set(3, 3, 3, new StringTag("jumaji"));

        CompoundTag serializedTag = cuboid.toCompoundTag();
        assertNotNull(serializedTag);
        assertTrue(serializedTag.containsKey("palette"));
        assertTrue(serializedTag.containsKey("data"));
        assertTrue(serializedTag.get("palette") instanceof ListTag);
        assertTrue(serializedTag.get("data") instanceof LongArrayTag);

//        System.out.println(JsonPrettyPrinter.prettyPrintJson(serializedTag.toString()));
        PalettizedCuboid<StringTag> cuboid2 = PalettizedCuboid.fromCompoundTag(serializedTag, 4);
        assertNotNull(cuboid2);
        assertEquals(cuboid.size(), cuboid2.size());
        assertEquals(cuboid.cubeEdgeLength(), cuboid2.cubeEdgeLength());
        assertArrayEquals(cuboid.packedData.toArray(), cuboid2.packedData.toArray());
        assertEquals(cuboid.paletteSize(), cuboid2.paletteSize());
        for (int i = 0; i < cuboid.palette.size(); i++) {
            assertEquals(cuboid.palette.get(i), cuboid2.palette.get(i));
        }
    }


    public void testToCompoundTag_singleElement() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(4, new StringTag("dripstone_caves"));
        CompoundTag tag = cuboid.toCompoundTag();
        assertEquals(1, tag.size());
        assertEquals(1, tag.getListTag("palette").size());
        assertEquals(new StringTag("dripstone_caves"), tag.getListTag("palette").get(0));
    }


    public void testToCompoundTag_minimumBitsPerIndex() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(16, new StringTag("air"));
        cuboid.set(42, new StringTag("answers"));

        // when not providing a minimum bits per index, for a 16^3 cuboid, 4 bits per index is assumed
        CompoundTag tag = cuboid.toCompoundTag();
        assertEquals(2, tag.size());
        assertEquals(2, tag.getListTag("palette").size());
        assertEquals(256, tag.getLongArrayTag("data").length());
        assertEquals(new StringTag("air"), tag.getListTag("palette").get(0));
        assertEquals(new StringTag("answers"), tag.getListTag("palette").get(1));

        // we can override this detected default
        tag = cuboid.toCompoundTag(0, 1);
        assertEquals(2, tag.size());
        assertEquals(2, tag.getListTag("palette").size());
        assertEquals(64, tag.getLongArrayTag("data").length());
        assertEquals(new StringTag("air"), tag.getListTag("palette").get(0));
        assertEquals(new StringTag("answers"), tag.getListTag("palette").get(1));
    }

    public void testFromCompoundTag_returnsNullWhenGivenNullTag() {
        assertNull(PalettizedCuboid.fromCompoundTag(null, 4));
    }

    public void testFromCompoundTag_singleElement() {
        ListTag<StringTag> paletteTag = new ListTag<>(StringTag.class);
        CompoundTag rootTag = new CompoundTag();
        rootTag.put("palette", paletteTag);
        paletteTag.add(new StringTag("dripstone_caves"));
        PalettizedCuboid<StringTag> cuboid = PalettizedCuboid.fromCompoundTag(rootTag, 4);
        assertNotNull(cuboid);
        assertEquals(64, cuboid.size());
        assertEquals(1, cuboid.paletteSize());
        assertEquals(64, cuboid.countIf(e -> e.getValue().equals("dripstone_caves")));
    }

    public void testFromCompoundTag_missingDataTagThrows() {
        ListTag<StringTag> paletteTag = new ListTag<>(StringTag.class);
        CompoundTag rootTag = new CompoundTag();
        rootTag.put("palette", paletteTag);
        paletteTag.add(new StringTag("a"));
        paletteTag.add(new StringTag("b"));
        assertThrowsException(() -> PalettizedCuboid.fromCompoundTag(rootTag, 4), IllegalArgumentException.class);
    }

    public void testIterator_happyCase() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        cuboid.set(0, 0, 0, new StringTag("000"));
        cuboid.set(1, 0, 0, new StringTag("100"));
        cuboid.set(0, 0, 1, new StringTag("001"));
        cuboid.set(1, 0, 1, new StringTag("101"));
        cuboid.set(0, 1, 0, new StringTag("010"));
        cuboid.set(1, 1, 0, new StringTag("110"));
        cuboid.set(0, 1, 1, new StringTag("011"));
        cuboid.set(1, 1, 1, new StringTag("111"));

        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        assertTrue(iter.hasNext());
        assertEquals("000", iter.next().getValue());
        assertEquals("000", iter.current().getValue());
        assertEquals(0, iter.currentIndex());
        assertEquals(IntPointXYZ.XYZ(0, 0, 0), iter.currentXYZ());
        assertTrue(iter.hasNext());
        assertEquals("100", iter.next().getValue());
        assertEquals(1, iter.currentIndex());
        assertTrue(iter.hasNext());
        assertEquals("001", iter.next().getValue());
        assertEquals(0, iter.currentX());
        assertTrue(iter.hasNext());
        assertEquals("101", iter.next().getValue());
        assertEquals(IntPointXYZ.XYZ(1, 0, 1), iter.currentXYZ());
        assertEquals(1, iter.currentX());
        assertEquals(0, iter.currentY());
        assertEquals(1, iter.currentZ());
        assertTrue(iter.hasNext());
        assertEquals("010", iter.next().getValue());
        assertEquals(1, iter.currentY());
        assertEquals(0, iter.currentZ());
        assertTrue(iter.hasNext());
        assertEquals("110", iter.next().getValue());
        assertTrue(iter.hasNext());
        assertEquals("011", iter.next().getValue());
        assertTrue(iter.hasNext());
        assertEquals("111", iter.next().getValue());
        assertEquals("111", iter.current().getValue());
        assertFalse(iter.hasNext());
    }

    public void testIterator_nextXYZ() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        assertEquals(IntPointXYZ.XYZ(0, 0, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(1, 0, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(0, 0, 1), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(1, 0, 1), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(0, 1, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(1, 1, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(0, 1, 1), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(1, 1, 1), iter.nextXYZ());
        assertFalse(iter.hasNext());
    }

    public void testIterator_current_ThrowsNoSuchElementExceptionIfNextHasNeverBeenCalled() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        assertThrowsException(iter::current, NoSuchElementException.class);
    }

    public void testIterator_filtered_current_ThrowsNoSuchElementExceptionIfNextHasNeverBeenCalled() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        StringTag tag = new StringTag("abba");
        cuboid.set(1, tag);
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator(tag::equals);
        assertThrowsException(iter::current, NoSuchElementException.class);
    }

    public void testIterator_currentIndex_ThrowsNoSuchElementExceptionIfNextHasNeverBeenCalled() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        assertThrowsException(iter::currentIndex, NoSuchElementException.class);
    }

    public void testIterator_currentXYZ_ThrowsNoSuchElementExceptionIfNextHasNeverBeenCalled() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        assertThrowsException(iter::currentXYZ, NoSuchElementException.class);
        assertThrowsException(iter::currentX, NoSuchElementException.class);
        assertThrowsException(iter::currentY, NoSuchElementException.class);
        assertThrowsException(iter::currentZ, NoSuchElementException.class);
    }

    public void testIterator_next_ThrowsNoSuchElementExceptionIfThereAreNoMoreElements() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        while (iter.hasNext()) iter.next();
        assertThrowsException(iter::next, NoSuchElementException.class);
    }

    public void testIterator_filtered_next_ThrowsNoSuchElementExceptionIfThereAreNoMoreElements() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        StringTag tag = new StringTag("abba");
        cuboid.set(1, tag);
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator(tag::equals);
        while (iter.hasNext()) iter.next();
        assertThrowsException(iter::next, NoSuchElementException.class);
    }

    public void testIterator_PaletteCorruptedExceptionThrown() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        iter.next().setValue("boom");
        assertThrowsException(iter::hasNext, PalettizedCuboid.PaletteCorruptedException.class);
    }

    public void testIterator_ConcurrentModificationExceptionThrown() {
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, new StringTag("xxxx"));
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        cuboid.set(0, 1, 0, new StringTag("010"));
        assertThrowsException(iter::hasNext, ConcurrentModificationException.class);
    }

    public void testIterator_set_canSafelyModify() {
        StringTag fillTag = new StringTag("xxxx");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, fillTag);
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        iter.next();
        iter.set(new StringTag("a"));
        iter.next();
        iter.next();
        iter.set(new StringTag("b"));
        assertTrue(iter.hasNext());
        assertEquals(6, cuboid.countIf(fillTag::equals));
        assertTrue(cuboid.contains(new StringTag("a")));
        assertTrue(cuboid.contains(new StringTag("b")));
    }

    public void testIterator_set_throwsIfCalledBeforeNext() {
        StringTag fillTag = new StringTag("xxxx");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, fillTag);
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        assertThrowsException(() -> iter.set(new StringTag("a")), NoSuchElementException.class);
    }

    public void testIterator_set_detectsCommonPaletteCorruptionCase() {
        StringTag fillTag = new StringTag("xxxx");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, fillTag);
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        StringTag tag = iter.next();
        tag.setValue("boom");
        assertThrowsException(() -> iter.set(tag), PalettizedCuboid.PaletteCorruptedException.class);
    }

    public void testIterator_set_detectsConcurrentModificationCase() {
        StringTag fillTag = new StringTag("xxxx");
        PalettizedCuboid<StringTag> cuboid = new PalettizedCuboid<>(2, fillTag);
        PalettizedCuboid<StringTag>.CursorIterator iter = cuboid.iterator();
        iter.next();
        cuboid.set(0, 1, 0, new StringTag("pop"));
        assertThrowsException(() -> iter.set(new StringTag("a")), ConcurrentModificationException.class);
    }


    // Interesting because bit packing has no waste on the per-long level, 4 bits to encode size 14 palette data.
    public void testFromCompoundTag_blockStates_1_20_4__14entries() {
        CompoundTag tag = (CompoundTag) deserializeFromFile("mca_palettes/block_states-1.20.4-14entries.snbt").getTag();
//        System.out.println(JsonPrettyPrinter.prettyPrintJson(tag.toString()));
        PalettizedCuboid<CompoundTag> cuboid = PalettizedCuboid.fromCompoundTag(tag, 16, DataVersion.JAVA_1_20_4.id());
        assertNotNull(cuboid);
        assertEquals(4096, cuboid.size());
        assertEquals(14, cuboid.paletteSize());
        assertEquals(2, cuboid.countIf(e -> e.getString("Name").equals("minecraft:glow_lichen")));
        PalettizedCuboid<CompoundTag>.CursorIterator iter =
                cuboid.iterator(e -> e.getString("Name").equals("minecraft:glow_lichen"));
        assertTrue(iter.hasNext());
        iter.next();
        assertEquals(cuboid.xyzOf(8, 33, 15), iter.currentXYZ());
        assertTrue(iter.hasNext());
        iter.next();
        assertEquals(cuboid.xyzOf(9, 33, 15), iter.currentXYZ());
        assertFalse(iter.hasNext());
    }

    // Interesting because only 3 bits are required to encode size 6 palette data, but it actually uses 4 bits.
    public void testFromCompoundTag_blockStates_1_20_4__6entries() {
        CompoundTag tag = (CompoundTag) deserializeFromFile("mca_palettes/block_states-1.20.4-6entries.snbt").getTag();
//        System.out.println(JsonPrettyPrinter.prettyPrintJson(tag.toString()));
        PalettizedCuboid<CompoundTag> cuboid = PalettizedCuboid.fromCompoundTag(tag, 16, DataVersion.JAVA_1_20_4.id());
        assertNotNull(cuboid);
        assertEquals(4096, cuboid.size());
        assertEquals(6, cuboid.paletteSize());
        assertEquals(1, cuboid.countIf(e -> e.getString("Name").equals("minecraft:dripstone_block")));
        assertEquals(62, cuboid.countIf(e -> e.getString("Name").equals("minecraft:coal_ore")));

    }

    public void testFromCompoundTag_blockStates_1_20_4__72entries() throws IOException {
        // interesting because it's the largest chunk section found in a random world's region file.
        CompoundTag tag = (CompoundTag) deserializeFromFile("mca_palettes/block_states-1.20.4-r.0.0_X6Y-3Z23_72entries.snbt").getTag();
//        System.out.println(JsonPrettyPrinter.prettyPrintJson(tag.toString()));
        PalettizedCuboid<CompoundTag> cuboid = PalettizedCuboid.fromCompoundTag(tag, 16, DataVersion.JAVA_1_20_4.id());
        assertNotNull(cuboid);
        assertEquals(4096, cuboid.size());
        assertEquals(72, cuboid.paletteSize());
        PalettizedCuboid<CompoundTag>.CursorIterator iter =
                cuboid.iterator(e -> e.getString("Name").equals("minecraft:sticky_piston"));
        assertTrue(iter.hasNext());
        IntPointXYZ subChunkLocation = new IntPointXYZ(6, -3, 23);
        IntPointXYZ blockAbsoluteLocation = subChunkLocation.transformChunkSectionToBlock().add(iter.nextXYZ());
        assertEquals(new IntPointXYZ(99, -48, 372), blockAbsoluteLocation);
        assertFalse(iter.hasNext());
        assertEquals("west", iter.current().getCompoundTag("Properties").getString("facing"));
        assertTrue(iter.current().getCompoundTag("Properties").getBoolean("extended"));
    }

    public void testFromCompoundTag_biomes_1_20_4__6entries() throws IOException {
        // interesting because it highlights the need to compute bits per index from palette size and not from the
        // number of longs - computing from number of longs gives the wrong answer because this sample overflows
        // 3 longs by as single record.
        CompoundTag tag = (CompoundTag) deserializeFromFile("mca_palettes/biomes-1.20.4-r.0.0_X21Y-3Z3_6entries.snbt").getTag();
//        System.out.println(JsonPrettyPrinter.prettyPrintJson(tag.toString()));
        PalettizedCuboid<StringTag> cuboid = PalettizedCuboid.fromCompoundTag(tag, 4, DataVersion.JAVA_1_20_4.id());
        assertNotNull(cuboid);
        assertEquals(64, cuboid.size());
        assertEquals(6, cuboid.paletteSize());
        assertEquals(6, cuboid.countIf(e -> e.getValue().equals("minecraft:dripstone_caves")));
        PalettizedCuboid<StringTag>.CursorIterator iter =
                cuboid.iterator(e -> e.getValue().equals("minecraft:dripstone_caves"));
        assertEquals(IntPointXYZ.XYZ(0, 2, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(0, 3, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(1, 3, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(2, 3, 0), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(0, 3, 1), iter.nextXYZ());
        assertEquals(IntPointXYZ.XYZ(1, 3, 1), iter.nextXYZ());
        assertFalse(iter.hasNext());
    }

    public void testFromCompoundTag_biomes_1_20_4__2entries() throws IOException {
        // interesting because entry count is exactly a power of 2
        CompoundTag tag = (CompoundTag) deserializeFromFile("mca_palettes/biomes-1.20.4-r.-3.-3_X-91Y-2Z-87_2entries.snbt").getTag();
//        System.out.println(JsonPrettyPrinter.prettyPrintJson(tag.toString()));
        PalettizedCuboid<StringTag> cuboid = PalettizedCuboid.fromCompoundTag(tag, 4, DataVersion.JAVA_1_20_4.id());
//        System.out.println(cuboid);

        assertNotNull(cuboid);
        assertEquals(64, cuboid.size());
        assertEquals(2, cuboid.paletteSize());
        assertEquals(24, cuboid.countIf(e -> e.getValue().equals("minecraft:dripstone_caves")));
        assertEquals(64 - 24, cuboid.countIf(e -> e.getValue().equals("minecraft:savanna")));
    }
}
