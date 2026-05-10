package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.NbtTestCase;
import io.github.ensgijs.nbt.mca.DataVersion;
import io.github.ensgijs.nbt.tag.LongArrayTag;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import static io.github.ensgijs.nbt.mca.util.LongArrayTagPackedIntegers.PackingStrategy.NO_SPLIT_VALUES_ACROSS_LONGS;
import static io.github.ensgijs.nbt.mca.util.LongArrayTagPackedIntegers.PackingStrategy.SPLIT_VALUES_ACROSS_LONGS;
import static io.github.ensgijs.nbt.mca.util.LongArrayTagPackedIntegers.calculateBitsRequired;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

public class LongArrayTagPackedIntegersTest extends NbtTestCase {

    // <editor-fold desc="Test data" defaultstate="collapsed">
    private long[] getSplitValuesAcrossLongsTestData() {
        return new long[] {
                -7905747598138111418L,
                -4034096956261382364L,
                -1985479300307607665L,
                160190676332679752L,
                3537733969192683524L,
                2635249149083706482L,
                -8048741705815158215L,
                5198158411513084188L,
                2563050546444967058L,
                1263510874847481933L,
                -3952908983306664157L,
                -6565560818798419055L,
                -7910268677794852407L,
                -9134254101975906012L,
                4079221292204147954L,
                -7184891145390624151L,
                5631489967097282880L,
                2563051097278199968L,
                1317800911811943499L,
                -3925781591302844377L,
                -6565560818798419438L,
                -3282780392202497399L,
                6426804121022679460L,
                4079221292212511922L,
                -7472277643142573455L,
                5703689948425773360L,
                2563332572253333652L,
                1461986468094581325L,
                658829914959925032L,
                2662306004410966930L,
                -6750565342366886838L,
                7581981832104790308L,
                4365763922339947762L,
                -7039368025146592647L,
                5703689948425777464L,
                2635672192637025432L};
    }
    private final String expectedSplitAcrossLongsGrid = """ 
            70 69 69 69 71 73 73 73 71 71 71 70 64 64 63 63
            70 70 70 69 71 71 73 71 71 73 71 71 64 64 64 63
            77 71 70 70 71 71 71 71 73 73 73 71 65 64 64 64
            77 72 71 71 71 70 70 71 72 73 71 71 71 71 71 71
            77 72 72 72 71 71 70 70 71 71 71 71 71 73 71 71
            76 78 78 78 78 77 71 71 70 70 69 71 73 73 73 71
            74 78 79 80 79 78 72 78 78 78 78 77 71 73 72 71
            74 78 80 80 80 78 73 78 78 80 78 78 75 75 71 71
            75 78 79 80 79 78 73 78 80 80 80 78 77 76 75 68
            76 78 78 78 78 77 74 78 79 80 78 78 77 77 75 69
            76 76 76 75 75 75 74 78 78 78 78 78 77 75 75 70
            77 76 76 76 78 79 79 79 79 74 74 75 75 75 75 71
            77 77 77 76 79 80 81 80 79 75 74 74 74 73 72 71
            78 78 77 77 79 81 81 81 79 75 75 74 74 73 73 72
            78 78 78 77 79 79 81 80 79 75 79 79 79 79 79 72
            79 79 78 78 78 79 79 79 79 76 79 80 81 79 79 73""";


    private long[] getNoSplitValuesAcrossLongsTestData() {
        return new long[] {
                2490852214246277761L,
                2328405075627938441L,
                2490851869573055105L,
                2328405074959209610L,
                2472520261958959745L,
                2328687099825951881L,
                2328405073883103873L,
                2472802356486804097L,
                2346454725484151433L,
                2472802286691484290L,
                2346454656764678793L,
                2346454725618894466L,
                2346454725484414082L,
                2364504377354159746L,
                2346454725618631811L,
                2364504377354159234L,
                2364504377488640132L,
                2364504377354159747L,
                2382589282315732611L,
                2364504446208116868L,
                2400603749275731587L,
                2382554029224168069L,
                2418547572882605699L,
                2400603680825215621L,
                2364504377354422404L,
                2400603680825478275L,
                2364504446208117381L,
                2400603749678384771L,
                2382554029224168069L,
                2418653125998872195L,
                2382589282315733637L,
                2364504377354160260L,
                2400603680825215622L,
                2364504377488640133L,
                2400603680825476739L,
                2382554029224168069L,
                17616930435L};
    }
    private final String expectedNoSplitAcrossLongsGrid = """
            64 64 72 72 77 73 73 72 72 64 77 64 64 64 64 64
            64 64 72 73 73 73 73 73 72 64 64 64 64 64 64 64
            64 64 72 72 73 73 73 72 72 64 64 64 64 64 64 64
            64 64 72 72 72 73 72 72 72 64 64 64 65 65 65 65
            64 64 64 72 72 72 72 72 64 64 64 65 65 65 66 66
            65 65 65 65 65 65 65 64 65 65 65 65 66 66 66 66
            66 66 66 65 65 65 65 65 65 65 65 66 66 66 66 66
            67 67 67 67 66 66 66 66 66 66 66 66 66 66 66 66
            68 68 68 68 67 67 67 67 67 67 66 66 66 66 66 66
            69 68 68 68 68 68 68 67 67 67 66 66 66 66 66 66
            69 68 68 68 68 68 68 68 67 67 67 66 66 66 66 66
            69 69 68 68 68 68 68 68 67 67 67 66 66 66 66 66
            69 69 68 68 68 68 68 68 67 67 67 66 66 66 66 66
            69 69 68 68 68 68 68 68 67 67 67 66 66 66 66 66
            69 68 68 68 68 68 68 68 67 67 67 66 66 66 66 66
            69 68 68 68 68 68 68 68 68 67 67 67 66 66 66 66""";
    // </editor-fold>

