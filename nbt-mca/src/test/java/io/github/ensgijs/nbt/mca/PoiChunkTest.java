package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.TextNbtParser;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.MoveChunkFlags;
import io.github.ensgijs.nbt.query.NbtPath;
import io.github.ensgijs.nbt.tag.CompoundTag;

import java.io.IOException;

public class PoiChunkTest extends PoiChunkBaseTest<PoiRecord, PoiChunk> {

    @Override
    protected PoiChunk createChunk(DataVersion dataVersion) {
        return new PoiChunk(dataVersion.id());
    }

    @Override
    protected PoiChunk createChunk(CompoundTag tag) {
        return new PoiChunk(tag);
    }

    @Override
    protected PoiChunk createChunk(CompoundTag tag, long loadFlags) {
        return new PoiChunk(tag, loadFlags);
    }

    @Override
    protected PoiRecord createPoiRecord(int x, int y, int z, String type) {
        return new PoiRecord(x, y, z, type);
    }


    public void testMoveChunk_viaPoiRecords() {
        PoiChunk chunk = createFilledChunk(0, 0, DEFAULT_TEST_VERSION);
        PoiRecord record = new PoiRecord(1, 200, 3, "whatever");
        chunk.add(record);
        assertTrue(chunk.moveChunk(-3, 4, MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS));
        assertEquals((-3 * 16) + 1, record.x);
        assertEquals(200, record.y);
        assertEquals((4 * 16) + 3, record.z);
    }

    public void testMoveChunk_poiRecordsNotParsed() throws IOException {
        final String nbtText = "{DataVersion:3700,Sections:{\"5\":{Records:[{free_tickets:0,pos:[I;-1219,83,-1339],type:\"minecraft:bee_nest\"}],Valid:1b}}}";
        PoiChunk chunk = new PoiChunk(TextNbtParser.parseInline(nbtText), LoadFlags.RAW);
        chunk.chunkX = -77;
        chunk.chunkZ = -84;
        assertTrue(chunk.moveChunk(-3, 4, MoveChunkFlags.MOVE_CHUNK_DEFAULT_FLAGS));
//        System.out.println(TextNbtHelpers.toTextNbt(chunk.getHandle()));
        int[] newPos = NbtPath.of("Sections.5.Records[0].pos").getIntArray(chunk.getHandle());
        assertEquals(-35, newPos[0]);
        assertEquals(83, newPos[1]);
        assertEquals(69, newPos[2]);
    }
}
