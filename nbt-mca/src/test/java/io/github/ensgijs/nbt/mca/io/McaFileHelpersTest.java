package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.mca.*;

import java.io.File;
import java.nio.file.Paths;

public class McaFileHelpersTest extends McaTestCase {

	public void testLocationConversion() {
		assertEquals(0, McaFileHelpers.blockToChunk(0));
		assertEquals(0, McaFileHelpers.blockToChunk(15));
		assertEquals(1, McaFileHelpers.blockToChunk(16));
		assertEquals(-1, McaFileHelpers.blockToChunk(-1));
		assertEquals(-1, McaFileHelpers.blockToChunk(-16));
		assertEquals(-2, McaFileHelpers.blockToChunk(-17));

		assertEquals(0, McaFileHelpers.blockToRegion(0));
		assertEquals(0, McaFileHelpers.blockToRegion(511));
		assertEquals(1, McaFileHelpers.blockToRegion(512));
		assertEquals(-1, McaFileHelpers.blockToRegion(-1));
		assertEquals(-1, McaFileHelpers.blockToRegion(-512));
		assertEquals(-2, McaFileHelpers.blockToRegion(-513));

		assertEquals(0, McaFileHelpers.chunkToRegion(0));
		assertEquals(0, McaFileHelpers.chunkToRegion(31));
		assertEquals(1, McaFileHelpers.chunkToRegion(32));
		assertEquals(-1, McaFileHelpers.chunkToRegion(-1));
		assertEquals(-1, McaFileHelpers.chunkToRegion(-32));
		assertEquals(-2, McaFileHelpers.chunkToRegion(-33));

		assertEquals(0, McaFileHelpers.regionToChunk(0));
		assertEquals(32, McaFileHelpers.regionToChunk(1));
		assertEquals(-32, McaFileHelpers.regionToChunk(-1));
		assertEquals(-64, McaFileHelpers.regionToChunk(-2));

		assertEquals(0, McaFileHelpers.regionToBlock(0));
		assertEquals(512, McaFileHelpers.regionToBlock(1));
		assertEquals(-512, McaFileHelpers.regionToBlock(-1));
		assertEquals(-1024, McaFileHelpers.regionToBlock(-2));

		assertEquals(0, McaFileHelpers.chunkToBlock(0));
		assertEquals(16, McaFileHelpers.chunkToBlock(1));
		assertEquals(-16, McaFileHelpers.chunkToBlock(-1));
		assertEquals(-32, McaFileHelpers.chunkToBlock(-2));
	}

	public void testBlockAbsoluteToChunkRelative_int() {
		assertEquals(0, McaFileHelpers.blockAbsoluteToChunkRelative(0));
		assertEquals(0, McaFileHelpers.blockAbsoluteToChunkRelative(16 * 81));
		assertEquals(15, McaFileHelpers.blockAbsoluteToChunkRelative(16 * 81 - 1));
		assertEquals(0, McaFileHelpers.blockAbsoluteToChunkRelative(-16 * 81));
		assertEquals(15, McaFileHelpers.blockAbsoluteToChunkRelative(-16 * 81 - 1));
		assertEquals(1, McaFileHelpers.blockAbsoluteToChunkRelative(-16 * 81 + 1));
	}

	public void testBlockAbsoluteToChunkRelative_double() {
		assertEquals(0.0, McaFileHelpers.blockAbsoluteToChunkRelative(0.0), 1e-10);
		assertEquals(16 - 1e-6, McaFileHelpers.blockAbsoluteToChunkRelative(-1e-6), 1e-10);
		assertEquals(3.4567, McaFileHelpers.blockAbsoluteToChunkRelative(16 * 81 + 3.4567), 1e-10);
		assertEquals(16 - 3.4567, McaFileHelpers.blockAbsoluteToChunkRelative(16 * 81 - 3.4567), 1e-10);
		assertEquals(16 - 3.4567, McaFileHelpers.blockAbsoluteToChunkRelative(-16 * 81 - 3.4567), 1e-10);
		assertEquals(3.4567, McaFileHelpers.blockAbsoluteToChunkRelative(-16 * 81 + 3.4567), 1e-10);
	}

