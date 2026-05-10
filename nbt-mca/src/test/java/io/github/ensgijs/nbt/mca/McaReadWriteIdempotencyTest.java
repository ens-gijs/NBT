package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.mca.util.ChunkIterator;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO: scan resources folder and auto-run this test for each mca file and type so we don't need to update tests every time we add a new test region file.
public class McaReadWriteIdempotencyTest extends McaFileBaseTest {
    /**
     * When set, text nbt files will be generated for every chunk scanned.
     * The output can be found in the project root directory in a folder called TESTDBG.
     * This folder should be set in .getignore
     * <p>When these test</p>
     */
    private boolean DUMP_TESTDBG_OUTPUT = false;

    /**
     * When set, the nbt tag for the region file will be cleared after parsing and before writing to check for any missed tags.
     * When set to false this will also cause the dumped nbt to be in file order (unsorted) - but the order is
     * expected to be the same between read and write.
     * <p>tip: it's a good idea when adding support for a new version to set the following no mater the flavor to see what's there (look at the diff in TESTDBG).
     * <pre>{@code
     * DUMP_TESTDBG_OUTPUT = true;
     * CLEAR_DATA_TAG_BEFORE_REGURGITATION = true;
     * }</pre></p>
     */
    private boolean CLEAR_DATA_TAG_BEFORE_REGURGITATION = true;

