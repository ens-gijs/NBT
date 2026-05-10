package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.mca.util.ChunkIterator;
import io.github.ensgijs.nbt.mca.util.SectionIterator;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.ListTag;

import java.io.*;
import java.util.Objects;

public class McaRegionFileTest extends McaTestCase {

	public void testGetChunkIndex() {
		assertEquals(0, McaRegionFile.getChunkIndex(0, 0));
		assertEquals(0, McaRegionFile.getChunkIndex(32, 32));
		assertEquals(0, McaRegionFile.getChunkIndex(-32, -32));
		assertEquals(0, McaRegionFile.getChunkIndex(0, 32));
		assertEquals(0, McaRegionFile.getChunkIndex(-32, 32));
		assertEquals(1023, McaRegionFile.getChunkIndex(31, 31));
		assertEquals(1023, McaRegionFile.getChunkIndex(-1, -1));
		assertEquals(1023, McaRegionFile.getChunkIndex(63, 63));
		assertEquals(632, McaRegionFile.getChunkIndex(24, -13));
		int i = 0;
		for (int cz = 0; cz < 32; cz++) {
			for (int cx = 0; cx < 32; cx++) {
				assertEquals(i++, McaRegionFile.getChunkIndex(cx, cz));
			}
		}
	}

	public void testChangeData() {
		McaRegionFile mcaFile = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		assertNotNull(mcaFile);
		mcaFile.setChunk(0, null);
		File tmpFile = getNewTmpFile("1_13_1/region/r.2.2.mca");
		Integer x = assertThrowsNoException(() -> McaFileHelpers.write(mcaFile, tmpFile, true));
		assertNotNull(x);
		assertEquals(2, x.intValue());
		McaRegionFile again = assertThrowsNoException(() -> McaFileHelpers.read(tmpFile));
		assertNotNull(again);
		for (int i = 0; i < 1024; i++) {
			if (i != 512 && i != 1023) {
				assertNull(again.getChunk(i));
			} else {
				assertNotNull(again.getChunk(i));
			}
		}
	}

	public void testChangeLastUpdate() {
		McaRegionFile from = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		assertNotNull(from);
		File tmpFile = getNewTmpFile("1_13_1/region/r.2.2.mca");
		assertThrowsNoException(() -> McaFileHelpers.write(from, tmpFile, true));
		McaRegionFile to = assertThrowsNoException(() -> McaFileHelpers.read(tmpFile));
		assertNotNull(to);
		assertFalse(from.getChunk(0).getLastMCAUpdate() == to.getChunk(0).getLastMCAUpdate());
		assertFalse(from.getChunk(512).getLastMCAUpdate() == to.getChunk(512).getLastMCAUpdate());
		assertFalse(from.getChunk(1023).getLastMCAUpdate() == to.getChunk(1023).getLastMCAUpdate());
		assertTrue(to.getChunk(0).getLastMCAUpdate() == to.getChunk(512).getLastMCAUpdate());
		assertTrue(to.getChunk(0).getLastMCAUpdate() == to.getChunk(1023).getLastMCAUpdate());
	}

