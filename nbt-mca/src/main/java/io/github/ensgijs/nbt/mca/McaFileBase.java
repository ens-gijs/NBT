package io.github.ensgijs.nbt.mca;


import io.github.ensgijs.nbt.io.CompressionType;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.mca.util.ChunkIterator;
import io.github.ensgijs.nbt.util.ArgValidator;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An abstract representation of an mca (aka "region") file.
 */
public abstract class McaFileBase<T extends ChunkBase> implements Iterable<T> {

	protected int regionX, regionZ;
	protected T[] chunks;
	protected int minDataVersion;
	protected int maxDataVersion;
	protected int defaultDataVersion = DataVersion.latest().id();  // data version to use when creating new chunks

	/**
	 * MCA file represents a world save file used by Minecraft to store world
	 * data on the hard drive.
	 * This constructor needs the x- and z-coordinates of the stored region,
	 * which can usually be taken from the file name {@code r.x.z.mca}
	 *
	 * <p>Use this constructor when you plan to {@code deserialize(..)} an MCA file.
	 * If you are creating an MCA file from scratch prefer {@link #McaFileBase(int, int, int)}.
	 * @param regionX The x-coordinate of this mca file in region coordinates.
	 * @param regionZ The z-coordinate of this mca file in region coordinates.
	 */
	public McaFileBase(int regionX, int regionZ) {
		this.regionX = regionX;
		this.regionZ = regionZ;
	}

	/**
	 * Use this constructor to specify a default data version when creating MCA files without loading
	 * from disk.
	 *
	 * @param regionX The x-coordinate of this mca file in region coordinates.
	 * @param regionZ The z-coordinate of this mca file in region coordinates.
	 * @param defaultDataVersion Data version which will be used when creating new chunks.
	 */
	public McaFileBase(int regionX, int regionZ, int defaultDataVersion) {
		this.regionX = regionX;
		this.regionZ = regionZ;
		this.defaultDataVersion = defaultDataVersion;
		this.minDataVersion = defaultDataVersion;
		this.maxDataVersion = defaultDataVersion;
	}

	/**
	 * Use this constructor to specify a default data version when creating MCA files without loading
	 * from disk.
	 *
	 * @param regionX The x-coordinate of this mca file in region coordinates.
	 * @param regionZ The z-coordinate of this mca file in region coordinates.
	 * @param defaultDataVersion Data version which will be used when creating new chunks.
	 */
	public McaFileBase(int regionX, int regionZ, DataVersion defaultDataVersion) {
		this(regionX, regionZ, defaultDataVersion.id());
	}

	/**
	 * Gets the count of non-null chunks.
	 */
	public int count() {
		return (int) stream().filter(Objects::nonNull).count();
	}

	/**
	 * Get minimum data version of found in loaded chunk data
	 */
	public int getMinChunkDataVersion() {
		return minDataVersion;
	}

	/**
	 * Get maximum data version of found in loaded chunk data
	 */
	public int getMaxChunkDataVersion() {
		return maxDataVersion;
	}

	/**
	 * Get chunk version which will be used when automatically creating new chunks
	 * and for chunks created by {@link #createChunk()}.
	 */
	public int getDefaultChunkDataVersion() {
		return defaultDataVersion;
	}

	public DataVersion getDefaultChunkDataVersionEnum() {
		return DataVersion.bestFor(defaultDataVersion);
	}

	/**
	 * Set chunk version which will be used when automatically creating new chunks
	 * and for chunks created by {@link #createChunk()}.
	 */
	public void setDefaultChunkDataVersion(int defaultDataVersion) {
		this.defaultDataVersion = defaultDataVersion;
	}

	public void setDefaultChunkDataVersion(DataVersion defaultDataVersion) {
		this.defaultDataVersion = defaultDataVersion.id();
	}

	/**
	 * @return The x-value currently set for this mca file in region coordinates.
	 * @see #moveRegion(int, int, long, boolean)
	 */
	public int getRegionX() {
		return regionX;
	}

	/**
	 * @return The z-value currently set for this mca file in region coordinates.
	 * @see #moveRegion(int, int, long, boolean)
	 */
	public int getRegionZ() {
		return regionZ;
	}

	/**
	 * Returns result of calling {@link McaFileHelpers#createNameFromRegionLocation(int, int)}
	 * with current region coordinate values.
	 * @return A mca filename in the format "r.{regionX}.{regionZ}.mca"
	 */
	public String createRegionName() {
		return McaFileHelpers.createNameFromRegionLocation(regionX, regionZ);
	}

