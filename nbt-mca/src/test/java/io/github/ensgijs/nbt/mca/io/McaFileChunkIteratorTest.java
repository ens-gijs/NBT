package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.mca.*;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;

import java.io.IOException;

public class McaFileChunkIteratorTest extends McaTestCase {

    public void validateIteratePoiFile(long loadFlags) throws IOException {
        McaFileChunkIterator<PoiChunk> iter = McaFileChunkIterator.iterate(
                getResourceFile("1_20_4/poi/r.-3.-3.mca"), loadFlags, PoiChunk::new
        );
//        System.out.println(iter);
        assertTrue(iter.hasNext());
        PoiChunk chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-94, -71), chunk.getChunkXZ());
        assertEquals(1713564474, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-78, -70), chunk.getChunkXZ());
        assertEquals(1713564484, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-77, -84), chunk.getChunkXZ());
        assertEquals(1713564485, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-77, -73), chunk.getChunkXZ());
        assertEquals(1713564485, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-77, -68), chunk.getChunkXZ());
        assertEquals(1713564485, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-82, -67), chunk.getChunkXZ());
        assertEquals(1713564485, chunk.getLastMCAUpdate());

        assertFalse(iter.hasNext());
    }

    public void testIteratePoiFile() throws IOException {
        validateIteratePoiFile(LoadFlags.LOAD_ALL_DATA);
        validateIteratePoiFile(LoadFlags.RAW);
    }


    public void validateIterateRegionFile(long loadFlags) throws IOException {
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(
                getResourceFile("1_20_4/region/r.-3.-3.mca"), loadFlags, TerrainChunk::new
        );
//        System.out.println(iter);
        assertTrue(iter.hasNext());
        TerrainChunk chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-91, -87), chunk.getChunkXZ());
        assertEquals(1713564480, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-95, -86), chunk.getChunkXZ());
        assertEquals(1713564471, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-94, -86), chunk.getChunkXZ());
        assertEquals(1713564470, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-95, -85), chunk.getChunkXZ());
        assertEquals(1713564471, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-94, -85), chunk.getChunkXZ());
        assertEquals(1713564471, chunk.getLastMCAUpdate());

        assertFalse(iter.hasNext());
    }

    public void testIterateRegionFile() throws IOException {
        validateIterateRegionFile(LoadFlags.LOAD_ALL_DATA);
        validateIterateRegionFile(LoadFlags.RAW);
    }


    public void validateIterateEntitiesFile(long loadFlags) throws IOException {
        McaFileChunkIterator<EntitiesChunk> iter = McaFileChunkIterator.iterate(
                getResourceFile("1_20_4/entities/r.-3.-3.mca"), loadFlags, EntitiesChunk::new
        );
//        System.out.println(iter);
        assertTrue(iter.hasNext());
        EntitiesChunk chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-91, -87), chunk.getChunkXZ());
        assertEquals(1713564491, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-95, -86), chunk.getChunkXZ());
        assertEquals(1713564491, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-94, -86), chunk.getChunkXZ());
        assertEquals(1713564491, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-95, -85), chunk.getChunkXZ());
        assertEquals(1713564491, chunk.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        chunk = iter.next();
        assertNotNull(chunk);
        assertEquals(IntPointXZ.XZ(-94, -85), chunk.getChunkXZ());
        assertEquals(1713564491, chunk.getLastMCAUpdate());

        assertFalse(iter.hasNext());
    }

    public void testIterateEntitiesFile() throws IOException {
        validateIterateEntitiesFile(LoadFlags.LOAD_ALL_DATA);
//        validateIterateEntitiesFile(LoadFlags.RAW);
    }
}