    /**
     * Reads an mca file, writes it, reads it back in, verifies that the NBT of the first non-empty chunk is identical
     * between the reads and that the data versions are correct and that McaFileHelpers.readAuto returned the correct type.
     */
    private <CT extends ChunkBase, FT extends McaFileBase<CT>> void validateReadWriteParity(DataVersion expectedDataVersion, String mcaResourcePath, Class<FT> clazz) throws IOException {
        validateReadWriteParity(expectedDataVersion, expectedDataVersion, mcaResourcePath, clazz);
    }
    /**
     * Reads an mca file, writes it, reads it back in, verifies that the NBT of the first non-empty chunk is identical
     * between the reads and that the data versions are correct and that McaFileHelpers.readAuto returned the correct type.
     */
    private <CT extends ChunkBase, FT extends McaFileBase<CT>> void validateReadWriteParity(DataVersion minExpectedDataVersion, DataVersion maxExpectedDataVersion, String mcaResourcePath, Class<FT> clazz) throws IOException {
        long flags = LoadFlags.LOAD_ALL_DATA;  // | LoadFlags.RELEASE_CHUNK_DATA_TAG;
//        System.out.printf("LoadFlags: 0x%08X_%08X%n", (int)(flags >> 32), flags & 0xFFFF_FFFFL);
        FT mcaA = assertThrowsNoException(() -> McaFileHelpers.readAuto(
                getResourceFile(mcaResourcePath),
                flags));
        assertTrue(clazz.isInstance(mcaA));
        assertTrue(mcaA.stream().anyMatch(Objects::nonNull));

        String assertionMsg = mcaResourcePath +
                "> Expected: " + minExpectedDataVersion + (minExpectedDataVersion != maxExpectedDataVersion ? " to " + maxExpectedDataVersion : "") +
                ";Actual: " + DataVersion.bestFor(mcaA.getDefaultChunkDataVersion());

        if (minExpectedDataVersion == maxExpectedDataVersion) {
            assertEquals(assertionMsg, minExpectedDataVersion.id(), mcaA.getMinChunkDataVersion());
            assertEquals(assertionMsg, maxExpectedDataVersion.id(), mcaA.getMaxChunkDataVersion());
        } else {
            assertTrue(assertionMsg, mcaA.getMinChunkDataVersion() >= minExpectedDataVersion.id());
            assertTrue(assertionMsg, mcaA.getMinChunkDataVersion() <= maxExpectedDataVersion.id());
            assertTrue(assertionMsg, mcaA.getMaxChunkDataVersion() >= minExpectedDataVersion.id());
            assertTrue(assertionMsg, mcaA.getMaxChunkDataVersion() <= maxExpectedDataVersion.id());
        }

        for (CT chunk : mcaA) {
            if (chunk == null) continue;
            assertTrue(
                    chunk.getChunkXZ() + "; " + assertionMsg,
                    minExpectedDataVersion.id() <= chunk.getDataVersionEnum().id() && maxExpectedDataVersion.id() >= chunk.getDataVersionEnum().id());
            if (CLEAR_DATA_TAG_BEFORE_REGURGITATION) {
                // yes, we have to clear each section tag because Section's hold onto the tag ref.
                if (chunk instanceof SectionedChunkBase) {
                    for (SectionBase<?> section : (SectionedChunkBase<?>) chunk) {
                        section.data.clear();
                    }
                }
                chunk.data.clear();
            }
        }

        File tmpFile = super.getNewTmpFile(Paths.get("out", mcaResourcePath).toString());
        assertThrowsNoException(() -> McaFileHelpers.write(mcaA, tmpFile));

        FT mcaB = assertThrowsNoException(() -> McaFileHelpers.readAuto(tmpFile));
        assertTrue(clazz.isInstance(mcaB));
        assertTrue(mcaB.stream().anyMatch(Objects::nonNull));
        assertEquals(mcaA.getDefaultChunkDataVersion(), mcaB.getDefaultChunkDataVersion());
        assertEquals(mcaA.getMinChunkDataVersion(), mcaB.getMinChunkDataVersion());
        assertEquals(mcaA.getMaxChunkDataVersion(), mcaB.getMaxChunkDataVersion());

        for (int i = 0; i < 1024; i++) {
            CT chunkA = mcaA.getChunk(i);
            CT chunkB = mcaB.getChunk(i);
            if (chunkA != null && chunkB != null) {
                if (DUMP_TESTDBG_OUTPUT) {
                    try {
                        TextNbtHelpers.writeTextNbtFile(Paths.get("TESTDBG", mcaResourcePath + ".i" + String.format("%04d", i) + "." + chunkA.getChunkXZ().toString("x%dz%d") + ".original.snbt"), chunkA.data, /*pretty print*/ true, /*sorted*/ true);
                        if (!chunkA.data.equals(chunkB.data)) {
                            TextNbtHelpers.writeTextNbtFile(Paths.get("TESTDBG", mcaResourcePath + ".i" + String.format("%04d", i) + "." + chunkA.getChunkXZ().toString("x%dz%d") + ".regurgitated.snbt"), chunkB.data, /*pretty print*/ true, /*sorted*/ true);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        fail(ex.getMessage());
                    }
                }

                assertEquals(chunkA.getDataVersionEnum(), chunkB.getDataVersionEnum());
                assertEquals("Expected original and regurgitated files to be the same, but they weren't. Enable DUMP_TESTDBG_OUTPUT for the failing test, rerun, and look at the diff.", chunkA.data, chunkB.data);
            } else {
                assertNull(chunkA);
                assertNull(chunkB);
            }
        }
    }

    public void testMcaReadWriteParity_1_9_4() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_9_4, "1_9_4/region/r.2.-1.mca", McaRegionFile.class);
    }

    public void testMcaReadWriteParity_1_12_2() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_12_2, "1_12_2/region/r.0.0.mca", McaRegionFile.class);
    }

    public void testMcaReadWriteParity_1_13_0() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_13_0, "1_13_0/region/r.0.0.mca", McaRegionFile.class);
    }

    public void ignore_testMcaReadWriteParity_1_13_1() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_13_1, "1_13_1/region/r.2.2.mca", McaRegionFile.class);
    }

    public void testMcaReadWriteParity_1_13_2() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_13_2, "1_13_2/region/r.-2.-2.mca", McaRegionFile.class);
    }

    public void testMcaReadWriteParity_1_14_4() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_14_4, "1_14_4/region/r.-1.0.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_14_4, "1_14_4/poi/r.-1.0.mca", McaPoiFile.class);
    }

    public void testMcaReadWriteParity_1_15_2() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_15_2, "1_15_2/region/r.-1.0.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_15_2, "1_15_2/poi/r.-1.0.mca", McaPoiFile.class);

        validateReadWriteParity(DataVersion.JAVA_1_15_2, "1_15_2/region/r.0.0.mca", McaRegionFile.class);
    }

    public void testMcaReadWriteParity_1_16_5() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_16_5, "1_16_5/region/r.0.-1.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_16_5, "1_16_5/poi/r.0.-1.mca", McaPoiFile.class);
    }

    public void testMcaReadWriteParity_1_17_1() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_17_1, "1_17_1/region/r.-3.-2.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_17_1, "1_17_1/poi/r.-3.-2.mca", McaPoiFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_17_1, "1_17_1/entities/r.-3.-2.mca", McaEntitiesFile.class);
    }

    public void testMcaReadWriteParity_1_18_PRE1() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_18_PRE1, "1_18_PRE1/region/r.-2.-3.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_18_PRE1, "1_18_PRE1/poi/r.-2.-3.mca", McaPoiFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_18_PRE1, "1_18_PRE1/entities/r.-2.-3.mca", McaEntitiesFile.class);
    }

    public void testMcaReadWriteParity_1_18_1() throws IOException {
        validateReadWriteParity(DataVersion.JAVA_1_18_1, "1_18_1/region/r.0.-2.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_18_1, "1_18_1/poi/r.0.-2.mca", McaPoiFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_18_1, "1_18_1/entities/r.0.-2.mca", McaEntitiesFile.class);

        validateReadWriteParity(DataVersion.JAVA_1_18_1, "1_18_1/region/r.8.1.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_18_1, "1_18_1/entities/r.8.1.mca", McaEntitiesFile.class);
    }

    public void testMcaReadWriteParity_1_20_4() throws IOException {
//        DUMP_TESTDBG_OUTPUT = true;
        validateReadWriteParity(DataVersion.JAVA_1_20_4, "1_20_4/region/r.-3.-3.mca", McaRegionFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_20_4, "1_20_4/poi/r.-3.-3.mca", McaPoiFile.class);
        validateReadWriteParity(DataVersion.JAVA_1_20_4, "1_20_4/entities/r.-3.-3.mca", McaEntitiesFile.class);
    }


    // <editor-fold desc="Utility to import sample chunks from save game files">
    private final static String MC_SAVES_ROOT = "C:/Users/Ross/AppData/Roaming/.minecraft/saves/";
    private final static String TEST_RESOURCE_ROOT = "src/test/resources/";  // output

//    public void testStripAndImportRegionFiles() throws IOException {
//        // easyImport("1_18_1", 275, 33);
//        // pillager outpost and a chunk of a mineshaft - world seed: -4846182428012336372L
//        easyImport("1_20_4", XZ(-94, -85), XZ(-94, -86), XZ(-95, -85), XZ(-95, -86), XZ(-91, -87));
//    }

    // <editor-fold defaultstate="collapsed">
    private void easyImport(String saveName, int x, int z) throws IOException {
        easyImport(saveName, IntPointXZ.XZ(x, z));
    }

    private void easyImport(String saveName, IntPointXZ... chunkCoords) throws IOException {
        List<String> mcaFileNames = Arrays.stream(chunkCoords)
                .map(c -> McaFileHelpers.createNameFromChunkLocation(c.getX(), c.getZ()))
                .distinct()
                .collect(Collectors.toList());
        Set<IntPointXZ> coordsFilter = new HashSet<>(Arrays.asList(chunkCoords));
        for (String rname : mcaFileNames) {
            copyRegion(saveName, rname, (x, z) -> coordsFilter.contains(new IntPointXZ(x, z)));
        }
    }

    private DataVersion copyRegion(Path mcaPathIn, Function<McaFileBase<?>, File> outFileNameProvider, BiPredicate<Integer, Integer> chunkFilter) throws IOException {
        if (!mcaPathIn.toFile().exists()) return DataVersion.UNKNOWN;
        McaFileBase<?> mcaIn = McaFileHelpers.readAuto(mcaPathIn.toFile(), LoadFlags.RAW);
        File mcaOutFile = outFileNameProvider.apply(mcaIn);
        McaFileBase<ChunkBase> mcaOut = McaFileHelpers.autoMCAFile(mcaOutFile);
        mcaOut.setDefaultChunkDataVersion(mcaIn.getDefaultChunkDataVersion());
        ChunkIterator<?> iter = mcaIn.iterator();
        while (iter.hasNext()) {
            ChunkBase chunk = iter.next();
            if (chunkFilter.test(iter.currentAbsoluteX(), iter.currentAbsoluteZ())) {
                mcaOut.setChunk(iter.currentIndex(), chunk);
            }
        }
        McaFileHelpers.write(mcaOut, mcaOutFile);
        System.out.println("Wrote " + mcaOutFile.getAbsolutePath());
        return DataVersion.bestFor(mcaIn.getDefaultChunkDataVersion());
    }

    private File makeOutName(McaFileBase<?> mca, String type) {
        DataVersion dv = mca.getDefaultChunkDataVersionEnum();
        String verString = dv.name().substring(5);  // strip JAVA_ prefix
        Path outPath = Paths.get(TEST_RESOURCE_ROOT, verString, type);
        if (!outPath.toFile().exists())
            outPath.toFile().mkdirs();
        return Paths.get(outPath.toString(), mca.createRegionName()).toFile();
    }

    private void copyRegion(String saveName, String rname, BiPredicate<Integer, Integer> chunkFilter) throws IOException {
        DataVersion dv = copyRegion(
                Paths.get(MC_SAVES_ROOT, saveName, "region", rname),
                mca -> makeOutName(mca, "region"),
                chunkFilter);
        if (dv.hasPoiMca())
            copyRegion(
                    Paths.get(MC_SAVES_ROOT, saveName, "poi", rname),
                    mca -> makeOutName(mca, "poi"),
                    chunkFilter);
        if (dv.hasEntitiesMca())
            copyRegion(
                    Paths.get(MC_SAVES_ROOT, saveName, "entities", rname),
                    mca -> makeOutName(mca, "entities"),
                    chunkFilter);
    }
    // </editor-fold>
    // </editor-fold>
}
