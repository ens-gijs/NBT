package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.io.TextNbtParser;
import io.github.ensgijs.nbt.mca.*;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.regex.Pattern;

import io.github.ensgijs.nbt.mca.io.RandomAccessMcaFile.SectorManager;
import io.github.ensgijs.nbt.mca.io.RandomAccessMcaFile.SectorManager.SectorBlock;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;
import io.github.ensgijs.nbt.mca.util.PalettizedCuboid;
import io.github.ensgijs.nbt.tag.CompoundTag;

public class RandomAccessMcaFileTest extends McaTestCase {

    public void testSectorManager_sanity() throws IOException {
        SectorManager sm = new SectorManager();
        assertEquals(2, sm.appendAtSector);  // default is sector 2

        int[] sectorTable = new int[1024];
        sectorTable[0] = new SectorBlock(5, 1).pack();
        sectorTable[33] = new SectorBlock(18, 1).pack();
        sectorTable[64] = new SectorBlock(9, 4).pack();
        sectorTable[1] = new SectorBlock(2, 1).pack();
        sectorTable[32] = new SectorBlock(3, 2).pack();
        sm.sync(sectorTable);

        assertEquals(19, sm.appendAtSector);
        assertEquals(2, sm.freeSectors.size());
        assertEquals(new SectorBlock(6, 3), sm.freeSectors.get(0));
        assertEquals(new SectorBlock(13, 5), sm.freeSectors.get(1));

        // take from first free block
        assertEquals(new SectorBlock(6, 1), sm.allocate(1));
        assertEquals(2, sm.freeSectors.size());
        assertEquals(new SectorBlock(7, 2), sm.freeSectors.get(0));
        assertEquals(new SectorBlock(13, 5), sm.freeSectors.get(1));
        assertEquals(19, sm.appendAtSector);

        // take from second free block
        assertEquals(new SectorBlock(13, 4), sm.allocate(4));
        assertEquals(2, sm.freeSectors.size());
        assertEquals(new SectorBlock(7, 2), sm.freeSectors.get(0));
        assertEquals(new SectorBlock(17, 1), sm.freeSectors.get(1));
        assertEquals(19, sm.appendAtSector);

        // no free block big enough - take off the end
        assertEquals(new SectorBlock(19, 4), sm.allocate(4));
        assertEquals(2, sm.freeSectors.size());
        assertEquals(new SectorBlock(7, 2), sm.freeSectors.get(0));
        assertEquals(new SectorBlock(17, 1), sm.freeSectors.get(1));
        assertEquals(23, sm.appendAtSector);

        // release and merge into second free block
        sm.release(13, 4);
        assertEquals(2, sm.freeSectors.size());
        assertEquals(new SectorBlock(7, 2), sm.freeSectors.get(0));
        assertEquals(new SectorBlock(13, 5), sm.freeSectors.get(1));
        assertEquals(23, sm.appendAtSector);

        // release and merge into second free block case 2
        sm.release(18, 1);
        assertEquals(2, sm.freeSectors.size());
        assertEquals(new SectorBlock(7, 2), sm.freeSectors.get(0));
        assertEquals(new SectorBlock(13, 6), sm.freeSectors.get(1));
        assertEquals(23, sm.appendAtSector);

        // release last block which touches the current appendAtSector
        sm.release(19, 4);
        assertEquals(13, sm.appendAtSector);
        assertEquals(1, sm.freeSectors.size());
        assertEquals(new SectorBlock(7, 2), sm.freeSectors.get(0));

        // taking the last free sector should be safe too
        assertEquals(new SectorBlock(7, 1), sm.allocate(1));
        assertEquals(new SectorBlock(8, 1), sm.allocate(1));
        assertEquals(0, sm.freeSectors.size());
        assertEquals(13, sm.appendAtSector);

        // allocating with no free sectors also works
        assertEquals(new SectorBlock(13, 1), sm.allocate(1));
        assertEquals(0, sm.freeSectors.size());
        assertEquals(14, sm.appendAtSector);


        // nothing in table in sector 2
        sectorTable = new int[1024];
        sectorTable[547] = new SectorBlock(5, 1).pack();
        sm.sync(sectorTable);
        assertEquals(1, sm.freeSectors.size());
        assertEquals(new SectorBlock(2, 3), sm.freeSectors.get(0));
        assertEquals(6, sm.appendAtSector);


        // release between free sectors
        sm.freeSectors.clear();
        sm.freeSectors.add(new SectorBlock(2, 1));
        sm.freeSectors.add(new SectorBlock(20, 1));
        sm.appendAtSector = 42;
        sm.release(10, 2);

        assertEquals(3, sm.freeSectors.size());
        assertEquals(new SectorBlock(10, 2), sm.freeSectors.get(1));
    }

