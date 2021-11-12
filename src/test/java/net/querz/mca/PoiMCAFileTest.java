package net.querz.mca;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PoiMCAFileTest extends MCAFileBaseTest {

    public void testPoiMca_1_17_1() {
        PoiMCAFile mca = assertThrowsNoException(() -> MCAUtil.readAuto(copyResourceToTmp("1_17_1/poi/r.-3.-2.mca")));
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

    public void testMcaReadWriteParity_1_17_1() {
        validateReadWriteParity(DataVersion.JAVA_1_17_1, "1_17_1/poi/r.-3.-2.mca", PoiMCAFile.class);
    }
}