	public void testGetters() {
		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		assertNotNull(f);

		assertThrowsRuntimeException(() -> f.getChunk(-1), IndexOutOfBoundsException.class);
		assertThrowsRuntimeException(() -> f.getChunk(1024), IndexOutOfBoundsException.class);
		assertNotNull(assertThrowsNoRuntimeException(() -> f.getChunk(0)));
		assertNotNull(assertThrowsNoRuntimeException(() -> f.getChunk(1023)));
		assertNotNull(assertThrowsNoRuntimeException(() -> f.getChunk(96, 64)));
		assertNotNull(assertThrowsNoRuntimeException(() -> f.getChunk(95, 95)));
		assertNull(assertThrowsNoRuntimeException(() -> f.getChunk(63, 64)));
		assertNull(assertThrowsNoRuntimeException(() -> f.getChunk(95, 64)));
		assertNotNull(assertThrowsNoRuntimeException(() -> f.getChunk(64, 96)));
		assertNull(assertThrowsNoRuntimeException(() -> f.getChunk(64, 63)));
		assertNull(assertThrowsNoRuntimeException(() -> f.getChunk(64, 95)));
		//not loaded

		McaRegionFile u = new McaRegionFile(2, 2);
		assertThrowsRuntimeException(() -> u.getChunk(-1), IndexOutOfBoundsException.class);
		assertThrowsRuntimeException(() -> u.getChunk(1024), IndexOutOfBoundsException.class);
		assertNull(assertThrowsNoRuntimeException(() -> u.getChunk(0)));
		assertNull(assertThrowsNoRuntimeException(() -> u.getChunk(1023)));

		assertEquals(1628, f.getChunk(0).getDataVersion());
		assertEquals(1538048269, f.getChunk(0).getLastMCAUpdate());
		assertEquals(1205486986, f.getChunk(0).getLastUpdateTick());
		assertNotNull(f.getChunk(0).getLegacyBiomes());
		assertNull(f.getChunk(0).getHeightMaps());
		assertNull(f.getChunk(0).getCarvingMasks());
		assertEquals(ListTag.createUnchecked(null), f.getChunk(0).getEntities());
		assertNull(f.getChunk(0).getTileEntities());
		assertNull(f.getChunk(0).getTileTicks());
		assertNull(f.getChunk(0).getLiquidTicks());
		assertNull(f.getChunk(0).getLights());
		assertNull(f.getChunk(0).getLiquidsToBeTicked());
		assertNull(f.getChunk(0).getToBeTicked());
		assertNull(f.getChunk(0).getPostProcessing());
		assertNotNull(f.getChunk(0).getStructures());

		assertNotNull(f.getChunk(0).getSection(0).getSkyLight());
		assertEquals(2048, f.getChunk(0).getSection(0).getSkyLight().length);
		assertNotNull(f.getChunk(0).getSection(0).getBlockLight());
		assertEquals(2048, f.getChunk(0).getSection(0).getBlockLight().length);
//		assertNotNull(f.getChunk(0).getSection(0).getBlockStates());
//		assertEquals(256, f.getChunk(0).getSection(0).getBlockStates().length);
	}

	private TerrainChunk createChunkWithPos() {
		CompoundTag data = new CompoundTag();
		CompoundTag level = new CompoundTag();
		data.put("Level", level);
		data.putInt("DataVersion", DataVersion.JAVA_1_16_5.id());
		return new TerrainChunk(data);
	}