    public void testSectorManager_scan_throwsWhenGivenWrongSizedArray() {
        assertThrowsException(() -> new SectorManager().sync(null), NullPointerException.class);
        assertThrowsException(() -> new SectorManager().sync(new int[256]), IllegalArgumentException.class);
        assertThrowsException(() -> new SectorManager().sync(new int[4096]), IllegalArgumentException.class);
    }

    public void testHasChunkAbsolute() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        assertTrue(poiMca.hasChunkAbsolute(-77, -84));
        assertTrue(poiMca.hasChunkAbsolute(new IntPointXZ(-77, -73)));
        assertTrue(poiMca.hasChunkAbsolute(-94, -71));
        assertTrue(poiMca.hasChunkAbsolute(-78, -70));
        assertTrue(poiMca.hasChunkAbsolute(-77, -68));
        assertTrue(poiMca.hasChunkAbsolute(-82, -67));

        assertFalse(poiMca.hasChunkAbsolute(0, 0));  // out of bounds doesn't throw
        assertFalse(poiMca.hasChunkAbsolute(new IntPointXZ(-82, -70)));
        poiMca.close();
    }

    public void testHasChunkRelative() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        assertTrue(poiMca.hasChunkRelative(19, 12));
        assertTrue(poiMca.hasChunkRelative(19, 23));
        assertFalse(poiMca.hasChunkRelative(0, 0));
        assertFalse(poiMca.hasChunkRelative(31, 31));
        assertThrowsException(() -> poiMca.hasChunkRelative(new IntPointXZ(-1, 0)), IndexOutOfBoundsException.class);
        poiMca.close();
    }

    public void testReadWriteReadIdempotency() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        PoiChunk chunksA[] = {
            poiMca.readAbsolute(-77, -84),
            poiMca.readAbsolute(-77, -73),
            poiMca.readAbsolute(-94, -71),
            poiMca.readAbsolute(-78, -70),
            poiMca.readAbsolute(-77, -68),
            poiMca.readAbsolute(-82, -67)};
        poiMca.write(chunksA);
        assertEquals(0, poiMca.optimizeFile());
        poiMca.flush();
        poiMca.close();
        poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        PoiChunk chunksB[] = {
                poiMca.readAbsolute(-77, -84),
                poiMca.readAbsolute(-77, -73),
                poiMca.readAbsolute(-94, -71),
                poiMca.readAbsolute(-78, -70),
                poiMca.readAbsolute(-77, -68),
                poiMca.readAbsolute(-82, -67)};

        for (int i = 0; i < chunksA.length; i++) {
            assertEquals(chunksA[i].getHandle(), chunksB[i].getHandle());
        }
        poiMca.close();
    }

    public void testRemoveChunkAbsolute() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        assertTrue(poiMca.removeChunkAbsolute(new IntPointXZ(-77, -84)));
        assertTrue(poiMca.removeChunkAbsolute(-94, -71));
        assertTrue(poiMca.removeChunkAbsolute(-78, -70));
        assertTrue(poiMca.removeChunkAbsolute(-82, -67));
        assertFalse(poiMca.removeChunkAbsolute(1, -2));  // out of bounds
        assertFalse(poiMca.removeChunkAbsolute(-90, -70));  // doesn't exist

        assertFalse(poiMca.hasChunkAbsolute(-77, -84));
        assertTrue(poiMca.hasChunkAbsolute(-77, -73));
        assertFalse(poiMca.hasChunkAbsolute(-94, -71));
        assertFalse(poiMca.hasChunkAbsolute(-78, -70));
        assertTrue(poiMca.hasChunkAbsolute(-77, -68));
        assertFalse(poiMca.hasChunkAbsolute(-82, -67));
        assertTrue(poiMca.optimizeFile() > 0);
        poiMca.close();

        poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        assertFalse(poiMca.hasChunkAbsolute(-77, -84));
        assertTrue(poiMca.hasChunkAbsolute(-77, -73));
        assertFalse(poiMca.hasChunkAbsolute(-94, -71));
        assertFalse(poiMca.hasChunkAbsolute(-78, -70));
        assertTrue(poiMca.hasChunkAbsolute(-77, -68));
        assertFalse(poiMca.hasChunkAbsolute(-82, -67));
        poiMca.close();
    }

    public void testRemoveChunkRelative() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        assertTrue(poiMca.removeChunkRelative(new IntPointXZ(19, 12)));
        assertTrue(poiMca.removeChunkRelative(19, 23));
        assertFalse(poiMca.removeChunkRelative(new IntPointXZ(0, 0)));
        assertFalse(poiMca.removeChunkRelative(31, 31));
        assertThrowsException(() -> poiMca.removeChunkRelative(new IntPointXZ(-1, 0)), IndexOutOfBoundsException.class);
        assertThrowsException(() -> poiMca.removeChunkRelative(31, 32), IndexOutOfBoundsException.class);
        poiMca.close();
    }

    public void testRemoveChunkRelative_outOfBoundsThrows() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        assertThrowsException(() -> poiMca.removeChunkRelative(32, 32), IndexOutOfBoundsException.class);
        poiMca.close();
    }

    public void testRemoveChunk_removingAllChunks() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        assertTrue(poiMca.removeChunkRelative(14, 29));
        assertTrue(poiMca.removeChunkAbsolute(-77, -73));
        assertTrue(poiMca.removeChunkAbsolute(-94, -71));
        assertTrue(poiMca.removeChunkAbsolute(-78, -70));
        assertTrue(poiMca.removeChunkAbsolute(-77, -68));
        assertTrue(poiMca.removeChunkRelative(19, 12));
        assertTrue(poiMca.optimizeFile() > 0);
        poiMca.close();
        assertEquals(2 * 4096, Files.size(file.toPath()));
    }

    public void testChunkSectorTableToString() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        assertEquals(1024 - 6L, Pattern.compile("\\s----(?=\\s)").matcher(poiMca.chunkSectorTableToString()).results().count());
        poiMca.close();

    }

    public void testCanBeUsedToCreateNewMcaFile_empty() throws IOException {
        File file = getNewTmpFile("poi/r.1.2.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        assertEquals(new IntPointXZ(1, 2), poiMca.getRegionXZ());
        poiMca.touch();
        poiMca.close();
        assertEquals(2 * 4096, Files.size(file.toPath()));
    }

    public void testGetChunkTimestamp() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        assertEquals(1713564485, poiMca.getChunkTimestampRelative(new IntPointXZ(14, 29)));
        assertEquals(1713564485, poiMca.getChunkTimestampAbsolute(new IntPointXZ(-77, -73)));
        assertEquals(-1, poiMca.getChunkTimestampAbsolute(-900, -70));  // out of bounds
        assertEquals(-1, poiMca.getChunkTimestampAbsolute(-90, -70));  // in bounds, doesn't exist
        assertEquals(-1, poiMca.getChunkTimestampRelative(25, 17));  // in bounds, doesn't exist
        assertThrowsException(() -> poiMca.getChunkTimestampRelative(new IntPointXZ(-1, 29)), IndexOutOfBoundsException.class);
        poiMca.close();
    }

    public void testRead_indexOutOfBounds() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        assertThrowsException(() -> poiMca.read(-1), IndexOutOfBoundsException.class);
        assertThrowsException(() -> poiMca.read(1024), IndexOutOfBoundsException.class);
        assertThrowsException(() -> poiMca.readRelative(new IntPointXZ(0, 32)), IndexOutOfBoundsException.class);
        assertThrowsException(() -> poiMca.readAbsolute(new IntPointXZ(0, 0)), IndexOutOfBoundsException.class);
        poiMca.close();
    }

    public void testRead_chunkSectorPointsOutsideFile_throwsEOF() throws IOException {
        File file = getNewTmpFile("r.0.0.mca");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(4096 * 2);
        raf.writeInt(0x0201);
        raf.close();

        assertEquals(2 * 4096, Files.size(file.toPath()));
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        assertThrowsException(() -> poiMca.read(0), EOFException.class);
        assertThrowsException(poiMca::optimizeFile, IOException.class);
        assertThrowsNoException(poiMca::close);
        assertTrue(poiMca.fileFinalized);
    }

    public void testRead_encodedChunkSizeTooLargeThrows() throws IOException {
        File file = getNewTmpFile("r.0.0.mca");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(4096 * 4);
        raf.writeInt(0x0201);
        raf.seek(4096 * 2);
        raf.writeInt(5000);
        raf.close();

        assertEquals(4 * 4096, Files.size(file.toPath()));
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "r");
        assertThrowsException(() -> poiMca.read(0), CorruptMcaFileException.class);
        poiMca.close();
    }

    public void testWrite_chunkOutOfBoundsThrows() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        var chunk = poiMca.readAbsolute(-77, -73);
        chunk.moveChunk(0, 0, MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS);
        assertThrowsException(() -> poiMca.write(chunk), IndexOutOfBoundsException.class);
        poiMca.close();
    }


    public void testWrite_chunkXzNotSetThrows() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        var chunk = new PoiChunk(TextNbtParser.parseInline("{DataVersion: 3700, Sections: {}}"));
        assertThrowsException(() -> {
            try {
                poiMca.write(chunk);
            } catch (IOException e) {
                fail();
            }
            return null;
        }, IllegalArgumentException.class, s -> s.equals("Chunk XZ must be set!"));
        poiMca.close();
    }

    public void testWrite_chunkWrittenForFirstTime() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/poi/r.-3.-3.mca");
        var poiMca = new RandomAccessMcaFile<>(PoiChunk.class, file, "rw");
        var chunk = new PoiChunk(TextNbtParser.parseInline("{DataVersion: 3700, Sections: {}}"));
        chunk.moveChunk(-90, -70, 0);
        poiMca.write(chunk);
        assertTrue(poiMca.hasChunkAbsolute(-90, -70));
        poiMca.close();
    }

    public void testWrite_chunkSizeReduced_placedInPreviousSectorAndRemainderSectorsReleased() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/region/r.-3.-3.mca");
        var terrainMca = new RandomAccessMcaFile<>(TerrainChunk.class, file, "rw");
        terrainMca.touch();
        final int index = McaFileBase.getChunkIndex(5, 9);
        assertEquals(0x0202, terrainMca.chunkSectors[index]);
        TerrainChunk chunk = terrainMca.read(index);
        assertNotNull(chunk);
        assertEquals(new IntPointXZ(5 - 3 * 32, 9 - 3 * 32), chunk.getChunkXZ());
        for (var sector: chunk) {
            sector.getBlockStates().fill(TextNbtParser.parseInline("{Name: \"minecraft:air\"}"));
        }
        terrainMca.write(chunk);
        assertEquals(0x0201, terrainMca.chunkSectors[index]);
        assertEquals(SectorBlock.unpack(0x0301), terrainMca.sectorManager.freeSectors.getFirst());
        terrainMca.close();
    }

    public void testWrite_chunkSizeIncreased_placedAtEndOfFile() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/region/r.-3.-3.mca");
        var terrainMca = new RandomAccessMcaFile<>(TerrainChunk.class, file, "rw");
        terrainMca.touch();
        final int index = McaFileBase.getChunkIndex(5, 9);
        assertEquals(0x0202, terrainMca.chunkSectors[index]);
        TerrainChunk chunk = terrainMca.read(index);
        assertNotNull(chunk);
        assertEquals(new IntPointXZ(5 - 3 * 32, 9 - 3 * 32), chunk.getChunkXZ());
        PalettizedCuboid<CompoundTag> bigSection = new PalettizedCuboid<>(16, TextNbtParser.parseInline("{Name: \"minecraft:air\"}"));
        for (int i = 0; i < 16 * 16 * 16; i++) {
            bigSection.set(i, TextNbtParser.parseInline("{Name: \"minecraft:random_garbage_" + String.format("%d%X", i, -i) + "\"}"));
        }
        chunk.getSection(8).setBlockStates(bigSection);
        terrainMca.write(chunk);
        assertEquals(0x0C0A, terrainMca.chunkSectors[index]);
        assertEquals(SectorBlock.unpack(0x0202), terrainMca.sectorManager.freeSectors.getFirst());
        terrainMca.close();
    }

    public void testReadOnly_writeThrows() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/region/r.-3.-3.mca");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        var terrainMca = new RandomAccessMcaFile<>(TerrainChunk.class, raf, IntPointXZ.XZ(-3, -3), "r");
        terrainMca.touch();
        TerrainChunk chunk = terrainMca.readRelative(5, 9);
        assertNotNull(chunk);
        assertThrowsException(() -> terrainMca.write(chunk), IOException.class);
        terrainMca.close();
    }

    public void testReadOnly_optimizeFileThrows() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/region/r.-3.-3.mca");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        var terrainMca = new RandomAccessMcaFile<>(TerrainChunk.class, raf, IntPointXZ.XZ(-3, -3), "r");
        assertThrowsException(() -> terrainMca.optimizeFile(), IOException.class);
        terrainMca.close();
    }

    public void testReadOnly_flushDoesNotThrow() throws IOException {
        File file = super.copyResourceToTmp("1_20_4/region/r.-3.-3.mca");
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        var terrainMca = new RandomAccessMcaFile<>(TerrainChunk.class, raf, IntPointXZ.XZ(-3, -3), "r");
        assertThrowsNoException(terrainMca::flush);
        terrainMca.close();
    }
}
