package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.TextNbtHelpers;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;
import io.github.ensgijs.nbt.mca.util.RegionBoundingRectangle;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.IntArrayTag;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public abstract class TerrainChunkBaseTest<T extends TerrainChunkBase> extends ChunkBaseTest<T> {

    public void testMoveBoundingBox() {
        IntArrayTag bb = new IntArrayTag(1, 2, 3, 4, 5, 6);
        assertTrue(TerrainChunkBase.moveBoundingBox(
                bb,
                new IntPointXZ(-12, 42),
                new RegionBoundingRectangle(0, 0)
        ));
        assertArrayEquals(new int[] {}, bb.getValue());

        bb = new IntArrayTag(1, 2, 3, 4, 5, 6);
        assertTrue(TerrainChunkBase.moveBoundingBox(
                bb,
                new IntPointXZ(-12, 42),
                new RegionBoundingRectangle(-1, 0)
        ));
        assertArrayEquals(new int[] {1 - 12, 2, 3 + 42, 4 - 12, 5, 6 + 42}, bb.getValue());
    }

    public void testMoveStructureStart_happyCase() throws IOException {
        T chunk = createChunk(DataVersion.latest());
        CompoundTag startsTag = TextNbtHelpers.readTextNbtFile(getResourceFile("chunk_mover_samples/single_start_record_with_all_xz_fields.r.-3.-3.c.-95.-85.snbt")).getTagAutoCast();
        IntPointXZ regionDeltaXZ = new IntPointXZ(3, 3);
        assertTrue(chunk.moveStructureStart(
                startsTag,
                regionDeltaXZ.transformRegionToChunk(),
                regionDeltaXZ.transformRegionToBlock(),
                new RegionBoundingRectangle(0, 0)));
//        System.out.println(TextNbtHelpers.toTextNbt(startsTag));
        CompoundTag expectedStartsTag = TextNbtHelpers.readTextNbtFile(getResourceFile("chunk_mover_samples/single_start_record_with_all_xz_fields.r.0.0.c.1.11.snbt")).getTagAutoCast();
        assertEquals(expectedStartsTag, startsTag);
    }

    public void testMoveStructureStart_structOutOfRegionBounds() throws IOException {
        T chunk = createChunk(DataVersion.latest());
        CompoundTag startsTag = TextNbtHelpers.readTextNbtFile(getResourceFile("chunk_mover_samples/single_start_record_with_all_xz_fields.r.-3.-3.c.-95.-85.snbt")).getTagAutoCast();
        IntPointXZ regionDeltaXZ = new IntPointXZ(3, 3);
        assertTrue(chunk.moveStructureStart(
                startsTag,
                regionDeltaXZ.transformRegionToChunk(),
                regionDeltaXZ.transformRegionToBlock(),
                new RegionBoundingRectangle(1, 1)));  // an out-of-bounds area
//        System.out.println(TextNbtHelpers.toTextNbt(startsTag));
        assertEquals(1, startsTag.size());
        assertEquals("INVALID", startsTag.getString("id"));
    }

    public void testMoveStructureStart_structPartiallyOutOfRegionBounds_clipped() throws IOException {
        T chunk = createChunk(DataVersion.latest());
        CompoundTag startsTag = TextNbtHelpers.readTextNbtFile(getResourceFile("chunk_mover_samples/single_start_record_with_all_xz_fields.r.-3.-3.c.-95.-85.snbt")).getTagAutoCast();
        // specify that we want to move the chunk to 0 0
        IntPointXZ chunkDeltaXZ = new IntPointXZ(-startsTag.getInt("ChunkX"), -startsTag.getInt("ChunkZ"));
        assertTrue(chunk.moveStructureStart(
                startsTag,
                chunkDeltaXZ,
                chunkDeltaXZ.transformChunkToBlock(),
                new RegionBoundingRectangle(0, 0)));
//        System.out.println(TextNbtHelpers.toTextNbt(startsTag));
        CompoundTag expectedStartsTag = TextNbtHelpers.readTextNbtFile(getResourceFile("chunk_mover_samples/single_start_record_with_all_xz_fields.r.0.0.c.0.0_POST_MOVE_CLIPPED.snbt")).getTagAutoCast();
        assertEquals(expectedStartsTag, startsTag);
    }

    public void testMoveStructureStart_structPartiallyOutOfRegionBounds_noclip() throws IOException {
        T chunk = createChunk(DataVersion.latest());
        CompoundTag startsTag = TextNbtHelpers.readTextNbtFile(getResourceFile("chunk_mover_samples/single_start_record_with_all_xz_fields.r.-3.-3.c.-95.-85.snbt")).getTagAutoCast();
        // specify that we want to move the chunk to 0 0
        IntPointXZ chunkDeltaXZ = new IntPointXZ(-startsTag.getInt("ChunkX"), -startsTag.getInt("ChunkZ"));
        assertTrue(chunk.moveStructureStart(
                startsTag,
                chunkDeltaXZ,
                chunkDeltaXZ.transformChunkToBlock(),
                null));  // no clipping box
//        System.out.println(TextNbtHelpers.toTextNbt(startsTag));
        CompoundTag expectedStartsTag = TextNbtHelpers.readTextNbtFile(getResourceFile("chunk_mover_samples/single_start_record_with_all_xz_fields.r.0.0.c.0.0_POST_MOVE_NOCLIP.snbt")).getTagAutoCast();
        assertEquals(expectedStartsTag, startsTag);
    }
}