	/**
	 * @return type of chunk this MCA File holds
	 */
	public abstract Class<T> chunkClass();

	/**
	 * Creates a new chunk properly initialized to be compatible with this MCA file. At a minimum the new
	 * chunk will have an appropriate data version set.
	 */
	public abstract T createChunk();

	/**
	 * Called to deserialize a Chunk. Caller will have set the position of {@code raf} to start reading.
	 * @param raf The {@code RandomAccessFile} to read from.
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @param timestamp The timestamp when this chunk was last updated as a UNIX timestamp.
	 * @param chunkAbsXZ Absolute chunk XZ coord as calculated from region location and chunk index.
	 * @return Deserialized chunk.
	 * @throws IOException if something went wrong during deserialization.
	 */
	protected T deserializeChunk(RandomAccessFile raf, long loadFlags, int timestamp, IntPointXZ chunkAbsXZ) throws IOException {
		T chunk = createChunk();
		chunk.deserialize(raf, loadFlags, timestamp, chunkAbsXZ.getX(), chunkAbsXZ.getZ());
		// I'm going to leave this as an "idea" for now
//		if (!chunkAbsXZ.equals(chunk.getChunkX(), chunk.getChunkZ())) {
//			// this would be a good place for a logger warning
//			if (chunk.moveChunkImplemented() && chunk.moveChunkHasFullVersionSupport()) {
//				chunk.moveChunk(chunkAbsXZ.getX(), chunkAbsXZ.getZ());
//			}
//		}
		return chunk;
	}

	/**
	 * Reads an .mca file from a {@code RandomAccessFile} into this object.
	 * This method does not perform any cleanups on the data.
	 * @param raf The {@code RandomAccessFile} to read from.
	 * @throws IOException If something went wrong during deserialization.
	 */
	public void deserialize(RandomAccessFile raf) throws IOException {
		deserialize(raf, LoadFlags.LOAD_ALL_DATA);
	}

	/**
	 * Reads an .mca file from a {@code RandomAccessFile} into this object.
	 * This method does not perform any cleanups on the data.
	 * @param raf The {@code RandomAccessFile} to read from.
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @throws IOException If something went wrong during deserialization.
	 */
	@SuppressWarnings("unchecked")
	public void deserialize(RandomAccessFile raf, long loadFlags) throws IOException {
		chunks = (T[]) Array.newInstance(chunkClass(), 1024);
		minDataVersion = Integer.MAX_VALUE;
		maxDataVersion = Integer.MIN_VALUE;
		final IntPointXZ chunkOffsetXZ = new IntPointXZ(regionX * 32, regionZ * 32);
		for (int i = 0; i < 1024; i++) {
			// Location information for a chunk consists of four bytes split into two fields:
			// the first three bytes are a (big-endian) offset in 4KiB sectors from the start of the file,
			// and a remaining byte that gives the length of the chunk (also in 4KiB sectors, rounded up).
			// Chunks are always less than 1MiB in size. If a chunk isn't present in the region file
			// (e.g. because it hasn't been generated or migrated yet), both fields are zero.
			raf.seek(i * 4);
			int offset = raf.read() << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offset |= raf.read() & 0xFF;
			if (raf.readByte() == 0) {
				continue;
			}
			raf.seek(4096 + (i * 4));
			int timestamp = raf.readInt();
			raf.seek(4096L * offset + 4); //+4: skip data size
			T chunk = deserializeChunk(raf, loadFlags, timestamp,
					getRelativeChunkXZ(i).add(chunkOffsetXZ));
			chunks[i] = chunk;
			if (chunk != null && chunk.hasDataVersion()) {
				if (chunk.getDataVersion() < minDataVersion) {
					minDataVersion = chunk.getDataVersion();
				}
				if (chunk.getDataVersion() > maxDataVersion) {
					maxDataVersion = chunk.getDataVersion();
				}
			}
		}
		maxDataVersion = Math.max(maxDataVersion, 0);
		minDataVersion = Math.min(minDataVersion, maxDataVersion);
		defaultDataVersion = maxDataVersion;
	}

	/**
	 * Calls {@link McaFileBase#serialize(RandomAccessFile, CompressionType, boolean)} with GZIP chunk compression and
	 * without updating any timestamps.
	 * @see McaFileBase#serialize(RandomAccessFile, CompressionType, boolean)
	 * @param raf The {@code RandomAccessFile} to write to.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something went wrong during serialization.
	 */
	public int serialize(RandomAccessFile raf) throws IOException {
		return serialize(raf, CompressionType.ZLIB, false);
	}

