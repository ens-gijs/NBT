package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.io.McaFileHelpers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class McaPoiFileTest extends McaFileBaseTest {
    public void testPoiMca_1_17_1() {
        McaPoiFile mca = assertThrowsNoException(() -> McaFileHelpers.readAuto(copyResourceToTmp("1_17_1/poi/r.-3.-2.mca")));
        PoiChunk chunk = mca.stream().filter(Objects::nonNull).findFirst().orElse(null);
        assertNotNull(chunk);
        assertEquals(DataVersion.JAVA_1_17_1, chunk.getDataVersionEnum());
        assertTrue(chunk.isPoiSectionValid(3));
        assertTrue(chunk.isPoiSectionValid(4));

        assertFalse(chunk.isEmpty());
        Map<String, List<PoiRecord>> recordsByType = chunk.stream().collect(Collectors.groupingBy(PoiRecord::getType));
        assertTrue(recordsByType.containsKey("minecraft:home"));
        assertTrue(recordsByType.containsKey("minecraft:cartographer"));
        assertTrue(recordsByType.containsKey("minecraft:nether_portal"));
        assertEquals(1, recordsByType.get("minecraft:home").size());
        assertEquals(6, recordsByType.get("minecraft:nether_portal").size());
        assertEquals(new PoiRecord(-1032, 63, -670, "minecraft:home"), recordsByType.get("minecraft:home").get(0));
        // it'd be better if we had a bell in this chunk to test a non-zero value here
        assertEquals(0, recordsByType.get("minecraft:home").get(0).getFreeTickets());
    }

    // TODO: make this a real test
//    public void testMoveChunk_1_20_4() throws IOException {
//        String mcaResourcePath = "1_20_4/poi/r.-3.-3.mca";
//        McaPoiFile mca = assertThrowsNoException(() -> McaFileHelpers.readAuto(getResourceFile(mcaResourcePath))); // , LoadFlags.RAW
//        assertNotNull(mca);
//
//        assertTrue(mca.moveRegion(0, 0, false));
//
//        String newMcaName = mca.createRegionName();
//        assertEquals("r.0.0.mca", newMcaName);
//        int i = 0;
//        for (PoiChunk chunk : mca) {
//            if (chunk != null) {
//                chunk.updateHandle();
//                TextNbtHelpers.writeTextNbtFile(Paths.get("TESTDBG", mcaResourcePath + ".MOVEDTO." + newMcaName + ".i" + String.format("%04d", i) + "." + chunk.getChunkXZ().toString("x%dz%d") + ".original.snbt"), chunk.data, /*pretty print*/ true, /*sorted*/ true);
//            }
//            i++;
//        }
//    }
}
