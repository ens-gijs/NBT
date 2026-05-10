package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.mca.io.McaFileHelpers;

/**
 * Represents a Terrain data mca file (one that lives in the /region folder).
 * Prior to MC 1.14 /region/*.mca files where the only ones that existed, 1.14 introduced /poi/*.mca
 * and 1.17 added /entities/*.mca - this class (currently) supports both legacy region files (that contain
 * entity data) as well as modern ones that do not.
 */
public class McaRegionFile extends McaFileBase<TerrainChunk> implements Iterable<TerrainChunk> {
	/**
	 * The default chunk data version used when no custom version is supplied.
	 * <p>Deprecated: use {@code DataVersion.latest().id()} instead.
	 */
	@Deprecated
	public static final int DEFAULT_DATA_VERSION = DataVersion.latest().id();

	/**
	 * {@inheritDoc}
	 */
	public McaRegionFile(int regionX, int regionZ) {
		super(regionX, regionZ);
	}

	/**
	 * {@inheritDoc}
	 */
	public McaRegionFile(int regionX, int regionZ, int defaultDataVersion) {
		super(regionX, regionZ, defaultDataVersion);
	}

	/**
	 * {@inheritDoc}
	 */
	public McaRegionFile(int regionX, int regionZ, DataVersion defaultDataVersion) {
		super(regionX, regionZ, defaultDataVersion);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<TerrainChunk> chunkClass() {
		return TerrainChunk.class;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TerrainChunk createChunk() {
		return TerrainChunk.newChunk(getDefaultChunkDataVersion());
	}

	/**
	 * @deprecated Use {@link #setBiomeAt(int, int, int, int)} instead
	 */
	@Deprecated
	public void setBiomeAt(int blockX, int blockZ, int biomeID) {
		createChunkIfMissing(blockX, blockZ).setLegacyBiomeAt(blockX, blockZ, biomeID);
	}

	public void setBiomeAt(int blockX, int blockY, int blockZ, int biomeID) {
		createChunkIfMissing(blockX, blockZ).setLegacyBiomeAt(blockX, blockY, blockZ, biomeID);
	}

	/**
	 * @deprecated Use {@link #getBiomeAt(int, int, int)} instead
	 */
	@Deprecated
	public int getBiomeAt(int blockX, int blockZ) {
		int chunkX = McaFileHelpers.blockToChunk(blockX), chunkZ = McaFileHelpers.blockToChunk(blockZ);
		TerrainChunk chunk = getChunk(getChunkIndex(chunkX, chunkZ));
		if (chunk == null) {
			return -1;
		}
		return chunk.getLegacyBiomeAt(blockX, blockZ);
	}

	/**
	 * Fetches the biome id at a specific block.
	 * @param blockX The x-coordinate of the block.
	 * @param blockY The y-coordinate of the block.
	 * @param blockZ The z-coordinate of the block.
	 * @return The biome id if the chunk exists and the chunk has biomes, otherwise -1.
	 * @deprecated unsupported after JAVA_1_18_21W38A
	 */
	public int getBiomeAt(int blockX, int blockY, int blockZ) {
		int chunkX = McaFileHelpers.blockToChunk(blockX), chunkZ = McaFileHelpers.blockToChunk(blockZ);
		TerrainChunk chunk = getChunk(getChunkIndex(chunkX, chunkZ));
		if (chunk == null) {
			return -1;
		}
		return chunk.getLegacyBiomeAt(blockX,blockY, blockZ);
	}

//	/**
//	 * Set a block state at a specific block location.
//	 * The block coordinates can be absolute coordinates or they can be relative to the region.
//	 * @param blockX The x-coordinate of the block.
//	 * @param blockY The y-coordinate of the block.
//	 * @param blockZ The z-coordinate of the block.
//	 * @param state The block state to be set.
//	 * @param cleanup Whether the Palette and the BLockStates should be recalculated after adding the block state.
//	 */
//	public void setBlockStateAt(int blockX, int blockY, int blockZ, CompoundTag state, boolean cleanup) {
//		createChunkIfMissing(blockX, blockZ).setBlockStateAt(blockX, blockY, blockZ, state, cleanup);
//	}
//
//	/**
//	 * Fetches a block state at a specific block location.
//	 * The block coordinates can be absolute coordinates or they can be relative to the region.
//	 * @param blockX The x-coordinate of the block.
//	 * @param blockY The y-coordinate of the block.
//	 * @param blockZ The z-coordinate of the block.
//	 * @return The block state or <code>null</code> if the chunk or the section do not exist.
//	 */
//	public CompoundTag getBlockStateAt(int blockX, int blockY, int blockZ) {
//		int chunkX = McaFileHelpers.blockToChunk(blockX), chunkZ = McaFileHelpers.blockToChunk(blockZ);
//		TerrainChunk chunk = getChunk(chunkX, chunkZ);
//		if (chunk == null) {
//			return null;
//		}
//		return chunk.getBlockStateAt(blockX, blockY, blockZ);
//	}
//
//	/**
//	 * Recalculates the Palette and the BlockStates of all chunks and sections of this region.
//	 */
//	public void cleanupPalettesAndBlockStates() {
//		for (TerrainChunk chunk : chunks) {
//			if (chunk != null) {
//				chunk.cleanupPalettesAndBlockStates();
//			}
//		}
//	}
}