	/**
	 * Calls {@link McaFileBase#serialize(RandomAccessFile, CompressionType, boolean)} without updating any timestamps.
	 * @see McaFileBase#serialize(RandomAccessFile, CompressionType, boolean)
	 * @param raf The {@code RandomAccessFile} to write to.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something went wrong during serialization.
	 */
	public int serialize(RandomAccessFile raf, CompressionType chunkCompressionType) throws IOException {
		return serialize(raf, chunkCompressionType, false);
	}

	/**
	 * Serializes this object to an .mca file.
	 * This method does not perform any cleanups on the data.
	 * @param raf The {@code RandomAccessFile} to write to.
	 * @param changeLastUpdate Whether it should update all timestamps that show
	 *                         when this file was last updated.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something went wrong during serialization.
	 */
	public int serialize(RandomAccessFile raf, CompressionType chunkCompressionType, boolean changeLastUpdate) throws IOException {
		ArgValidator.requireValue(raf, "raf");
		int globalOffset = 2;
		int lastWritten = 0;
		int timestamp = (int) (System.currentTimeMillis() / 1000L);
		int chunksWritten = 0;
		int chunkXOffset = McaFileHelpers.regionToChunk(regionX);
		int chunkZOffset = McaFileHelpers.regionToChunk(regionZ);

		// ensure that the mca header tables always exist
		raf.seek(0x2000 - 4);
		raf.writeInt(0);

		if (chunks == null) {
			return 0;
		}

		for (int cz = 0; cz < 32; cz++) {
			for (int cx = 0; cx < 32; cx++) {
				int index = getChunkIndex(cx, cz);
				T chunk = chunks[index];
				if (chunk == null) {
					continue;
				}
				raf.seek(4096L * globalOffset);
				lastWritten = chunk.serialize(raf, chunkXOffset + cx, chunkZOffset + cz, chunkCompressionType, true);

				chunksWritten++;

				// compute the count of 4kb sectors the chunk data occupies
				int sectors = (lastWritten >> 12) + (lastWritten % 4096 == 0 ? 0 : 1);

				raf.seek(index * 4L);
				raf.writeByte(globalOffset >>> 16);
				raf.writeByte(globalOffset >> 8 & 0xFF);
				raf.writeByte(globalOffset & 0xFF);
				raf.writeByte(sectors);

				// write timestamp
				raf.seek(index * 4L + 4096);
				raf.writeInt(changeLastUpdate ? timestamp : chunk.getLastMCAUpdate());

				globalOffset += sectors;
			}
		}

		// padding
		if (lastWritten % 4096 != 0) {
			raf.seek(globalOffset * 4096L - 1);
			raf.write(0);
		}
		return chunksWritten;
	}

	/**
	 * Set a specific Chunk at a specific index. The index must be in range of 0 - 1023.
	 * Take care as the given chunk is NOT copied by this call.
	 * @param index The index of the Chunk.
	 * @param chunk The Chunk to be set.
	 * @throws IndexOutOfBoundsException If index is not in the range.
	 */
	@SuppressWarnings("unchecked")
	public void setChunk(int index, T chunk) {
		checkIndex(index);
		if (chunks == null) {
			chunks = (T[]) Array.newInstance(chunkClass(), 1024);
		}
		// TODO: figure out how best to sync chunk abs xz
//		getRelativeChunkXZ(index).add(regionX * 32, regionZ * 32);
		chunks[index] = chunk;
	}

	/**
	 * Set a specific Chunk at a specific chunk location.
	 * The x- and z-value can be absolute chunk coordinates or they can be relative to the region origin.
	 * @param chunkX The x-coordinate of the Chunk.
	 * @param chunkZ The z-coordinate of the Chunk.
	 * @param chunk The chunk to be set.
	 *
	 */
	public void setChunk(int chunkX, int chunkZ, T chunk) {
		setChunk(getChunkIndex(chunkX, chunkZ), chunk);
	}

	/**
	 * Returns the chunk data of a chunk at a specific index in this file.
	 * @param index The index of the chunk in this file.
	 * @return The chunk data.
	 */
	public T getChunk(int index) {
		checkIndex(index);
		if (chunks == null) {
			return null;
		}
		return chunks[index];
	}

	/**
	 * Returns the chunk data of a chunk in this file.
	 * @param chunkX The x-coordinate of the chunk.
	 * @param chunkZ The z-coordinate of the chunk.
	 * @return The chunk data.
	 */
	public T getChunk(int chunkX, int chunkZ) {
		return getChunk(getChunkIndex(chunkX, chunkZ));
	}