	public void testSetters() {
		McaRegionFile f = new McaRegionFile(2, 2);

		assertThrowsNoRuntimeException(() -> f.setChunk(0, createChunkWithPos()));
		assertEquals(createChunkWithPos().updateHandle(64, 64), f.getChunk(0).updateHandle(64, 64));
		assertThrowsRuntimeException(() -> f.setChunk(1024, createChunkWithPos()), IndexOutOfBoundsException.class);
		assertThrowsNoRuntimeException(() -> f.setChunk(1023, createChunkWithPos()));
		assertThrowsNoRuntimeException(() -> f.setChunk(0, null));
		assertNull(f.getChunk(0));
		assertThrowsNoRuntimeException(() -> f.setChunk(1023, createChunkWithPos()));
		assertThrowsNoRuntimeException(() -> f.setChunk(1023, createChunkWithPos()));

		f.getChunk(1023).setDataVersion(1627);
		assertEquals(1627, f.getChunk(1023).getDataVersion());
		f.getChunk(1023).setLastMCAUpdate(12345678);
		assertEquals(12345678, f.getChunk(1023).getLastMCAUpdate());
		f.getChunk(1023).setLastUpdateTick(87654321);
		assertEquals(87654321, f.getChunk(1023).getLastUpdateTick());
		f.getChunk(1023).setInhabitedTimeTicks(13243546);
		assertEquals(13243546, f.getChunk(1023).getInhabitedTimeTicks());
		assertThrowsRuntimeException(() -> f.getChunk(1023).setLegacyBiomes(new int[255]), IllegalArgumentException.class);
		int[] biomes = new int[256];
		assertThrowsNoRuntimeException(() -> f.getChunk(1023).setLegacyBiomes(biomes));
		assertSame(biomes, f.getChunk(1023).getLegacyBiomes());
		CompoundTag compoundTag = getSomeCompoundTag();
		f.getChunk(1023).setHeightMaps(compoundTag);
		assertSame(compoundTag, f.getChunk(1023).getHeightMaps());
		compoundTag = getSomeCompoundTag();
		f.getChunk(1023).setCarvingMasks(compoundTag);
		assertSame(compoundTag, f.getChunk(1023).getCarvingMasks());
		ListTag<CompoundTag> compoundTagListTag = getSomeCompoundTagList();
		f.getChunk(1023).setEntities(compoundTagListTag);
		assertSame(compoundTagListTag, f.getChunk(1023).getEntities());
		compoundTagListTag = getSomeCompoundTagList();
		f.getChunk(1023).setTileEntities(compoundTagListTag);
		assertSame(compoundTagListTag, f.getChunk(1023).getTileEntities());
		compoundTagListTag = getSomeCompoundTagList();
		f.getChunk(1023).setTileTicks(compoundTagListTag);
		assertSame(compoundTagListTag, f.getChunk(1023).getTileTicks());
		compoundTagListTag = getSomeCompoundTagList();
		f.getChunk(1023).setLiquidTicks(compoundTagListTag);
		assertSame(compoundTagListTag, f.getChunk(1023).getLiquidTicks());
		ListTag<ListTag<?>> listTagListTag = getSomeListTagList();
		f.getChunk(1023).setLights(listTagListTag);
		assertSame(listTagListTag, f.getChunk(1023).getLights());
		listTagListTag = getSomeListTagList();
		f.getChunk(1023).setLiquidsToBeTicked(listTagListTag);
		assertSame(listTagListTag, f.getChunk(1023).getLiquidsToBeTicked());
		listTagListTag = getSomeListTagList();
		f.getChunk(1023).setToBeTicked(listTagListTag);
		assertSame(listTagListTag, f.getChunk(1023).getToBeTicked());
		listTagListTag = getSomeListTagList();
		f.getChunk(1023).setPostProcessing(listTagListTag);
		assertSame(listTagListTag, f.getChunk(1023).getPostProcessing());
		compoundTag = getSomeCompoundTag();
		f.getChunk(1023).setStructures(compoundTag);
		assertSame(compoundTag, f.getChunk(1023).getStructures());
		TerrainSection s = f.getChunk(1023).createSection();
		f.getChunk(1023).setSection(0, s);
		assertEquals(0, s.getSectionY());
		assertEquals(s, f.getChunk(1023).getSection(0));
//		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockStates(null), NullPointerException.class);
//		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockStates(new long[321]), IllegalArgumentException.class);
//		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockStates(new long[255]), IllegalArgumentException.class);
//		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockStates(new long[4097]), IllegalArgumentException.class);
//		assertThrowsNoRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockStates(new long[320]));
//		assertThrowsNoRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockStates(new long[4096]));
//		assertThrowsNoRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockStates(new long[256]));
		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockLight(new byte[2047]), IllegalArgumentException.class);
		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockLight(new byte[2049]), IllegalArgumentException.class);
		assertThrowsNoRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockLight(new byte[2048]));
		assertThrowsNoRuntimeException(() -> f.getChunk(1023).getSection(0).setBlockLight(null));
		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setSkyLight(new byte[2047]), IllegalArgumentException.class);
		assertThrowsRuntimeException(() -> f.getChunk(1023).getSection(0).setSkyLight(new byte[2049]), IllegalArgumentException.class);
		assertThrowsNoRuntimeException(() -> f.getChunk(1023).getSection(0).setSkyLight(new byte[2048]));
		assertThrowsNoRuntimeException(() -> f.getChunk(1023).getSection(0).setSkyLight(null));
	}

	public void testGetBiomeAt2D() {
		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		assertEquals(21, f.getBiomeAt(1024, 1024));
		assertEquals(-1, f.getBiomeAt(1040, 1024));
		f.setChunk(0, 1, TerrainChunk.newChunk(2201));
		assertEquals(-1, f.getBiomeAt(1024, 1040));
	}

	public void testSetBiomeAt_2D_2dBiomeWorld() {
		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		f.setBiomeAt(1024, 1024, 20);
		assertEquals(20, f.getChunk(64, 64).updateHandle(64, 64).getCompoundTag("Level").getIntArray("Biomes")[0]);
		f.setBiomeAt(1039, 1039, 47);
		assertEquals(47, f.getChunk(64, 64).updateHandle(64, 64).getCompoundTag("Level").getIntArray("Biomes")[255]);
		f.setBiomeAt(1040, 1024, 20);

		int[] biomes = f.getChunk(65, 64).updateHandle(65, 64).getCompoundTag("Level").getIntArray("Biomes");
		assertEquals(256, biomes.length);
		assertEquals(20, biomes[0]);
		for (int i = 1; i < 256; i++) {
			assertEquals(-1, biomes[i]);
		}
	}

	public void testSetBiomeAt_2D_3DBiomeWorld() {
		McaRegionFile f = new McaRegionFile(2, 2, DataVersion.JAVA_1_15_0);
		f.setBiomeAt(1040, 1024, 20);
		int[] biomes = f.getChunk(65, 64).updateHandle(65, 64).getCompoundTag("Level").getIntArray("Biomes");
		assertEquals(1024, biomes.length);
		for (int i = 0; i < 1024; i++) {
			assertTrue(i % 16 == 0 ? biomes[i] == 20 : biomes[i] == -1);
		}
	}