    private int indexOf(int x, int z) {
        return z << 4 | x;
    }

    public void testCalculateBitsRequired() {
        assertEquals(0, calculateBitsRequired(0));
        assertEquals(1, calculateBitsRequired(1));
        assertEquals(2, calculateBitsRequired(2));
        assertEquals(2, calculateBitsRequired(3));
        assertEquals(3, calculateBitsRequired(4));
        assertEquals(3, calculateBitsRequired(7));
        assertEquals(4, calculateBitsRequired(8));
        assertEquals(4, calculateBitsRequired(15));
        assertEquals(5, calculateBitsRequired(16));
        assertEquals(9, calculateBitsRequired(511));
        assertEquals(10, calculateBitsRequired(512));
        assertEquals(31, calculateBitsRequired(Integer.MAX_VALUE));
    }

    public void testBuilder_build_noGivenTag() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .dataVersion(DataVersion.latest())
                .length(64)
                .minBitsPerValue(3)
                .valueOffset(-4)
                .build();
        assertEquals(NO_SPLIT_VALUES_ACROSS_LONGS, packed.getPackingStrategy());
        assertEquals(4, packed.getHandle().length());
        assertEquals(64, packed.length);
        assertEquals(3, packed.getMinBitsPerValue());
        assertEquals(3, packed.getBitsPerValue());
        assertEquals(-4, packed.getValueOffset());
        assertEquals((int) Math.pow(2, 3) - 1 - 4, packed.getCurrentMaxPackableValue());
        assertEquals(0, packed.getActualUsedBitsPerValue());
        assertSame(packed.getHandle().getValue(), packed.longs());
    }

    public void testBuilder_build_packingStrategyHasDefault() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .length(64)
                .minBitsPerValue(1)
                .build();
        assertEquals(NO_SPLIT_VALUES_ACROSS_LONGS, packed.getPackingStrategy());
    }

    public void testBuilder_build_settingDataVersionToZero() {
        var builder = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1);
        builder.dataVersion(0);
        assertEquals(NO_SPLIT_VALUES_ACROSS_LONGS, builder.build().getPackingStrategy());

    }

    public void testBuilder_build_throwsWhenLongArrayTagHasUnexpectedLength() {
        assertThrows(IllegalArgumentException.class, () -> LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(1)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData())));
    }

    public void testBuilder_build_givenValueArray() {
        LongArrayTagPackedIntegers packed1 = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .valueOffset(-65)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        LongArrayTagPackedIntegers packed2 = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .valueOffset(-65)
                .build(packed1.toArray());
        assertEquals(expectedNoSplitAcrossLongsGrid, packed2.toString2dGrid());
    }

    public void testBuilder_build_capacityRequired() {
        var builder = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS);
        assertThrowsException(builder::build, IllegalArgumentException.class, s -> s.contains("capacity"));
    }

    public void testToString3dGrid() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .initializeForStoring(5)
                .build(new LongArrayTag(3785436329631921288L, 3932602464921073299L, 2797418351439529682L, 4L));
        assertEquals("""
            Y=0
            0 1 2 2
            3 3 4 4
            3 4 4 4
            4 4 4 4
            Y=1
            0 1 2 2
            3 3 2 2
            3 3 4 4
            3 4 4 4
            Y=2
            5 1 2 2
            3 3 2 2
            3 3 2 2
            3 3 4 4
            Y=3
            5 5 5 2
            5 5 2 2
            3 3 2 2
            3 3 2 4""", packed.toString3dGrid());
    }

    public void testToString3dGrid_throwsIfLengthDoesNotHaveACubeRoot() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(32)
                .minBitsPerValue(1)
                .build();
        assertThrows(UnsupportedOperationException.class, packed::toString3dGrid);
    }

    public void testToString2dGrid_throwsIfLengthDoesNotHaveASquareRoot() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(32)
                .minBitsPerValue(1)
                .build();
        assertThrows(UnsupportedOperationException.class, packed::toString2dGrid);
    }

    public void testNoSplitValuesAcrossLongs_extraTallWorldHeightmap() {
        // world build height from -512 to 575
        final int chunkBottomSectionY = -32;
        final int chunkTopSectionY = 36;
        long[] longs = new long[] {
                // <editor-fold desc="Test data" defaultstate="collapsed">
                9891638458552882L,
                9856436898204210L,
                9856436898202160L,
                9891638458552880L,
                9856436902398514L,
                9856436898202160L,
                9891638458548784L,
                9856445492333106L,
                9856436898202160L,
                9891638450160176L,
                9856445492333105L,
                9856436898202160L,
                9891621270290992L,
                9856436902398513L,
                9856436898202160L,
                9856436898202160L,
                9856436902398513L,
                9856436898202160L,
                9856436898202160L,
                9856436902398512L,
                10120448600832560L,
                9856436898202160L,
                9856436898202160L,
                11002806832L,
                9856436898202175L,
                9856436898202160L,
                4941562348080L,
                9856436899282944L,
                9856436898202160L,
                10120319688868400L,
                9856436961147455L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                9856436898202160L,
                560L};
        // </editor-fold>
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .valueOffset(chunkBottomSectionY * 16 - 1)
                .initializeForStoring(chunkTopSectionY * 16 + 15)
                .build(new LongArrayTag(longs));

        assertEquals("""
                49   49   49   49   49   49   48   47   47   47   47   47   47   47   47   47
                49   49   49   49   49   48   48   47   47   47   47   47   47   47   47   47
                49   49   49   49   48   48   48   47   47   47   47   47   47   47   47   47
                49   49   48   48   48   48   47   47   47   47   47   47   47   47   47   47
                49   48   48   48   47   47   47   47   47   47   47   47   47   47   47   47
                48   48   48   47   47   47   47   47   47   47   47   47   47   47   47   47
                48   48   47   47   47   62   62   62   62   47   47   47   47   47   47   47
                47   47   47   47   47   62 -512 -513   62   47   47   47   47   47   47   47
                47   47   47   47   47   62 -513 -513  575   47   47   47   47   47   47   47
                47   47   47   47   47   62   62   62   62   47   47   47   47   47   47   47
                47   47   47   47   47   47   47   47   47   47   47   47   47   47   47   47
                47   47   47   47   47   47   47   47   47   47   47   47   47   47   47   47
                47   47   47   47   47   47   47   47   47   47   47   47   47   47   47   47
                47   47   47   47   47   47   47   47   47   47   47   47   47   47   47   47
                47   47   47   47   47   47   47   47   47   47   47   47   47   47   47   47
                47   47   47   47   47   47   47   47   47   47   47   47   47   47   47   47
              """, packed.toString2dGrid() + "\n");
    }

    public void testNoSplitValuesAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .valueOffset(-65)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertEquals(expectedNoSplitAcrossLongsGrid, packed.toString2dGrid());
        assertSame(packed.getHandle().getValue(), packed.longs());
    }

    public void testSplitValuesAcrossLongs() {
        LongArrayTag tag = new LongArrayTag(getSplitValuesAcrossLongsTestData());
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(tag);
        assertEquals(expectedSplitAcrossLongsGrid, packed.toString2dGrid());
        assertSame(packed.getHandle().getValue(), packed.longs());
    }

    public void testNoSplitValuesAcrossLongs_convertTo_Split() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .valueOffset(-65)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertEquals(37, packed.getHandle().length());
        packed.setPackingStrategy(SPLIT_VALUES_ACROSS_LONGS);
        assertEquals(36, packed.getHandle().length());
        assertEquals(expectedNoSplitAcrossLongsGrid, packed.toString2dGrid());
        assertSame(packed.getHandle().getValue(), packed.longs());
    }

    public void testSplitValuesAcrossLongs_convertTo_NoSplit() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertEquals(36, packed.getHandle().length());
        packed.setPackingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS);
        assertEquals(37, packed.getHandle().length());
        assertEquals(expectedSplitAcrossLongsGrid, packed.toString2dGrid());
        assertSame(packed.getHandle().getValue(), packed.longs());
    }

    public void testSetMinBitsPerValue_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .build();
        packed.set(42, 1);
        assertThrows(IllegalArgumentException.class, () -> packed.setMinBitsPerValue(0));
        assertThrows(IllegalArgumentException.class, () -> packed.setMinBitsPerValue(32));
        assertEquals(1, packed.getMinBitsPerValue());
        assertEquals(1, packed.getHandle().length());
        assertEquals(0, packed.getCurrentMinPackableValue());
        assertEquals(1, packed.getCurrentMaxPackableValue());
        packed.setMinBitsPerValue(31);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(31, packed.getMinBitsPerValue());
        assertEquals(32, packed.getHandle().length());
        assertEquals(0, packed.getCurrentMinPackableValue());
        assertEquals(Integer.MAX_VALUE, packed.getCurrentMaxPackableValue());
        assertEquals(1, packed.get(42));
        assertEquals(0, packed.get(41));
        assertEquals(0, packed.get(43));

        packed.set(0, 99);
        packed.setMinBitsPerValue(5);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(5, packed.getMinBitsPerValue());
        assertEquals(32, packed.getHandle().length());
        assertEquals(99, packed.get(0));
    }

    public void testSetMinBitsPerValue_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .build();
        packed.set(42, 1);
        assertThrows(IllegalArgumentException.class, () -> packed.setMinBitsPerValue(0));
        assertThrows(IllegalArgumentException.class, () -> packed.setMinBitsPerValue(32));
        assertEquals(1, packed.getMinBitsPerValue());
        assertEquals(1, packed.getHandle().length());
        assertEquals(0, packed.getCurrentMinPackableValue());
        assertEquals(1, packed.getCurrentMaxPackableValue());
        packed.setMinBitsPerValue(31);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(31, packed.getMinBitsPerValue());
        assertEquals(31, packed.getHandle().length());
        assertEquals(0, packed.getCurrentMinPackableValue());
        assertEquals(Integer.MAX_VALUE, packed.getCurrentMaxPackableValue());
        assertEquals(1, packed.get(42));
        assertEquals(0, packed.get(41));
        assertEquals(0, packed.get(43));

        packed.set(0, 99);
        packed.setMinBitsPerValue(5);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(5, packed.getMinBitsPerValue());
        assertEquals(31, packed.getHandle().length());
        assertEquals(99, packed.get(0));
    }

    public void testGet_throws_outOfBounds() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .build();
        assertThrows(IndexOutOfBoundsException.class, () -> packed.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> packed.get(64));
    }

    public void testSet_throws_outOfBounds() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .build();
        assertThrows(IndexOutOfBoundsException.class, () -> packed.set(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> packed.set(64, 0));
    }

    public void testSet_throws_whenValueBelowValueOffset() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .valueOffset(10)
                .build();
        assertThrows(IllegalArgumentException.class, () -> packed.set(0, 1));
    }

    public void testSet_automaticallyResizesAsNeeded_splitValuesAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .build();
        assertEquals(1, packed.getHandle().length());
        packed.set(5, 1);
        assertEquals(1, packed.get(5));
        assertEquals(1, packed.getHandle().length());
        packed.set(5, 2);
        assertEquals(2, packed.get(5));
        assertEquals(2, packed.getHandle().length());
        assertSame(packed.getHandle().getValue(), packed.longs());
        packed.set(5, 3);
        assertEquals(3, packed.get(5));
        assertEquals(2, packed.getHandle().length());
        packed.set(5, 31);
        assertEquals(31, packed.get(5));
        assertEquals(5, packed.getHandle().length());
        assertSame(packed.getHandle().getValue(), packed.longs());
    }

    public void testClear_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertEquals(37, packed.getHandle().length());
        packed.setMinBitsPerValue(5);
        assertEquals(37, packed.getHandle().length());
        packed.clear(false);
        assertEquals(37, packed.getHandle().length());
        assertEquals(0, packed.get(0));
        packed.clear(true);
        assertEquals(22, packed.getHandle().length());
        assertSame(packed.getHandle().getValue(), packed.longs());

        // check that bits per value and values per long didn't get broken
        packed.set(57, 29);
        assertEquals(29, packed.get(57));
    }

    public void testClear_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertEquals(36, packed.getHandle().length());
        packed.setMinBitsPerValue(5);
        assertEquals(36, packed.getHandle().length());
        packed.clear(false);
        assertEquals(36, packed.getHandle().length());
        assertEquals(0, packed.get(0));
        packed.clear(true);
        assertEquals(20, packed.getHandle().length());
        assertSame(packed.getHandle().getValue(), packed.longs());

        // check that bits per value and values per long didn't get broken
        packed.set(57, 29);
        assertEquals(29, packed.get(57));
    }

    public void testContains_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertTrue(packed.contains(130));
        assertFalse(packed.contains(120));
        assertFalse(packed.contains(-1));
        assertFalse(packed.contains(512));

        packed.setValueOffset(-65);
        assertFalse(packed.contains(130));
        assertTrue(packed.contains(77));
        assertFalse(packed.contains(-66));
        assertFalse(packed.contains(512 - 65));
    }

    public void testContains_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertTrue(packed.contains(79));
        assertFalse(packed.contains(90));
        assertFalse(packed.contains(-1));
        assertFalse(packed.contains(512));

        packed.setValueOffset(-65);
        assertFalse(packed.contains(79));
        assertTrue(packed.contains(-2));
        assertFalse(packed.contains(-66));
        assertFalse(packed.contains(512 - 65));
    }

    public void testCount_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertEquals(26, packed.count(130));
        assertEquals(0, packed.count(99));

        packed.setValueOffset(-65);
        assertEquals(26, packed.count(130-65));
        assertEquals(0, packed.count(99));
    }

    public void testCount_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertEquals(14, packed.count(80));
        assertEquals(0, packed.count(99));

        packed.setValueOffset(-65);
        assertEquals(3, packed.count(-2));
        assertEquals(0, packed.count(-100));
    }

    public void testCount_predicate_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertEquals(192, packed.count(v -> v > 130));
        assertEquals(0, packed.count(v -> v < 99));

        packed.setValueOffset(-65);
        assertEquals(192, packed.count(v -> v > (130-65)));
        assertEquals(0, packed.count(v -> v < -30));
        assertEquals(256, packed.count(v -> v > -30));
    }

    public void testCount_predicate_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertEquals(119, packed.count(v -> v > 75));
        assertEquals(256, packed.count(v -> v < 99));
        assertEquals(0, packed.count(v -> v >= 99));

        packed.setValueOffset(-65);
        assertEquals(119, packed.count(v -> v > (75-65)));
        assertEquals(0, packed.count(v -> v < -30));
        assertEquals(256, packed.count(v -> v > -30));
        assertEquals(11, packed.count(v -> v < 0));
    }

    public void testReplaceAll_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(0, -1));
        packed.replaceAll(130, 1300);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertTrue(packed.contains(1300));
        assertFalse(packed.contains(130));
        assertEquals(11, packed.getBitsPerValue());


        packed.setValueOffset(-65);
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(-66, 0));
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(0, -66));
        packed.replaceAll(1235, 0);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertTrue(packed.contains(0));
        assertFalse(packed.contains(1235));
    }

    public void testReplaceAll_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(0, -1));
        packed.replaceAll(80, 800);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertTrue(packed.contains(800));
        assertFalse(packed.contains(80));
        assertEquals(10, packed.getBitsPerValue());

        packed.setValueOffset(-65);
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(-66, 0));
        assertThrows(IllegalArgumentException.class, () -> packed.replaceAll(0, -66));
        packed.replaceAll(-2, -60);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertTrue(packed.contains(-60));
        assertFalse(packed.contains(-2));
    }

    public void testRemap_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertThrows(IllegalArgumentException.class, () -> packed.remap(v -> v > 135 ? -1 : v));
        packed.remap(v -> v > 135 ? 0 : v);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals("""
        129 129   0   0   0   0   0   0   0 129   0 129 129 129 129 129
        129 129   0   0   0   0   0   0   0 129 129 129 129 129 129 129
        129 129   0   0   0   0   0   0   0 129 129 129 129 129 129 129
        129 129   0   0   0   0   0   0   0 129 129 129 130 130 130 130
        129 129 129   0   0   0   0   0 129 129 129 130 130 130 131 131
        130 130 130 130 130 130 130 129 130 130 130 130 131 131 131 131
        131 131 131 130 130 130 130 130 130 130 130 131 131 131 131 131
        132 132 132 132 131 131 131 131 131 131 131 131 131 131 131 131
        133 133 133 133 132 132 132 132 132 132 131 131 131 131 131 131
        134 133 133 133 133 133 133 132 132 132 131 131 131 131 131 131
        134 133 133 133 133 133 133 133 132 132 132 131 131 131 131 131
        134 134 133 133 133 133 133 133 132 132 132 131 131 131 131 131
        134 134 133 133 133 133 133 133 132 132 132 131 131 131 131 131
        134 134 133 133 133 133 133 133 132 132 132 131 131 131 131 131
        134 133 133 133 133 133 133 133 132 132 132 131 131 131 131 131
        134 133 133 133 133 133 133 133 133 132 132 132 131 131 131 131""",
                packed.toString2dGrid());


        packed.setValueOffset(-65);
        assertThrows(IllegalArgumentException.class, () -> packed.remap(v -> v % 2 == 0 ? -999 : v));
        packed.remap(v -> v % 2 == 0 ? 999 : v);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals("""
        999 999 -65 -65 -65 -65 -65 -65 -65 999 -65 999 999 999 999 999
        999 999 -65 -65 -65 -65 -65 -65 -65 999 999 999 999 999 999 999
        999 999 -65 -65 -65 -65 -65 -65 -65 999 999 999 999 999 999 999
        999 999 -65 -65 -65 -65 -65 -65 -65 999 999 999  65  65  65  65
        999 999 999 -65 -65 -65 -65 -65 999 999 999  65  65  65 999 999
         65  65  65  65  65  65  65 999  65  65  65  65 999 999 999 999
        999 999 999  65  65  65  65  65  65  65  65 999 999 999 999 999
         67  67  67  67 999 999 999 999 999 999 999 999 999 999 999 999
        999 999 999 999  67  67  67  67  67  67 999 999 999 999 999 999
         69 999 999 999 999 999 999  67  67  67 999 999 999 999 999 999
         69 999 999 999 999 999 999 999  67  67  67 999 999 999 999 999
         69  69 999 999 999 999 999 999  67  67  67 999 999 999 999 999
         69  69 999 999 999 999 999 999  67  67  67 999 999 999 999 999
         69  69 999 999 999 999 999 999  67  67  67 999 999 999 999 999
         69 999 999 999 999 999 999 999  67  67  67 999 999 999 999 999
         69 999 999 999 999 999 999 999 999  67  67  67 999 999 999 999""",
                packed.toString2dGrid());
    }

    public void testRemap_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertThrows(IllegalArgumentException.class, () -> packed.remap(v -> -v));
        packed.remap(v -> Math.min(v, 75));
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals("""
        70 69 69 69 71 73 73 73 71 71 71 70 64 64 63 63
        70 70 70 69 71 71 73 71 71 73 71 71 64 64 64 63
        75 71 70 70 71 71 71 71 73 73 73 71 65 64 64 64
        75 72 71 71 71 70 70 71 72 73 71 71 71 71 71 71
        75 72 72 72 71 71 70 70 71 71 71 71 71 73 71 71
        75 75 75 75 75 75 71 71 70 70 69 71 73 73 73 71
        74 75 75 75 75 75 72 75 75 75 75 75 71 73 72 71
        74 75 75 75 75 75 73 75 75 75 75 75 75 75 71 71
        75 75 75 75 75 75 73 75 75 75 75 75 75 75 75 68
        75 75 75 75 75 75 74 75 75 75 75 75 75 75 75 69
        75 75 75 75 75 75 74 75 75 75 75 75 75 75 75 70
        75 75 75 75 75 75 75 75 75 74 74 75 75 75 75 71
        75 75 75 75 75 75 75 75 75 75 74 74 74 73 72 71
        75 75 75 75 75 75 75 75 75 75 75 74 74 73 73 72
        75 75 75 75 75 75 75 75 75 75 75 75 75 75 75 72
        75 75 75 75 75 75 75 75 75 75 75 75 75 75 75 73""",
                packed.toString2dGrid());

        packed.setValueOffset(-65);
        assertThrows(IllegalArgumentException.class, () -> packed.remap(v -> v < 0 ? v * 100 : v));
        packed.remap(v -> v < 0 ? v * 10 : v);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(
          """
            5   4   4   4   6   8   8   8   6   6   6   5 -10 -10 -20 -20
            5   5   5   4   6   6   8   6   6   8   6   6 -10 -10 -10 -20
           10   6   5   5   6   6   6   6   8   8   8   6   0 -10 -10 -10
           10   7   6   6   6   5   5   6   7   8   6   6   6   6   6   6
           10   7   7   7   6   6   5   5   6   6   6   6   6   8   6   6
           10  10  10  10  10  10   6   6   5   5   4   6   8   8   8   6
            9  10  10  10  10  10   7  10  10  10  10  10   6   8   7   6
            9  10  10  10  10  10   8  10  10  10  10  10  10  10   6   6
           10  10  10  10  10  10   8  10  10  10  10  10  10  10  10   3
           10  10  10  10  10  10   9  10  10  10  10  10  10  10  10   4
           10  10  10  10  10  10   9  10  10  10  10  10  10  10  10   5
           10  10  10  10  10  10  10  10  10   9   9  10  10  10  10   6
           10  10  10  10  10  10  10  10  10  10   9   9   9   8   7   6
           10  10  10  10  10  10  10  10  10  10  10   9   9   8   8   7
           10  10  10  10  10  10  10  10  10  10  10  10  10  10  10   7
           10  10  10  10  10  10  10  10  10  10  10  10  10  10  10   8
          """, packed.toString2dGrid() + "\n");
    }

    public void testCompact_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .valueOffset(-65)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        packed.remap(v -> v / 3);
        assertFalse(packed.shouldCompact());
        packed.setMinBitsPerValue(1);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertTrue(packed.shouldCompact());
        String expect = packed.toString2dGrid();
        assertEquals(7, packed.getActualUsedBitsPerValue());
        assertEquals(9, packed.getBitsPerValue());
        packed.compact();
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(7, packed.getBitsPerValue());
        assertEquals(expect, packed.toString2dGrid());
        assertEquals(29, packed.getHandle().length());
    }

    public void testCompact_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        packed.remap(v -> v / 3);
        assertFalse(packed.shouldCompact());
        packed.setMinBitsPerValue(1);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertTrue(packed.shouldCompact());
        String expect = packed.toString2dGrid();
        assertEquals(5, packed.getActualUsedBitsPerValue());
        assertEquals(9, packed.getBitsPerValue());
        packed.compact();
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(5, packed.getBitsPerValue());
        assertEquals(expect, packed.toString2dGrid());
        assertEquals(20, packed.getHandle().length());
    }


    public void testClone_noSplitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .valueOffset(-65)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));

        LongArrayTagPackedIntegers packed2 = packed.clone();
        assertEquals(packed.length, packed2.length);
        assertEquals(packed.getActualUsedBitsPerValue(), packed2.getActualUsedBitsPerValue());
        assertEquals(packed.getBitsPerValue(), packed2.getBitsPerValue());
        assertEquals(packed.getCurrentMaxPackableValue(), packed2.getCurrentMaxPackableValue());
        assertEquals(packed.getCurrentMinPackableValue(), packed2.getCurrentMinPackableValue());
        assertEquals(packed.getPackingStrategy(), packed2.getPackingStrategy());
        assertEquals(packed.getHandle(), packed2.getHandle());
        assertNotSame(packed.getHandle(), packed2.getHandle());
        assertNotSame(packed.longs(), packed2.longs());
        assertNotSame(packed.getHandle().getValue(), packed2.getHandle().getValue());
        assertSame(packed2.getHandle().getValue(), packed2.longs());
        assertEquals(packed.getValueOffset(), packed2.getValueOffset());
    }


    public void testClone_splitAcrossLongs() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .valueOffset(-1)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));

        LongArrayTagPackedIntegers packed2 = packed.clone();
        assertEquals(packed.length, packed2.length);
        assertEquals(packed.getActualUsedBitsPerValue(), packed2.getActualUsedBitsPerValue());
        assertEquals(packed.getBitsPerValue(), packed2.getBitsPerValue());
        assertEquals(packed.getCurrentMaxPackableValue(), packed2.getCurrentMaxPackableValue());
        assertEquals(packed.getCurrentMinPackableValue(), packed2.getCurrentMinPackableValue());
        assertEquals(packed.getPackingStrategy(), packed2.getPackingStrategy());
        assertEquals(packed.getHandle(), packed2.getHandle());
        assertNotSame(packed.getHandle(), packed2.getHandle());
        assertNotSame(packed.longs(), packed2.longs());
        assertNotSame(packed.getHandle().getValue(), packed2.getHandle().getValue());
        assertSame(packed2.getHandle().getValue(), packed2.longs());
        assertEquals(packed.getValueOffset(), packed2.getValueOffset());
    }


    public void testToValueArray_takingArray() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .valueOffset(-65)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertThrows(IllegalArgumentException.class, () -> packed.toArray(new int[0]));
        assertThrows(IllegalArgumentException.class, () -> packed.toArray(new int[257]));

        int[] ints = new int[packed.length];
        assertSame(ints, packed.toArray(ints));
        for (int i = 0; i < ints.length; i++) {
            assertEquals(packed.get(i), ints[i]);
        }
    }

    public void testToValueArray_takingArrayAndStartIndex() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .valueOffset(-65)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getNoSplitValuesAcrossLongsTestData()));
        assertThrows(IllegalArgumentException.class, () -> packed.toArray(new int[0], 0));
        assertThrows(IllegalArgumentException.class, () -> packed.toArray(new int[257], 2));
        assertThrows(IllegalArgumentException.class, () -> packed.toArray(new int[300], -1));

        int[] ints = new int[packed.length * 2];
        assertSame(ints, packed.toArray(ints, 100));
        for (int i = 0; i < packed.length; i++) {
            assertEquals(packed.get(i), ints[i + 100]);
        }
    }

    public void testSetFromValueArray() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(4)
                .minBitsPerValue(1)
                .build();
        assertThrows(IllegalArgumentException.class, () -> packed.setFromArray(new int[0]));
        assertThrows(IllegalArgumentException.class, () -> packed.setFromArray(new int[5]));

        // should grow bits per value
        final int[] given = new int[] {1, 7, 2, 0};
        packed.setFromArray(given);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(3, packed.getBitsPerValue());
        assertArrayEquals(given, packed.toArray());

        // should shrink bits per value
        final int[] given2 = new int[] {1, 3, 2, 0};
        packed.setFromArray(given2);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(2, packed.getBitsPerValue());
        assertArrayEquals(given2, packed.toArray());
    }

    public void testSetFromValueArray_takingStartIndex() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(4)
                .minBitsPerValue(1)
                .build();
        assertThrows(IllegalArgumentException.class, () -> packed.setFromArray(new int[0]));
        assertThrows(IllegalArgumentException.class, () -> packed.setFromArray(new int[5]));

        // should grow bits per value
        final int[] given = new int[] {1, 7, 2, 0, 5, 4, 9, 6, 0, -1, 1, -1};
        packed.setFromArray(given, 0);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(3, packed.getBitsPerValue());
        assertArrayEquals(new int[] {1, 7, 2, 0}, packed.toArray());

        packed.setFromArray(given, 4);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(4, packed.getBitsPerValue());
        assertArrayEquals(new int[] {5, 4, 9, 6}, packed.toArray());

        packed.setValueOffset(-1);
        packed.setFromArray(given, 8);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertEquals(2, packed.getBitsPerValue());
        assertArrayEquals(new int[] {0, -1, 1, -1}, packed.toArray());

        packed.setValueOffset(0);
        assertThrows(IllegalArgumentException.class, () -> packed.setFromArray(given, 6));
        assertEquals(1, packed.get(0));
        assertEquals(2, packed.getBitsPerValue());
    }

    public void testIterator() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .minBitsPerValue(1)
                .valueOffset(-1)
                .build(new int[] {1, 7, 2, -1});
        ListIterator<Integer> iter = packed.iterator();
