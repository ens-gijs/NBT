package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.mca.EntitiesChunk;
import io.github.ensgijs.nbt.mca.McaTestCase;
import io.github.ensgijs.nbt.mca.PoiChunk;
import io.github.ensgijs.nbt.mca.TerrainChunk;
import io.github.ensgijs.nbt.mca.entities.Entity;
import io.github.ensgijs.nbt.mca.entities.EntityFactory;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;
import io.github.ensgijs.nbt.query.NbtPath;
import io.github.ensgijs.nbt.tag.CompoundTag;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class RegionFileRelocatorTest extends McaTestCase {

    public void testRelocate_1_20_4() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_20_4").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(-3, -3, 0, 0));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(1, relocator.entitiesFilesRelocated());
        assertEquals(1, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_20_4"));
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(5, chunk.getChunkX());
        assertEquals(9, chunk.getChunkZ());
        IntPointXZ xz = IntPointXZ.unpack(NbtPath.of("References.minecraft:mineshaft[0]").getLong(chunk.getStructures()));
        assertEquals(new IntPointXZ(5, 9), xz);
        int[] bb = NbtPath.of("starts.minecraft:mineshaft.Children[0].BB").getIntArray(chunk.getStructures());
        assertArrayEquals(new int[] {82, 34, 146, 92, 39, 155}, bb);
        bb = NbtPath.of("starts.minecraft:mineshaft.Children[0].Entrances[1]").getIntArray(chunk.getStructures());
        assertArrayEquals(new int[] {1536 - 1451, 35, 1536 - 1382, 1536 - 1447, 37, 1536 - 1381}, bb);

        newMca = Paths.get(outRoot.getPath(), "entities", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_20_4"));

        newMca = Paths.get(outRoot.getPath(), "poi", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_20_4"));
    }

    public void testRelocateAll_1_18_1() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_18_1").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertEquals(2, relocator.relocateAll(10, 10));
        assertEquals(2, relocator.regionFilesRelocated());
        assertEquals(2, relocator.entitiesFilesRelocated());
        assertEquals(1, relocator.poiFilesRelocated());

        File newMca = Paths.get(outRoot.getPath(), "region", "r.10.8.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_18_1"));
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(339, chunk.getChunkX());
        assertEquals(273, chunk.getChunkZ());

        CompoundTag tileTag = NbtPath.of("block_entities[0]").getTag(chunk.getHandle());
        assertEquals(5431, tileTag.getInt("x"));
        assertEquals(4381, tileTag.getInt("z"));
        IntPointXZ xz = IntPointXZ.unpack(NbtPath.of("References.village[0]").getLong(chunk.getStructures()));
        assertEquals(new IntPointXZ(0x154, 0x111), xz);


        newMca = Paths.get(outRoot.getPath(), "region", "r.18.11.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_18_1"));
        iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        chunk = iter.next();
        assertEquals(595, chunk.getChunkX());
        assertEquals(353, chunk.getChunkZ());

        CompoundTag blockTicksTag = NbtPath.of("block_ticks[0]").getTag(chunk.getHandle());
        assertEquals(9526, blockTicksTag.getInt("x"));
        assertEquals(5650, blockTicksTag.getInt("z"));


        newMca = Paths.get(outRoot.getPath(), "entities", "r.10.8.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_18_1"));
        McaFileChunkIterator<EntitiesChunk> eiter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        EntitiesChunk echunk = eiter.next();
        List<Entity> entities = echunk.getEntities();
        assertEquals(5437, (int) entities.get(3).getX());
        assertEquals(64, (int) entities.get(3).getY());
        assertEquals(4368, (int) entities.get(3).getZ());
        int[] potential_job_site = NbtPath.of("Brain.memories.minecraft:potential_job_site.value.pos").getIntArray(entities.get(3).getHandle());
        assertArrayEquals(new int[] {5433, 68, 4377}, potential_job_site);


        newMca = Paths.get(outRoot.getPath(), "entities", "r.18.11.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_18_1"));

        newMca = Paths.get(outRoot.getPath(), "poi", "r.10.8.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_18_1"));
        McaFileChunkIterator<PoiChunk> piter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        PoiChunk poiChunk = piter.next();
        int[] pos = NbtPath.of("Sections.4.Records[0].pos").getIntArray(poiChunk.getHandle());
        assertArrayEquals(new int[] {5433, 65, 4371}, pos);
    }

    public void testRelocate_1_17_1() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_17_1").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(-3, -2, 0, 0));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(1, relocator.entitiesFilesRelocated());
        assertEquals(1, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_17_1"));
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(31, chunk.getChunkX());
        assertEquals(22, chunk.getChunkZ());

        CompoundTag tileTag = NbtPath.of("Level.TileEntities[0]").getTag(chunk.getHandle());
        assertEquals(506, tileTag.getInt("x"));
        assertEquals(358, tileTag.getInt("z"));
        IntPointXZ xz = IntPointXZ.unpack(NbtPath.of("References.village[0]").getLong(chunk.getStructures()));
        assertEquals(new IntPointXZ(33, 23), xz);


        newMca = Paths.get(outRoot.getPath(), "entities", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_17_1"));
        McaFileChunkIterator<EntitiesChunk> eiter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        EntitiesChunk echunk = eiter.next();
        List<Entity> entities = echunk.getEntities();
        assertEquals(504, (int) entities.get(0).getX());
        assertEquals(64, (int) entities.get(0).getY());
        assertEquals(357, (int) entities.get(0).getZ());
        int[] homePos = NbtPath.of("Brain.memories.minecraft:home.value.pos").getIntArray(entities.get(0).getHandle());
        assertArrayEquals(new int[] {504, 63, 354}, homePos);

        newMca = Paths.get(outRoot.getPath(), "poi", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_17_1"));
        McaFileChunkIterator<PoiChunk> piter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        PoiChunk poiChunk = piter.next();
        int[] pos = NbtPath.of("Sections.3.Records[0].pos").getIntArray(poiChunk.getHandle());
        assertArrayEquals(new int[] {504, 63, 354}, pos);

    }

    public void testRelocate_1_16_5() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_16_5").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(0, -1, -1, 1));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(1, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.-1.1.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_16_5"));
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(-28, chunk.getChunkX());
        assertEquals(37, chunk.getChunkZ());

        List<Entity> entities = EntityFactory.fromListTag(chunk.getEntities(), chunk.getDataVersion());
        assertEquals(-441, (int)entities.get(0).getX());
        assertEquals(34, (int)entities.get(0).getY());
        assertEquals(603, (int)entities.get(0).getZ());

        CompoundTag tileTag = NbtPath.of("Level.TileEntities[0]").getTag(chunk.getHandle());
        assertEquals(-441, tileTag.getInt("x"));
        assertEquals(603, tileTag.getInt("z"));
        IntPointXZ xz = IntPointXZ.unpack(NbtPath.of("References.mineshaft[0]").getLong(chunk.getStructures()));
        assertEquals(new IntPointXZ(-27, 39), xz);


        newMca = Paths.get(outRoot.getPath(), "poi", "r.-1.1.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_16_5"));
        McaFileChunkIterator<PoiChunk> piter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        PoiChunk poiChunk = piter.next();
        int[] pos = NbtPath.of("Sections.4.Records[0].pos").getIntArray(poiChunk.getHandle());
        assertArrayEquals(new int[] {-439, 73, 595}, pos);
    }

    public void testRelocateAll_1_15_2() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_15_2").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertEquals(2, relocator.relocateAll(5, 5));
        assertEquals(2, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(1, relocator.poiFilesRelocated());

        File newMca = Paths.get(outRoot.getPath(), "region", "r.4.5.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_15_2"));

        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(157, chunk.getChunkX());
        assertEquals(171, chunk.getChunkZ());

        List<Entity> entities = EntityFactory.fromListTag(chunk.getEntities(), chunk.getDataVersion());
        assertEquals(2517, (int)entities.get(0).getX());
        assertEquals(67, (int)entities.get(0).getY());
        assertEquals(2745, (int)entities.get(0).getZ());

        CompoundTag tileTag = NbtPath.of("Level.TileEntities[0]").getTag(chunk.getHandle());
        assertEquals(2521, tileTag.getInt("x"));
        assertEquals(2748, tileTag.getInt("z"));
        IntPointXZ xz = IntPointXZ.unpack(NbtPath.of("References.Village[0]").getLong(chunk.getStructures()));
        assertEquals(new IntPointXZ(1 + 5 * 32, 12 + 5 * 32), xz);


        newMca = Paths.get(outRoot.getPath(), "poi", "r.4.5.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_15_2"));
        McaFileChunkIterator<PoiChunk> piter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        PoiChunk poiChunk = piter.next();
        int[] pos = NbtPath.of("Sections.4.Records[0].pos").getIntArray(poiChunk.getHandle());
        assertArrayEquals(new int[] {2519, 70, 2748}, pos);

        newMca = Paths.get(outRoot.getPath(), "region", "r.5.5.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_15_2"));
    }

    public void testRelocate_1_14_4() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_14_4").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(-1, 0, 0, 0));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(1, relocator.poiFilesRelocated());

        File newMca = Paths.get(outRoot.getPath(), "region", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_14_4"));
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(31, chunk.getChunkX());
        assertEquals(16, chunk.getChunkZ());

        List<Entity> entities = EntityFactory.fromListTag(chunk.getEntities(), chunk.getDataVersion());
        assertEquals(509, (int)entities.get(0).getX());
        assertEquals(40, (int)entities.get(0).getY());
        assertEquals(264, (int)entities.get(0).getZ());

        CompoundTag tileTag = NbtPath.of("Level.TileEntities[0]").getTag(chunk.getHandle());
        assertEquals(508, tileTag.getInt("x"));
        assertEquals(262, tileTag.getInt("z"));


        newMca = Paths.get(outRoot.getPath(), "poi", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_14_4"));
        McaFileChunkIterator<PoiChunk> piter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        PoiChunk poiChunk = piter.next();
        int[] pos = NbtPath.of("Sections.4.Records[0].pos").getIntArray(poiChunk.getHandle());
        assertArrayEquals(new int[] {501, 64, 263}, pos);
    }

    public void testRelocate_1_13_2() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_13_2").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(-2, -2, 0, 0));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(0, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_13_2"));
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(22, chunk.getChunkX());
        assertEquals(19, chunk.getChunkZ());

        List<Entity> entities = EntityFactory.fromListTag(chunk.getEntities(), chunk.getDataVersion());
        assertEquals(359, (int)entities.get(0).getX());
        assertEquals(63, (int)entities.get(0).getY());
        assertEquals(312, (int)entities.get(0).getZ());

        IntPointXZ xz = IntPointXZ.unpack(NbtPath.of("References.Village[0]").getLong(chunk.getStructures()));
        assertEquals(new IntPointXZ(21, 20), xz);
    }

    public void testRelocate_1_13_1() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_13_1").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(2, 2, 0, 0));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(0, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_13_1"));
        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(0, chunk.getChunkX());
        assertEquals(0, chunk.getChunkZ());
        chunk = iter.next();
        assertEquals(0, chunk.getChunkX());
        assertEquals(16, chunk.getChunkZ());
        chunk = iter.next();
        assertEquals(31, chunk.getChunkX());
        assertEquals(31, chunk.getChunkZ());
    }

    public void testRelocate_1_13_0() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_13_0").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(0, 0, 1, 1));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(0, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.1.1.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_13_0"));

        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(38, chunk.getChunkX());
        assertEquals(42, chunk.getChunkZ());
        IntPointXZ xz = IntPointXZ.unpack(NbtPath.of("References.Mineshaft[0]").getLong(chunk.getStructures()));
        assertEquals(new IntPointXZ(34, 38), xz);
    }

    public void testRelocate_1_12_2() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_12_2").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(0, 0, 1, 1));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(0, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.1.1.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_12_2"));

        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(42, chunk.getChunkX());
        assertEquals(43, chunk.getChunkZ());

        List<Entity> entities = EntityFactory.fromListTag(chunk.getEntities(), chunk.getDataVersion());
        assertEquals(680, (int)entities.get(0).getX());
        assertEquals(40, (int)entities.get(0).getY());
        assertEquals(703, (int)entities.get(0).getZ());
    }

    public void testRelocate_1_9_4() throws IOException {
        File outRoot = getNewTmpDirectory();
        RegionFileRelocator relocator = new RegionFileRelocator()
                .sourceRoot(getResourceFile("1_9_4").getPath())
                .destinationRoot(outRoot.getPath())
                .removeMoveChunkFlags(MoveChunkFlags.DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION);
        assertTrue(relocator.relocate(2, -1, 0, 0));
        assertEquals(1, relocator.regionFilesRelocated());
        assertEquals(0, relocator.entitiesFilesRelocated());
        assertEquals(0, relocator.poiFilesRelocated());
        File newMca = Paths.get(outRoot.getPath(), "region", "r.0.0.mca").toFile();
        assertTrue(newMca.exists());
        assertTrue(Files.size(newMca.toPath()) > 0x2000);
//        McaDumper.dumpChunksAsTextNbt(newMca, Paths.get("TESTDBG", "relocation", "1_9_4"));

        McaFileChunkIterator<TerrainChunk> iter = McaFileChunkIterator.iterate(newMca, LoadFlags.LOAD_ALL_DATA);
        TerrainChunk chunk = iter.next();
        assertEquals(24, chunk.getChunkX());
        assertEquals(12, chunk.getChunkZ());

        List<Entity> entities = EntityFactory.fromListTag(chunk.getEntities(), chunk.getDataVersion());
        assertEquals(387, (int)entities.get(0).getX());
        assertEquals(71, (int)entities.get(0).getY());
        assertEquals(193, (int)entities.get(0).getZ());

        // TODO: get a chunk that has a structure for 1.9.4
    }
}

//        McaDumper.dumpChunksAsTextNbt(
//                Paths.get(getResourceFile("1_9_4").getPath(), "region", "r.2.-1.mca").toFile(),
//                Paths.get("TESTDBG", "original", "1_9_4"));
