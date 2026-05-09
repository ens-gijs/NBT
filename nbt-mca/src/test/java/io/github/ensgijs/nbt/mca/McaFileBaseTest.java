package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.util.IntPointXZ;

// TODO: implement abstract test pattern for McaFileBase & refactor MCAFileTest like mad
public class McaFileBaseTest extends McaTestCase {
    public void testGetRelativeChunkXZ() {
        // few enough iterations to just be lazy and do an exhaustive test
        for (int i = 0; i < 1024; i++) {
            IntPointXZ xz = McaFileBase.getRelativeChunkXZ(i);
            assertEquals(i, McaFileBase.getChunkIndex(xz.getX(), xz.getZ()));
        }
        assertThrowsException(() -> McaFileBase.getRelativeChunkXZ(-1), IndexOutOfBoundsException.class);
        assertThrowsException(() -> McaFileBase.getRelativeChunkXZ(1024), IndexOutOfBoundsException.class);
    }

    public void testGetChunkIndex() {
        assertEquals(0, McaFileBase.getChunkIndex(0, 0));
        assertEquals(0, McaFileBase.getChunkIndex(512, 512));
        assertEquals(0, McaFileBase.getChunkIndex(-512, -512));

        assertEquals(1023, McaFileBase.getChunkIndex(511, 511));

        assertEquals(31, McaFileBase.getChunkIndex(31, 0));
        assertEquals(32, McaFileBase.getChunkIndex(0, 1));

        assertEquals(-1, McaFileBase.getChunkIndex(ChunkBase.NO_CHUNK_COORD_SENTINEL, ChunkBase.NO_CHUNK_COORD_SENTINEL));
        assertEquals(-1, McaFileBase.getChunkIndex(ChunkBase.NO_CHUNK_COORD_SENTINEL, 0));
        assertEquals(-1, McaFileBase.getChunkIndex(0, ChunkBase.NO_CHUNK_COORD_SENTINEL));
    }
}