	/**
	 * Removes the chunk at the given index (sets it to null) and returns the previous value.
	 * @param index chunk index [0..1024)
	 * @return chunk which was removed, or null if there was none.
	 */
	public T removeChunk(int index) {
		T chunk = chunks[index];
		chunks[index] = null;
		return chunk;
	}

	/**
	 * Removes the chunk at the given xz (sets it to null) and returns the previous value.
	 * Works with absolute and relative coordinates.
	 * @param chunkX chunk x
	 * @param chunkZ chunk z
	 * @return chunk which was removed, or null if there was none.
	 */
	public T removeChunk(int chunkX, int chunkZ) {
		return removeChunk(getChunkIndex(chunkX, chunkZ));
	}

	/**
	 * Calculates the index of a chunk from its x and z-coordinates in this region.
	 * This works with absolute and relative coordinates.
	 * @param chunkX The x-coordinate of the chunk.
	 * @param chunkZ The z-coordinate of the chunk.
	 * @return The index of this chunk or -1 if either chunkX or chunkZ were {@link ChunkBase#NO_CHUNK_COORD_SENTINEL}.
	 */
	public static int getChunkIndex(int chunkX, int chunkZ) {
		if (chunkX != ChunkBase.NO_CHUNK_COORD_SENTINEL && chunkZ != ChunkBase.NO_CHUNK_COORD_SENTINEL) {
			return ((chunkZ & 0x1F) << 5) | (chunkX & 0x1F);
		}
		return -1;
	}

	/**
	 * Calculates the relative x z of a chunk within the current region given an index.
	 *
	 * @param index index of chunk in range [0..1024)
	 * @return x z location of the chunk in region relative coordinates where x and z each range [0..32)
	 */
	public static IntPointXZ getRelativeChunkXZ(int index) {
		checkIndex(index);
		return new IntPointXZ(index & 0x1F, index >> 5);
	}

	protected static void checkIndex(int index) {
		if (index < 0 || index > 1023) {
			throw new IndexOutOfBoundsException();
		}
	}

	protected T createChunkIfMissing(int blockX, int blockZ) {
		int chunkX = McaFileHelpers.blockToChunk(blockX), chunkZ = McaFileHelpers.blockToChunk(blockZ);
		T chunk = getChunk(chunkX, chunkZ);
		if (chunk == null) {
			chunk = createChunk();
			setChunk(getChunkIndex(chunkX, chunkZ), chunk);
		}
		return chunk;
	}

	public boolean moveRegion(int newRegionX, int newRegionZ, long moveChunkFlags, boolean force) {
		// Testing note: don't forget that updateHandle() needs to be called to see the results of this move!
		boolean changed = false;
		IntPointXZ newRegionMinChunkXZ = new IntPointXZ(newRegionX, newRegionZ).transformRegionToChunk();
		ChunkIterator<T> iter = this.iterator();
		while (iter.hasNext()) {
			T chunk = iter.next();
			if (chunk != null) {
				IntPointXZ newChunkXZ = iter.currentXZ().add(newRegionMinChunkXZ);
				changed |= chunk.moveChunk(newChunkXZ.getX(), newChunkXZ.getZ(), moveChunkFlags, force);
			}
		}
		this.regionX = newRegionX;
		this.regionZ = newRegionZ;
		return changed;
	}

	@Override
	public ChunkIterator<T> iterator() {
		return new ChunkIteratorImpl<>(this);
	}

	public Stream<T> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	protected static class ChunkIteratorImpl<I extends ChunkBase> implements ChunkIterator<I> {
		private final McaFileBase<I> owner;
		private int currentIndex;

		public ChunkIteratorImpl(McaFileBase<I> owner) {
			this.owner = owner;
			currentIndex = -1;
		}

		@Override
		public boolean hasNext() {
			return currentIndex < 1023;
		}

		@Override
		public I next() {
			if (!hasNext()) throw new NoSuchElementException();
			return owner.getChunk(++currentIndex);
		}

		@Override
		public void remove() {
			owner.setChunk(currentIndex, null);
		}

		@Override
		public void set(I chunk) {
			owner.setChunk(currentIndex, chunk);
		}

		@Override
		public int currentIndex() {
			return currentIndex;
		}

		@Override
		public int currentAbsoluteX() {
			return currentX() + owner.getRegionX() * 32;
		}

		@Override
		public int currentAbsoluteZ() {
			return currentZ() + owner.getRegionZ() * 32;
		}
	}
}
