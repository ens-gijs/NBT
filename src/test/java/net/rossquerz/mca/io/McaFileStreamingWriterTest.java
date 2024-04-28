package net.rossquerz.mca.io;

import net.rossquerz.mca.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static net.rossquerz.mca.util.IntPointXZ.XZ;

public class McaFileStreamingWriterTest extends McaTestCase {
    public void testWriteTerrainChunk() throws IOException {
        File file = getNewTmpFile("streaming_writer/region/r.0.1.mca");
        McaFileStreamingWriter<TerrainChunk> writer = new McaFileStreamingWriter<>(file);

        final TerrainChunk chunk1 = new TerrainChunk();
        chunk1.setLastMCAUpdate(12345678);
        chunk1.updateHandle(0, 32);
        writer.write(chunk1);

        final TerrainChunk chunkF = new TerrainChunk();
        chunkF.setLastMCAUpdate(87654321);
        chunkF.updateHandle(0, 32);
        assertThrowsException(() -> writer.write(chunkF), IOException.class);

        final TerrainChunk chunk2 = new TerrainChunk();
        chunk2.setLastMCAUpdate(54321678);
        chunk2.updateHandle(5, 32 + 3);
        writer.write(chunk2);
        writer.close();

        assertEquals(4 * 4096, Files.size(file.toPath()));

        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(
                TerrainChunk::new,
                file,
                LoadFlags.LOAD_ALL_DATA);

        assertTrue(iter.hasNext());
        final TerrainChunk chunk1in = iter.next();
        assertEquals(XZ(0, 32), chunk1in.getChunkXZ());
        assertEquals(12345678, chunk1in.getLastMCAUpdate());

        assertTrue(iter.hasNext());
        final TerrainChunk chunk2in = iter.next();
        assertEquals(XZ(5, 32 + 3), chunk2in.getChunkXZ());
        assertEquals(54321678, chunk2in.getLastMCAUpdate());

        assertFalse(iter.hasNext());

        McaRegionFile mca = McaFileHelpers.readAuto(file);
        assertNotNull(mca);
        assertNotNull(mca.getChunk(0, 32));
        assertEquals(12345678, mca.getChunk(0, 32).getLastMCAUpdate());
        assertNotNull(mca.getChunk(5, 32 + 3));
        assertEquals(54321678, mca.getChunk(5, 32 + 3).getLastMCAUpdate());
    }
}