//	public void testCleanupPaletteAndBlockStates() {
//		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
//		assertThrowsNoRuntimeException(f::cleanupPalettesAndBlockStates);
//		TerrainChunk c = f.getChunk(0, 0);
//		TerrainSection s = c.getSection(0);
//		assertEquals(10, s.getBlockPalette().size());
//		for (int i = 11; i <= 15; i++) {
//			s.addToPalette(block("minecraft:" + i));
//		}
//		assertEquals(15, s.getBlockPalette().size());
//		f.cleanupPalettesAndBlockStates();
//		assertEquals(10, s.getBlockPalette().size());
//		assertEquals(256, s.updateHandle(0).getLongArray("BlockStates").length);
//		int y = 0;
//		for (int i = 11; i <= 17; i++) {
//			f.setBlockStateAt(1, y++, 1, block("minecraft:" + i), false);
//		}
//		assertEquals(17, s.getBlockPalette().size());
//		assertEquals(320, s.updateHandle(0).getLongArray("BlockStates").length);
//		f.cleanupPalettesAndBlockStates();
//		assertEquals(17, s.getBlockPalette().size());
//		assertEquals(320, s.updateHandle(0).getLongArray("BlockStates").length);
//		f.setBlockStateAt(1, 0, 1, block("minecraft:bedrock"), false);
//		assertEquals(17, s.getBlockPalette().size());
//		assertEquals(320, s.updateHandle(0).getLongArray("BlockStates").length);
//		f.cleanupPalettesAndBlockStates();
//		assertEquals(16, s.getBlockPalette().size());
//		assertEquals(256, s.updateHandle(0).getLongArray("BlockStates").length);
//	}

	public void testMaxAndMinSectionY() {
		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		TerrainChunk c = f.getChunk(0, 0);
		assertEquals(0, c.getMinSectionY());
		assertEquals(5, c.getMaxSectionY());
		c.setSection(-64 / 16, TerrainSection.newSection());
		c.setSection((320 - 16) / 16, TerrainSection.newSection());
		assertEquals(-4, c.getMinSectionY());
		assertEquals(19, c.getMaxSectionY());
	}

