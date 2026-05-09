package io.github.ensgijs.nbt.mca;

public class McaTerrainFileTest extends McaFileBaseTest {
    // TODO: add coverage
    public void testMakeBuildHappy() {}

    // TODO: make this a real test
//    public void testMoveChunk_1_20_4() throws IOException {
//        String mcaResourcePath = "1_20_4/region/r.-3.-3.mca";
//        McaRegionFile mca = assertThrowsNoException(() -> McaFileHelpers.readAuto(getResourceFile(mcaResourcePath))); // , LoadFlags.RAW
//        assertNotNull(mca);
//
//        assertTrue(mca.moveRegion(0, 0, false));
//
//        String newMcaName = mca.createRegionName();
//        assertEquals("r.0.0.mca", newMcaName);
//        int i = 0;
//        for (TerrainChunk chunk : mca) {
//            if (chunk != null) {
//                chunk.updateHandle();
//                TextNbtHelpers.writeTextNbtFile(Paths.get("TESTDBG", mcaResourcePath + ".MOVEDTO." + newMcaName + ".i" + String.format("%04d", i) + "." + chunk.getChunkXZ().toString("x%dz%d") + ".original.snbt"), chunk.data, /*pretty print*/ true, /*sorted*/ true);
//            }
//            i++;
//        }
//    }
}