	public void testCreateNameFromLocation() {
		assertEquals("r.0.0.mca", McaFileHelpers.createNameFromBlockLocation(0, 0));
		assertEquals("r.0.0.mca", McaFileHelpers.createNameFromBlockLocation(511, 511));
		assertEquals("r.1.0.mca", McaFileHelpers.createNameFromBlockLocation(512, 511));
		assertEquals("r.0.-1.mca", McaFileHelpers.createNameFromBlockLocation(511, -1));
		assertEquals("r.0.-1.mca", McaFileHelpers.createNameFromBlockLocation(511, -512));
		assertEquals("r.0.-2.mca", McaFileHelpers.createNameFromBlockLocation(511, -513));
		assertEquals("r.0.1.mca", McaFileHelpers.createNameFromBlockLocation(511, 512));
		assertEquals("r.-1.0.mca", McaFileHelpers.createNameFromBlockLocation(-1, 511));
		assertEquals("r.-1.0.mca", McaFileHelpers.createNameFromBlockLocation(-512, 511));
		assertEquals("r.-2.0.mca", McaFileHelpers.createNameFromBlockLocation(-513, 511));
		
		assertEquals("r.0.0.mca", McaFileHelpers.createNameFromChunkLocation(0, 0));
		assertEquals("r.0.0.mca", McaFileHelpers.createNameFromChunkLocation(31, 31));
		assertEquals("r.1.0.mca", McaFileHelpers.createNameFromChunkLocation(32, 31));
		assertEquals("r.0.-1.mca", McaFileHelpers.createNameFromChunkLocation(31, -1));
		assertEquals("r.0.-1.mca", McaFileHelpers.createNameFromChunkLocation(31, -32));
		assertEquals("r.0.-2.mca", McaFileHelpers.createNameFromChunkLocation(31, -33));
		assertEquals("r.0.1.mca", McaFileHelpers.createNameFromChunkLocation(31, 32));
		assertEquals("r.-1.0.mca", McaFileHelpers.createNameFromChunkLocation(-1, 31));
		assertEquals("r.-1.0.mca", McaFileHelpers.createNameFromChunkLocation(-32, 31));
		assertEquals("r.-2.0.mca", McaFileHelpers.createNameFromChunkLocation(-33, 31));
		
		assertEquals("r.0.0.mca", McaFileHelpers.createNameFromRegionLocation(0, 0));
		assertEquals("r.1.0.mca", McaFileHelpers.createNameFromRegionLocation(1, 0));
		assertEquals("r.0.-1.mca", McaFileHelpers.createNameFromRegionLocation(0, -1));
		assertEquals("r.0.-2.mca", McaFileHelpers.createNameFromRegionLocation(0, -2));
		assertEquals("r.0.1.mca", McaFileHelpers.createNameFromRegionLocation(0, 1));
		assertEquals("r.-1.0.mca", McaFileHelpers.createNameFromRegionLocation(-1, 0));
		assertEquals("r.-2.0.mca", McaFileHelpers.createNameFromRegionLocation(-2, 0));
	}

	public void testMakeMyCoverageGreatAgain() {
		assertThrowsException(() -> McaFileHelpers.read((String) null), NullPointerException.class);
		assertThrowsException(() -> McaFileHelpers.write(null, (String) null), NullPointerException.class);
		assertThrowsException(() -> McaFileHelpers.write(null, (File) null), NullPointerException.class);
		assertThrowsException(() -> McaFileHelpers.write(null, (String) null, false), NullPointerException.class);
		assertThrowsException(() -> McaFileHelpers.read("r.a.b.mca"), IllegalArgumentException.class);
		assertThrowsException(() -> new McaRegionFile(0, 0).serialize(null), IllegalArgumentException.class);

		// test overwriting file
		McaRegionFile m = new McaRegionFile(0, 0);
		m.setChunk(0, TerrainChunk.newChunk());
		File target = getNewTmpFile("r.0.0.mca");
		assertThrowsNoException(() -> McaFileHelpers.write(m, target, false));
		assertThrowsNoException(() -> McaFileHelpers.write(m, target, false));
	}

	public void testAutoCreateMcaFile() {
		McaFileBase<?> mcaFile = McaFileHelpers.autoMCAFile(Paths.get("region", "r.1.2.mca"));
		assertTrue(mcaFile instanceof McaRegionFile);
		assertEquals(1, mcaFile.getRegionX());
		assertEquals(2, mcaFile.getRegionZ());

		mcaFile = McaFileHelpers.autoMCAFile(Paths.get("poi", "r.3.-4.mca"));
		assertTrue(mcaFile instanceof McaPoiFile);
		assertEquals(3, mcaFile.getRegionX());
		assertEquals(-4, mcaFile.getRegionZ());

		mcaFile = McaFileHelpers.autoMCAFile(Paths.get("entities", "r.-5.6.mca"));
		assertTrue(mcaFile instanceof McaEntitiesFile);
		assertEquals(-5, mcaFile.getRegionX());
		assertEquals(6, mcaFile.getRegionZ());
	}
}