//        System.out.println(packed.toString2dGrid());

        assertThrows(NoSuchElementException.class, iter::previous);
        assertFalse(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(0, iter.nextIndex());
        assertEquals(-1, iter.previousIndex());

        assertEquals(1, (int) iter.next());
        assertTrue(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextIndex());
        assertEquals(0, iter.previousIndex());

        assertEquals(1, (int) iter.previous());
        assertFalse(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(0, iter.nextIndex());
        assertEquals(-1, iter.previousIndex());

        assertEquals(1, (int) iter.next());
        assertTrue(iter.hasPrevious());
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextIndex());
        assertEquals(0, iter.previousIndex());

        assertEquals(7, (int) iter.next());
        assertTrue(iter.hasPrevious());
        assertTrue(iter.hasNext());

        assertEquals(2, (int) iter.next());
        assertTrue(iter.hasPrevious());
        assertTrue(iter.hasNext());

        assertEquals(-1, (int) iter.next());
        assertTrue(iter.hasPrevious());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    public void testIterator_setValue() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .minBitsPerValue(1)
                .valueOffset(-1)
                .build(new int[] {1, 7, 2, -1});
        ListIterator<Integer> iter = packed.iterator();
        assertThrows(IllegalStateException.class, () -> iter.set(2));
        iter.next();
        iter.set(5);
        iter.next();
        iter.next();
        iter.previous();
        iter.set(42);
        assertSame(packed.getHandle().getValue(), packed.longs());
        assertArrayEquals(new int[] {5, 7, 42, -1}, packed.toArray());
        iter.previous();
        iter.set(-1);
        assertArrayEquals(new int[] {5, -1, 42, -1}, packed.toArray());
        assertThrows(IllegalArgumentException.class, () -> iter.set(-2));
    }

    public void testGetSet2d() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(SPLIT_VALUES_ACROSS_LONGS)
                .length(256)
                .minBitsPerValue(9)
                .build(new LongArrayTag(getSplitValuesAcrossLongsTestData()));
        assertEquals(16, packed.squareEdgeLength());
        assertEquals(-1, packed.cubeEdgeLength());
        assertEquals(79, packed.get2d(4, 6));
        packed.set2d(4, 6, 99);
        assertEquals(99, packed.get2d(4, 6));
        assertEquals(""" 
            70 69 69 69 71 73 73 73 71 71 71 70 64 64 63 63
            70 70 70 69 71 71 73 71 71 73 71 71 64 64 64 63
            77 71 70 70 71 71 71 71 73 73 73 71 65 64 64 64
            77 72 71 71 71 70 70 71 72 73 71 71 71 71 71 71
            77 72 72 72 71 71 70 70 71 71 71 71 71 73 71 71
            76 78 78 78 78 77 71 71 70 70 69 71 73 73 73 71
            74 78 79 80 99 78 72 78 78 78 78 77 71 73 72 71
            74 78 80 80 80 78 73 78 78 80 78 78 75 75 71 71
            75 78 79 80 79 78 73 78 80 80 80 78 77 76 75 68
            76 78 78 78 78 77 74 78 79 80 78 78 77 77 75 69
            76 76 76 75 75 75 74 78 78 78 78 78 77 75 75 70
            77 76 76 76 78 79 79 79 79 74 74 75 75 75 75 71
            77 77 77 76 79 80 81 80 79 75 74 74 74 73 72 71
            78 78 77 77 79 81 81 81 79 75 75 74 74 73 73 72
            78 78 78 77 79 79 81 80 79 75 79 79 79 79 79 72
            79 79 78 78 78 79 79 79 79 76 79 80 81 79 79 73""", packed.toString2dGrid());
    }

    public void testGetSet3d() {
        LongArrayTagPackedIntegers packed = LongArrayTagPackedIntegers.builder()
                .packingStrategy(NO_SPLIT_VALUES_ACROSS_LONGS)
                .length(64)
                .minBitsPerValue(1)
                .initializeForStoring(5)
                .build(new LongArrayTag(3785436329631921288L, 3932602464921073299L, 2797418351439529682L, 4L));

        assertEquals(8, packed.squareEdgeLength());
        assertEquals(4, packed.cubeEdgeLength());
        assertEquals(4, packed.get3d(2, 1, 3));
        packed.set3d(2, 1, 3, 6);
        assertEquals(6, packed.get3d(2, 1, 3));
        assertEquals("""
            Y=0
            0 1 2 2
            3 3 4 4
            3 4 4 4
            4 4 4 4
            Y=1
            0 1 2 2
            3 3 2 2
            3 3 4 4
            3 4 6 4
            Y=2
            5 1 2 2
            3 3 2 2
            3 3 2 2
            3 3 4 4
            Y=3
            5 5 5 2
            5 5 2 2
            3 3 2 2
            3 3 2 4""", packed.toString3dGrid());
    }
}