//	public void testSetBlockDataAt() {
//		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
//		assertEquals(f.getMaxChunkDataVersion(), f.getMinChunkDataVersion());
//		assertTrue(f.getDefaultChunkDataVersion() > 0);
//		TerrainSection section = f.getChunk(0, 0).getSection(0);
//		assertEquals(10, section.getBlockPalette().size());
//		assertEquals(0b0001000100010001000100010001000100010001000100010001000100010001L, section.getBlockStates()[0]);
//		f.setBlockStateAt(0, 0, 0, block("minecraft:custom"), false);
//		assertEquals(11, section.getBlockPalette().size());
//		assertEquals(0b0001000100010001000100010001000100010001000100010001000100011010L, section.getBlockStates()[0]);
//
//		//test "line break"
//		int y = 1;
//		for (int i = 12; i <= 17; i++) {
//			f.setBlockStateAt(0, y++, 0, block("minecraft:" + i), false);
//		}
//		assertEquals(17, section.getBlockPalette().size());
//		assertEquals(320, section.getBlockStates().length);
//		assertEquals(0b0001000010000100001000010000100001000010000100001000010000101010L, section.getBlockStates()[0]);
//		assertEquals(0b0010000100001000010000100001000010000100001000010000100001000010L, section.getBlockStates()[1]);
//		f.setBlockStateAt(12, 0, 0, block("minecraft:18"), false);
//		assertEquals(0b0001000010000100001000010000100001000010000100001000010000101010L, section.getBlockStates()[0]);
//		assertEquals(0b0010000100001000010000100001000010000100001000010000100001000011L, section.getBlockStates()[1]);
//
//		//test chunkdata == null
//		assertNull(f.getChunk(1, 0));
//		f.setBlockStateAt(17, 0, 0, block("minecraft:test"), false);
//		assertNotNull(f.getChunk(1, 0));
//		assertEquals(f.getDefaultChunkDataVersion(), f.getChunk(1, 0).getDataVersion());
//		ListTag<CompoundTag> s = f.getChunk(1, 0).updateHandle(65, 64).getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
//		assertEquals(1, s.size());
//		assertEquals(2, s.get(0).getListTag("Palette").size());
//		assertEquals(256, s.get(0).getLongArray("BlockStates").length);
//		assertEquals(0b0000000000000000000000000000000000000000000000000000000000010000L, s.get(0).getLongArray("BlockStates")[0]);
//
//		//test section == null
//		assertNull(f.getChunk(66, 64));
//		TerrainChunk c = f.createChunk();
//		f.setChunk(66, 64, c);
//		assertNotNull(f.getChunk(66, 64));
//		CompoundTag levelTag = f.getChunk(66, 64).updateHandle(66, 64).getCompoundTag("Level");
//		assertNotNull(levelTag);
//		ListTag<?> sectionsTag = levelTag.getListTag("Sections");
//		assertNotNull(sectionsTag);
//		ListTag<CompoundTag> ss = sectionsTag.asCompoundTagList();
//		assertEquals(0, ss.size());
//		f.setBlockStateAt(33, 0, 0, block("minecraft:air"), false);
//		ss = f.getChunk(66, 64).updateHandle(66, 64).getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
//		assertEquals(1, ss.size());
//		f.setBlockStateAt(33, 0, 0, block("minecraft:foo"), false);
//		ss = f.getChunk(66, 64).updateHandle(66, 64).getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
//		assertEquals(1, ss.size());
//		assertEquals(2, ss.get(0).getListTag("Palette").size());
//		assertEquals(256, s.get(0).getLongArray("BlockStates").length);
//		assertEquals(0b0000000000000000000000000000000000000000000000000000000000010000L, ss.get(0).getLongArray("BlockStates")[0]);
//
//		//test force cleanup
//		ListTag<CompoundTag> sss = f.getChunk(31, 31).updateHandle(65, 65).getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
//		assertEquals(12, sss.get(0).getListTag("Palette").size());
//		y = 0;
//		for (int i = 13; i <= 17; i++) {
//			f.setBlockStateAt(1008, y++, 1008, block("minecraft:" + i), false);
//		}
//		f.getChunk(31, 31).getSection(0).cleanupPaletteAndBlockStates();
//		sss = f.getChunk(31, 31).updateHandle(65, 65).getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
//		assertEquals(17, sss.get(0).getListTag("Palette").size());
//		assertEquals(320, sss.get(0).getLongArray("BlockStates").length);
//		f.setBlockStateAt(1008, 4, 1008, block("minecraft:16"), true);
//		sss = f.getChunk(31, 31).updateHandle(65, 65).getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
//		assertEquals(16, sss.get(0).getListTag("Palette").size());
//		assertEquals(256, sss.get(0).getLongArray("BlockStates").length);
//	}

