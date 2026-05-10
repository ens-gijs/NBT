package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.entities.Entity;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class McaEntitiesFileTest extends McaFileBaseTest {

    public void testReadEntities_1_17_1() throws IOException {
        McaEntitiesFile mca = McaFileHelpers.readAuto(copyResourceToTmp("1_17_1/entities/r.-3.-2.mca"));
        assertNotNull(mca);
        EntitiesChunk chunk = mca.stream().filter(Objects::nonNull).findFirst().orElse(null);
        assertNotNull(chunk);
        List<Entity> entities = chunk.getEntities();
        assertNotNull(entities);
        assertFalse(entities.isEmpty());

        // mca specific checks (will need to be changed if mca file changes in meaningful ways)
        assertEquals(1, entities.size());
        Map<String, List<Entity>> entitiesByType = chunk.stream().collect(Collectors.groupingBy(Entity::getId));
        assertEquals(1, entitiesByType.size());
        assertTrue(entitiesByType.containsKey("minecraft:villager"));
        assertEquals(1, entitiesByType.get("minecraft:villager").size());
    }

    public void testReadEntities_1_18_PRE1() throws IOException {
        McaEntitiesFile mca = McaFileHelpers.readAuto(copyResourceToTmp("1_18_PRE1/entities/r.-2.-3.mca"));
        assertNotNull(mca);
        EntitiesChunk chunk = mca.stream().filter(Objects::nonNull).findFirst().orElse(null);
        assertNotNull(chunk);
        List<Entity> entities = chunk.getEntities();
        assertNotNull(entities);
        assertFalse(entities.isEmpty());

        // mca specific checks (will need to be changed if mca file changes in meaningful ways)
        assertEquals(7, entities.size());
        Map<String, List<Entity>> entitiesByType = chunk.stream().collect(Collectors.groupingBy(Entity::getId));
        assertEquals(3, entitiesByType.size());
        assertTrue(entitiesByType.containsKey("minecraft:villager"));
        assertEquals(4, entitiesByType.get("minecraft:villager").size());
        assertTrue(entitiesByType.containsKey("minecraft:chicken"));
        assertEquals(2, entitiesByType.get("minecraft:chicken").size());
        assertTrue(entitiesByType.containsKey("minecraft:cat"));
        assertEquals(1, entitiesByType.get("minecraft:cat").size());
    }

    // TODO: make this a real test
//    public void testMoveChunk_1_20_4() throws IOException {
//        String mcaResourcePath = "1_20_4/entities/r.-3.-3.mca";
//        McaEntitiesFile mca = assertThrowsNoException(() -> McaFileHelpers.readAuto(getResourceFile(mcaResourcePath))); // , LoadFlags.RAW
//        assertNotNull(mca);
//
//        assertTrue(mca.moveRegion(0, 0, false));
//
//        String newMcaName = mca.createRegionName();
//        assertEquals("r.0.0.mca", newMcaName);
//        int i = 0;
//        for (EntitiesChunk chunk : mca) {
//            if (chunk != null) {
//                chunk.updateHandle();
//                TextNbtHelpers.writeTextNbtFile(Paths.get("TESTDBG", mcaResourcePath + ".MOVEDTO." + newMcaName + ".i" + String.format("%04d", i) + "." + chunk.getChunkXZ().toString("x%dz%d") + ".original.snbt"), chunk.data, /*pretty print*/ true, /*sorted*/ true);
//            }
//            i++;
//        }
//    }
}