//	public void testSetBlockDataAt2527() {
//		//test "line break" for DataVersion 2527
//		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
//		TerrainChunk p = f.getChunk(0, 0);
//		p.setDataVersion(999999);
//		TerrainSection section = f.getChunk(0, 0).getSection(0);
//		assertEquals(10, section.getBlockPalette().size());
//		assertEquals(0b0001000100010001000100010001000100010001000100010001000100010001L, section.getBlockStates()[0]);
//		f.setBlockStateAt(0, 0, 0, block("minecraft:custom"), false);
//		assertEquals(11, section.getBlockPalette().size());
//		assertEquals(0b0001000100010001000100010001000100010001000100010001000100011010L, section.getBlockStates()[0]);
//		int y = 1;
//		for (int i = 12; i <= 17; i++) {
//			f.setBlockStateAt(0, y++, 0, block("minecraft:" + i), false);
//		}
//		assertEquals(17, section.getBlockPalette().size());
//		assertEquals(342, section.getBlockStates().length);
//	}
//
//	public void testGetBlockDataAt() {
//		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
//		assertEquals(block("minecraft:bedrock"), f.getBlockStateAt(0, 0, 0));
//		assertNull(f.getBlockStateAt(16, 0, 0));
//		assertEquals(block("minecraft:dirt"), f.getBlockStateAt(0, 62, 0));
//		assertEquals(block("minecraft:dirt"), f.getBlockStateAt(15, 67, 15));
//		assertNull(f.getBlockStateAt(3, 100, 3));
//	}

	public void testGetChunkStatus() {
		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		assertEquals("mobs_spawned", f.getChunk(0, 0).getStatus());
	}

	public void testSetChunkStatus() {
		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		assertThrowsNoRuntimeException(() -> f.getChunk(0, 0).setStatus("base"));
		assertEquals("base", f.getChunk(0, 0).updateHandle(64, 64).getCompoundTag("Level").getString("Status"));
		assertNull(f.getChunk(1, 0));
	}

	public void testChunkInitReferences() {
		CompoundTag t = new CompoundTag();
		assertThrowsRuntimeException(() -> new TerrainChunk(null), NullPointerException.class);
		assertThrowsRuntimeException(() -> new TerrainChunk(t), IllegalArgumentException.class);
	}

	public void testChunkInvalidCompressionType() {
		assertThrowsException(() -> {
			try (RandomAccessFile raf = new RandomAccessFile(getResourceFile("invalid_compression.dat"), "r")) {
				TerrainChunk c = new TerrainChunk(0);
				c.deserialize(raf, LoadFlags.LOAD_ALL_DATA, 0, 0, 0);
			}
		}, IOException.class);
	}

	public void testChunkInvalidDataTag() {
		assertThrowsException(() -> {
			try (RandomAccessFile raf = new RandomAccessFile(getResourceFile("invalid_data_tag.dat"), "r")) {
				TerrainChunk c = new TerrainChunk(0);
				c.deserialize(raf, LoadFlags.LOAD_ALL_DATA, 0, 0, 0);
			}
		}, IOException.class);
	}

	private void assertLoadFlag(Object field, long flags, long wantedFlag) {
		if((flags & wantedFlag) != 0) {
			assertNotNull(String.format("Should not be null. Flags=%08x, Wanted flag=%08x", flags, wantedFlag), field);
		} else {
			assertNull(String.format("Should be null. Flags=%08x, Wanted flag=%08x", flags, wantedFlag), field);
		}
	}

	private void assertPartialChunk(TerrainChunk c, long loadFlags) {
		assertLoadFlag(c.getLegacyBiomes(), loadFlags, LoadFlags.BIOMES);
		assertLoadFlag(c.getHeightMaps(), loadFlags, LoadFlags.HEIGHTMAPS);
		assertLoadFlag(c.getEntities(), loadFlags, LoadFlags.ENTITIES);
		assertLoadFlag(c.getCarvingMasks(), loadFlags, LoadFlags.CARVING_MASKS);
		assertLoadFlag(c.getLights(), loadFlags, LoadFlags.LIGHTS);
		assertLoadFlag(c.getPostProcessing(), loadFlags, LoadFlags.POST_PROCESSING);
		assertLoadFlag(c.getLiquidTicks(), loadFlags, LoadFlags.LIQUID_TICKS);
		assertLoadFlag(c.getLiquidsToBeTicked(), loadFlags, LoadFlags.LIQUIDS_TO_BE_TICKED);
		assertLoadFlag(c.getTileTicks(), loadFlags, LoadFlags.TILE_TICKS);
		assertLoadFlag(c.getTileEntities(), loadFlags, LoadFlags.TILE_ENTITIES);
		assertLoadFlag(c.getToBeTicked(), loadFlags, LoadFlags.TO_BE_TICKED);
		assertLoadFlag(c.getSection(0), loadFlags, LoadFlags.BLOCK_LIGHTS| LoadFlags.BLOCK_STATES| LoadFlags.SKY_LIGHT);
		if ((loadFlags & (LoadFlags.BLOCK_LIGHTS| LoadFlags.BLOCK_STATES| LoadFlags.SKY_LIGHT)) != 0) {
			TerrainSection s = c.getSection(0);
			assertNotNull(String.format("Section is null. Flags=%08x", loadFlags), s);
//			assertLoadFlag(s.getBlockStates(), loadFlags, BLOCK_STATES);
			assertLoadFlag(s.getBlockLight(), loadFlags, LoadFlags.BLOCK_LIGHTS);
			assertLoadFlag(s.getSkyLight(), loadFlags, LoadFlags.SKY_LIGHT);
		}
	}

	public void testReleaseChunkDataTagFlag_preventsUpdatingHandle_whilePartialLoadingAloneDoseNot() {
		long[] flags = new long[] {
				LoadFlags.BIOMES,
				LoadFlags.HEIGHTMAPS,
				LoadFlags.ENTITIES,
				LoadFlags.CARVING_MASKS,
				LoadFlags.LIGHTS,
				LoadFlags.POST_PROCESSING,
				LoadFlags.LIQUID_TICKS,
				LoadFlags.LIQUIDS_TO_BE_TICKED,
				LoadFlags.TILE_TICKS,
				LoadFlags.TILE_ENTITIES,
				LoadFlags.TO_BE_TICKED,
				LoadFlags.BLOCK_STATES,
				LoadFlags.BLOCK_LIGHTS,
				LoadFlags.SKY_LIGHT,
				LoadFlags.LOAD_ALL_DATA
		};

		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_13_1/region/r.2.2.mca")));
		TerrainChunk c = f.getChunk(0);
		c.setCarvingMasks(getSomeCompoundTag());
		c.setEntities(getSomeCompoundTagList());
		c.setLights(getSomeListTagList());
		c.setTileEntities(getSomeCompoundTagList());
		c.setTileTicks(getSomeCompoundTagList());
		c.setLiquidTicks(getSomeCompoundTagList());
		c.setToBeTicked(getSomeListTagList());
		c.setLiquidsToBeTicked(getSomeListTagList());
		c.setHeightMaps(getSomeCompoundTag());
		c.setPostProcessing(getSomeListTagList());
		c.getSection(0).setBlockLight(new byte[2048]);
		File tmp = this.getNewTmpFile("1_13_1/region/r.2.2.mca");
		assertThrowsNoException(() -> McaFileHelpers.write(f, tmp));

		for (long flag : flags) {
			McaRegionFile mcaFile = assertThrowsNoException(() -> McaFileHelpers.read(tmp, flag | LoadFlags.RELEASE_CHUNK_DATA_TAG));
			c = mcaFile.getChunk(0, 0);
			assertPartialChunk(c, flag);
			assertThrowsException(() -> McaFileHelpers.write(mcaFile, getNewTmpFile("r.12.34.mca")), UnsupportedOperationException.class);
		}

		for (long flag : flags) {
			McaRegionFile mcaFile = assertThrowsNoException(() -> McaFileHelpers.read(tmp, flag));
			c = mcaFile.getChunk(0, 0);
			assertPartialChunk(c, flag);
			assertThrowsNoException(() -> McaFileHelpers.write(mcaFile, getNewTmpFile("r.12.34.mca")));
		}
	}

	public void test1_15GetBiomeAt() {
		McaRegionFile f = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_15_2/region/r.0.0.mca")));
		assertEquals(162, f.getBiomeAt(31, 0, 63));
		assertEquals(4, f.getBiomeAt(16, 0, 48));
		assertEquals(4, f.getBiomeAt(16, 0, 63));
		assertEquals(162, f.getBiomeAt(31, 0, 48));
		assertEquals(162, f.getBiomeAt(31, 100, 63));
		assertEquals(4, f.getBiomeAt(16, 100, 48));
		assertEquals(4, f.getBiomeAt(16, 100, 63));
		assertEquals(162, f.getBiomeAt(31, 100, 48));
		assertEquals(162, f.getBiomeAt(31, 106, 63));
		assertEquals(4, f.getBiomeAt(16, 106, 48));
		assertEquals(4, f.getBiomeAt(16, 106, 63));
		assertEquals(162, f.getBiomeAt(31, 106, 48));
	}

	public void testChunkSectionPutSection() {
		McaRegionFile mca = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_17_1/region/r.-3.-2.mca")));
		TerrainChunk chunk = mca.stream().filter(Objects::nonNull).findFirst().orElse(null);
		assertNotNull(chunk);
		final TerrainSection section2 = chunk.getSection(2);
		assertNull(chunk.putSection(2, section2));  // no error to replace self
		assertThrowsException(() -> chunk.putSection(3, section2), IllegalArgumentException.class);  // should fail
		assertNotSame(section2, chunk.getSection(3));  // shouldn't have updated section 3
		final TerrainSection newSection = chunk.createSection();
		final TerrainSection prevSection2 = chunk.putSection(2, newSection);  // replace existing section 2 with the new one
		assertNotNull(prevSection2);
		assertSame(section2, prevSection2);  // check we got the existing section 2 when we replaced it
		assertSame(newSection, chunk.getSection(2));  // verify we put section 2
		assertEquals(2, newSection.getSectionY());  // insertion should update section height
		final TerrainSection section3 = chunk.putSection(3, section2);  // should be OK to put old section 2 into section 3 place now

		final TerrainSection section1 = chunk.getSection(1);
		final TerrainSection prevSection5 = chunk.putSection(5, section1, true);  // move section 1 into section 5
		assertNotNull(prevSection5);
		assertNull(chunk.getSection(1));  // verify we 'moved' section one out
		assertNotSame(section1, prevSection5);  // make sure the return value isn't stupid
		assertNull(chunk.putSection(1, prevSection5, true));  // moving 5 into empty slot is OK

		// guard against section y default(0) case
		final TerrainSection section0 = chunk.getSection(0);
		final TerrainSection newSection0 = chunk.createSection();
		assertSame(section0, chunk.putSection(0, newSection0));

		// and finally direct removal via putting null
		assertSame(newSection0, chunk.putSection(0, null));
		assertNull(chunk.getSection(0));
		assertNull(chunk.putSection(0, null));
		chunk.putSection(0, section0);
		assertSame(section0, chunk.putSection(0, null, true));
		assertNull(chunk.getSection(0));

		assertThrowsException(() -> chunk.putSection(Byte.MIN_VALUE - 1, chunk.createSection()), IllegalArgumentException.class);
		assertThrowsException(() -> chunk.putSection(Byte.MAX_VALUE + 1, chunk.createSection()), IllegalArgumentException.class);

		assertThrowsNoException(() -> chunk.putSection(Byte.MIN_VALUE, chunk.createSection()));
		assertThrowsNoException(() -> chunk.putSection(Byte.MAX_VALUE, chunk.createSection()));
	}

	public void testChunkSectionGetSectionY() {
		McaRegionFile mca = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_17_1/region/r.-3.-2.mca")));
		TerrainChunk chunk = mca.stream().filter(Objects::nonNull).findFirst().orElse(null);
		assertNotNull(chunk);
		assertEquals(SectionBase.NO_SECTION_Y_SENTINEL, chunk.getSectionY(null));
		assertEquals(SectionBase.NO_SECTION_Y_SENTINEL, chunk.getSectionY(chunk.createSection()));
		TerrainSection section = chunk.getSection(5);
		section.setHeight(-5);
		assertEquals(5, chunk.getSectionY(section));
		assertEquals(5, section.getSectionY());  // getSectionY should sync Y
	}

	public void testChunkSectionMinMaxSectionY() {
		TerrainChunk chunk = new TerrainChunk(42);
		chunk.setDataVersion(DataVersion.JAVA_1_17_1.id());
		assertEquals(SectionBase.NO_SECTION_Y_SENTINEL, chunk.getMinSectionY());
		assertEquals(SectionBase.NO_SECTION_Y_SENTINEL, chunk.getMaxSectionY());
		TerrainSection section = chunk.createSection(3);
		assertEquals(3, section.getSectionY());
		assertEquals(3, chunk.getMinSectionY());
		assertEquals(3, chunk.getMaxSectionY());
	}

	public void testMCAFileChunkIterator() {
		McaRegionFile mca = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_17_1/region/r.-3.-2.mca")));
		ChunkIterator<TerrainChunk> iter = mca.iterator();
		assertEquals(-1, iter.currentIndex());
		final int populatedX = -65 & 0x1F;
		final int populatedZ = -42 & 0x1F;
		int i = 0;
		for (int z = 0, wz = -2 * 32; z < 32; z++, wz++) {
			for (int x = 0, wx = -3 * 32; x < 32; x++, wx++) {
				assertTrue(iter.hasNext());
				TerrainChunk chunk = iter.next();
				assertEquals(i, iter.currentIndex());
				assertEquals(x, iter.currentX());
				assertEquals(z, iter.currentZ());
				assertEquals(wx, iter.currentAbsoluteX());
				assertEquals(wz, iter.currentAbsoluteZ());
				if (x == populatedX && z == populatedZ) {
					assertNotNull(chunk);
				} else {
					assertNull(chunk);
				}
				if (i == 1023) {
					iter.set(mca.createChunk());
				}
				i++;
			}
		}
		assertFalse(iter.hasNext());
		assertNotNull(mca.getChunk(1023));
	}

	public void testChunkSectionIterator() {
		McaRegionFile mca = assertThrowsNoException(() -> McaFileHelpers.read(copyResourceToTmp("1_17_1/region/r.-3.-2.mca")));
		assertEquals(1, mca.count());
		TerrainChunk chunk = mca.stream().filter(Objects::nonNull).findFirst().orElse(null);
		assertNotNull(chunk);
		final int minY = chunk.getMinSectionY();
		final int maxY = chunk.getMaxSectionY();
		assertNotNull(chunk.getSection(minY));
		assertNotNull(chunk.getSection(maxY));
		SectionIterator<TerrainSection> iter = chunk.iterator();
		for (int y = minY; y <= maxY; y++) {
			assertTrue(iter.hasNext());
			TerrainSection section = iter.next();
			assertNotNull(section);
			assertEquals(y, iter.sectionY());
			assertEquals(y, section.getSectionY());
			if (y > maxY - 2) {
				iter.remove();
			}
		}
		assertFalse(iter.hasNext());
		assertEquals(minY, chunk.getMinSectionY());
		assertEquals(maxY - 2, chunk.getMaxSectionY());
		assertNull(chunk.getSection(maxY));
		assertNull(chunk.getSection(maxY - 1));
		assertNotNull(chunk.getSection(maxY - 2));
	}
}
