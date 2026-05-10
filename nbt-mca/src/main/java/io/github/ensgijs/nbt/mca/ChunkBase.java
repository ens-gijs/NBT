package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.BinaryNbtDeserializer;
import io.github.ensgijs.nbt.io.BinaryNbtSerializer;
import io.github.ensgijs.nbt.io.CompressionType;
import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.MoveChunkFlags;
import io.github.ensgijs.nbt.mca.util.*;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.Tag;
import io.github.ensgijs.nbt.util.ObservedCompoundTag;
import io.github.ensgijs.nbt.query.NbtPath;

import java.io.*;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Abstraction for the base of all chunk types. Not all chunks types are sectioned, that layer comes further up
 * the hierarchy.
 * <p>
 *     <b>Cautionary note to implementors - DO NOT USE INLINE MEMBER INITIALIZATION IN YOUR CLASSES</b><br>
 *     Define all member initialization in {@link #initMembers()} or be very confused!
 *     Due to how Java initializes objects, this base class will call {@link #initReferences(long)} before any inline
 *     member initialization has occurred. The symptom of using inline member initialization is that you will get
 *     very confusing {@link NullPointerException}'s from within {@link #initReferences(long)} for members which
 *     are accessed by your {@link #initReferences(long)} implementation that you have defined inline initializers for
 *     because those initializers will not run until AFTER {@link #initReferences(long)} returns.
 * </p><p>
 *     It is however "safe" to use inline member initialization for any members which are not accessed from within
 *     {@link #initReferences(long)} - but unless you really fully understand the warning above and its full
 *     ramifications just don't do it.
 * </p>
 * @see SectionedChunkBase
 */
public abstract class ChunkBase implements VersionedDataContainer, TagWrapper<CompoundTag>, TracksUnreadDataTags {

	public static final int NO_CHUNK_COORD_SENTINEL = Integer.MIN_VALUE;

	protected final long originalLoadFlags;
	protected int dataVersion;
	protected int chunkX = NO_CHUNK_COORD_SENTINEL;
	protected int chunkZ = NO_CHUNK_COORD_SENTINEL;
	// TODO: this partial state thing is questionable - evaluate if the semantics are valid or broken
	protected boolean partial;
	protected boolean raw;
	protected int lastMCAUpdate;
	/** Should be treated as effectively read-only by child classes until after {@link #initReferences}
	 * invocation has returned. */
	protected CompoundTag data;
	protected Set<String> unreadDataTagKeys;

	/**
	 * {@inheritDoc}
	 */
	public Set<String> getUnreadDataTagKeys() {
		return unreadDataTagKeys;
	}

	/**
	 * {@inheritDoc}
	 * @return NotNull - if LoadFlags specified {@link LoadFlags#RAW} then the raw data is returned - else a new
	 * CompoundTag populated, by reference, with values that were not read during {@link #initReferences(long)}.
	 */
	public CompoundTag getUnreadDataTags() {
		if (raw) return data;
		CompoundTag unread = new CompoundTag(unreadDataTagKeys.size());
		data.forEach((k, v) -> {
			if (unreadDataTagKeys.contains(k)) {
				unread.put(k, v);
			}
		});
		return unread;
	}

	/**
	 * Due to how Java initializes objects and how this class hierarchy is setup it is ill-advised to use inline member
	 * initialization because {@link #initReferences(long)} will be called before members are initialized which WILL
	 * result in very confusing {@link NullPointerException}'s being thrown from within {@link #initReferences(long)}.
	 * This is not a problem that can be solved by moving initialization into your constructors, because you must call
	 * the super constructor as the first line of your child constructor!
	 * <p>So, to get around this hurdle, perform all member initialization you would normally inline in your
	 * class def, within this method instead. Implementers should never need to call this method themselves
	 * as ChunkBase will always call it, even from the default constructor. Remember to call {@code super();}
	 * from your default constructors to maintain this behavior.</p>
	 */
	protected void initMembers() { }

	protected ChunkBase(int dataVersion) {
		this.dataVersion = dataVersion;
		this.originalLoadFlags = LoadFlags.LOAD_ALL_DATA;
		this.lastMCAUpdate = (int)(System.currentTimeMillis() / 1000);
		initMembers();
	}

	/**
	 * Create a new chunk based on raw base data from a region file.
	 * @param data The raw base data to be used.
	 */
	public ChunkBase(CompoundTag data) {
		this(data, LoadFlags.LOAD_ALL_DATA);
	}

	/**
	 * Create a new chunk based on raw base data from a region file.
	 * @param data The raw base data to be used.
	 * @param loadFlags Union of {@link LoadFlags} to process.
	 */
	public ChunkBase(CompoundTag data, long loadFlags) {
		this.data = data;
		this.originalLoadFlags = loadFlags;
		initMembers();
		initReferences0(loadFlags);
	}

	private void initReferences0(long loadFlags) {
		Objects.requireNonNull(data, "data cannot be null");
		if ((loadFlags & LoadFlags.RAW) != 0) {
			dataVersion = data.getInt("DataVersion");
			raw = true;
		} else {
			final ObservedCompoundTag observedData = new ObservedCompoundTag(data);
			dataVersion = observedData.getInt("DataVersion");
			if (dataVersion == 0) {
				throw new IllegalArgumentException("data does not contain \"DataVersion\" tag");
			}

			data = observedData;
			initReferences(loadFlags);
			if (data != observedData) {
				throw new IllegalStateException("this.data was replaced during initReferences execution - this breaks unreadDataTagKeys behavior!");
			}
			unreadDataTagKeys = observedData.unreadKeys();

			if ((loadFlags & LoadFlags.RELEASE_CHUNK_DATA_TAG) != 0) {
				data = null;
				// this is questionable... maybe if we also check that data version is within the known bounds too
				// (to count it as non-partial) we could be reasonably confidant that the saved chunk would at least
				// have a vanilla level of data.
				if ((loadFlags & LoadFlags.LOAD_ALL_DATA) != LoadFlags.LOAD_ALL_DATA) partial = true;
			} else {
				// stop observing the data tag
				data = observedData.wrappedTag();
			}
		}
	}

	/**
	 * Child classes should not call this method directly, it will be called for them.
	 * Raw and partial data handling is taken care of, this method will not be called if {@code loadFlags} is
	 * {@link LoadFlags#RAW}.
	 */
	protected abstract void initReferences(final long loadFlags);

	/**
	 * @return one of: region, entities, poi
	 */
	public abstract String getMcaType();

	/**
	 * {@inheritDoc}
	 */
	public int getDataVersion() {
		return dataVersion;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDataVersion(int dataVersion) {
		this.dataVersion = Math.max(0, dataVersion);
	}

	/**
	 * Gets this chunk's chunk-x coordinate. Returns {@link #NO_CHUNK_COORD_SENTINEL} if not supported or unknown.
	 * @see #moveChunk(int, int, long, boolean)
	 */
	public int getChunkX() {
		return chunkX;
	}

	/**
	 * Gets this chunk's chunk-z coordinate. Returns {@link #NO_CHUNK_COORD_SENTINEL} if not supported or unknown.
	 * @see #moveChunk(int, int, long, boolean)
	 */
	public int getChunkZ() {
		return chunkZ;
	}

	/**
	 * Gets this chunk's chunk-xz coordinates. Returns x = z = {@link #NO_CHUNK_COORD_SENTINEL} if not supported or unknown.
	 * @see #moveChunk(int, int, long, boolean)
	 */
	public IntPointXZ getChunkXZ() {
		return new IntPointXZ(getChunkX(), getChunkZ());
	}

	/**
	 * Indicates if this chunk implementation supports calling {@link #moveChunk(int, int, long, boolean)}.
	 * @return false if {@link #moveChunk(int, int, long, boolean)} is not implemented (calling it will always throw).
	 */
	public abstract boolean moveChunkImplemented();

	/**
	 * Indicates if the current chunk can be be moved with confidence or not. If this function returns false
	 * and {@link #moveChunkImplemented()} returns true then you must use {@code moveChunk(x, z, true)} to attempt
	 * a best effort move.
	 */
	public abstract boolean moveChunkHasFullVersionSupport();

	/**
	 * Attempts to update all tags that use absolute positions within this chunk.
	 * <p>Call {@link #moveChunkImplemented()} to check if this implementation supports chunk relocation. Also
	 * check the result of {@link #moveChunkHasFullVersionSupport()} to get an idea of the level of support
	 * this implementation has for the current chunk.
	 * <p>If {@code force = true} the result of calling this function cannot be guaranteed to be complete and
	 * may still throw {@link UnsupportedOperationException}.
	 * @param newChunkX new absolute chunk-x
	 * @param newChunkZ new absolute chunk-z
	 * @param moveChunkFlags {@link MoveChunkFlags} OR'd together to control move chunk behavior.
	 * @param force true to ignore the guidance of {@link #moveChunkHasFullVersionSupport()} and make a best effort
	 *              anyway.
	 * @return true if any data was changed as a result of this call
	 * @throws UnsupportedOperationException thrown if this chunk implementation doest support moves, or moves
	 * for this chunks version (possibly even if force = true).
	 */
	public abstract boolean moveChunk(int newChunkX, int newChunkZ, long moveChunkFlags, boolean force);

	/**
	 * Calls {@code moveChunk(newChunkX, newChunkZ, moveChunkFlags, false);}
	 * @see #moveChunk(int, int, long, boolean)
	 */
	public boolean moveChunk(int chunkX, int chunkZ, long moveChunkFlags) {
		return moveChunk(chunkX, chunkZ, moveChunkFlags, false);
	}

	/**
	 * Serializes this chunk to a <code>DataOutput</code> sink.
	 * @param sink The DataOutput to be written to.
	 * @param xPos The x-coordinate of the chunk.
	 * @param zPos The z-coordinate of the chunk.
	 * @param compressionType Chunk compression strategy to use.
	 * @param writeByteLengthPrefixInt when true the first thing written to the sink will be the total bytes written
	 *                                 (a value equal to 4 less than the return value).
	 * @return The amount of bytes written to the DataOutput.
	 * @throws UnsupportedOperationException When something went wrong during writing.
	 * @throws IOException When something went wrong during writing.
	 */
	public int serialize(DataOutput sink, int xPos, int zPos, CompressionType compressionType, boolean writeByteLengthPrefixInt) throws IOException {
		if (partial) {
			throw new UnsupportedOperationException("Partially loaded chunks cannot be serialized");
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
		new BinaryNbtSerializer(compressionType).toStream(new NamedTag(null, updateHandle(xPos, zPos)), baos);
//		try (BufferedOutputStream nbtOut = new BufferedOutputStream(compressionType.compress(baos))) {
//			new BinaryNbtSerializer(false).toStream(new NamedTag(null, updateHandle(xPos, zPos)), nbtOut);
//		}
		byte[] rawData = baos.toByteArray();
		if (writeByteLengthPrefixInt)
			sink.writeInt(rawData.length + 1); // including the byte to store the compression type
		sink.writeByte(compressionType.getID());
		sink.write(rawData);
		return rawData.length + (writeByteLengthPrefixInt ? 5 : 1);
	}

	/**
	 * Reads chunk data from a RandomAccessFile. The RandomAccessFile must already be at the correct position.
	 * <p>It is expected that the byte size int has already been read and the next byte indicates the compression
	 * used. Essentially this method is symmetrical to {@link #serialize(DataOutput, int, int, CompressionType, boolean)}
	 * when passing writeByteLengthPrefixInt=false</p>
	 * @param raf The RandomAccessFile to read the chunk data from.
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @param lastMCAUpdateTimestamp Last mca update timestamp - epoch seconds. If LT0 the current system timestamp will be used.
	 * @param chunkAbsXHint The absolute chunk x-coord which should be used if the nbt data doesn't contain this information.
	 * @param chunkAbsZHint The absolute chunk z-coord which should be used if the nbt data doesn't contain this information.
	 * @throws IOException When something went wrong during reading.
	 */
	public void deserialize(RandomAccessFile raf, long loadFlags, int lastMCAUpdateTimestamp, int chunkAbsXHint, int chunkAbsZHint) throws IOException {
		deserialize(new FileInputStream(raf.getFD()), loadFlags, lastMCAUpdateTimestamp, chunkAbsXHint, chunkAbsZHint);
	}

	/**
	 * Reads chunk data from an InputStream. The InputStream must already be at the correct position.
	 * <p>It is expected that the byte size int has already been read and the next byte indicates the compression
	 * used. Essentially this method is symmetrical to {@link #serialize(DataOutput, int, int, CompressionType, boolean)}
	 * when passing writeByteLengthPrefixInt=false</p>
	 * @param inputStream The stream to read the chunk data from.
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @param lastMCAUpdateTimestamp Last mca update timestamp - epoch seconds. If LT0 the current system timestamp will be used.
	 * @param chunkAbsXHint The absolute chunk x-coord which should be used if the nbt data doesn't contain this information.
	 * @param chunkAbsZHint The absolute chunk z-coord which should be used if the nbt data doesn't contain this information.
	 * @throws IOException When something went wrong during reading.
	 */
	public void deserialize(InputStream inputStream, long loadFlags, int lastMCAUpdateTimestamp, int chunkAbsXHint, int chunkAbsZHint) throws IOException {
		int compressionTypeByte = inputStream.read();
		if (compressionTypeByte < 0)
			throw new EOFException();
		CompressionType compressionType = CompressionType.getFromID((byte) compressionTypeByte);
		if (compressionType == null) {
			throw new IOException("invalid compression type " + compressionTypeByte);
		}
		NamedTag tag = new BinaryNbtDeserializer(compressionType).fromStream(inputStream);
		if (tag != null && tag.getTag() instanceof CompoundTag) {
			data = (CompoundTag) tag.getTag();
			this.lastMCAUpdate = lastMCAUpdateTimestamp >= 0 ? lastMCAUpdateTimestamp : (int)(System.currentTimeMillis() / 1000);
			this.chunkX = chunkAbsXHint;
			this.chunkZ = chunkAbsZHint;
			initReferences0(loadFlags);
		} else {
			throw new IOException("invalid data tag: " + (tag == null ? "null" : tag.getClass().getName()));
		}
	}

	/**
	 * @return The timestamp when this region file was last updated in seconds since 1970-01-01.
	 */
	public int getLastMCAUpdate() {
		return lastMCAUpdate;
	}

	/**
	 * Sets the timestamp when this region file was last updated in seconds since 1970-01-01.
	 * @param lastMCAUpdate The time in seconds since 1970-01-01.
	 */
	public void setLastMCAUpdate(int lastMCAUpdate) {
		checkRaw();
		this.lastMCAUpdate = lastMCAUpdate;
	}

	/**
	 * @throws UnsupportedOperationException thrown if raw is true
	 */
	protected void checkRaw() {
		if (raw) {
			throw new UnsupportedOperationException("Cannot use helpers for this field when working with raw data");
		}
	}

	protected void checkPartial() {
		if (data == null) {
			throw new UnsupportedOperationException("Chunk was only partially loaded due to LoadFlags used");
		}
	}

	protected void checkChunkXZ() {
		if (chunkX == NO_CHUNK_COORD_SENTINEL || chunkZ == NO_CHUNK_COORD_SENTINEL) {
			throw new UnsupportedOperationException("This chunk doesn't know its XZ location");
		}
	}

	/**
	 * Provides a reference to the full chunk data.
	 * @return The full chunk data or null if there is none, e.g. when this chunk has only been loaded partially.
	 */
	public CompoundTag getHandle() {
		return data;
	}

	public CompoundTag updateHandle() {
		if (data == null) {
			throw new UnsupportedOperationException(
					"Cannot updateHandle() because data tag is null. This is probably because "+
					"the LoadFlag RELEASE_CHUNK_DATA_TAG was specified");
		}
		if (!raw) {
			data.putInt("DataVersion", dataVersion);
		}
		return data;
	}

	// Note: Not all chunk formats store xz in their NBT, but {@link McaFileBase} will call this update method
	// to give them the chance to record them.
	public CompoundTag updateHandle(int xPos, int zPos) {
		return updateHandle();
	}


	/**
	 * @param vaPath version aware nbt path
	 * @param <R> Return Type
	 * @return tag value, or null if there is none, or if the given vaPath doesn't support the current version
	 */
	protected <R extends Tag<?>> R getTag(VersionAware<NbtPath> vaPath) {
		NbtPath path = vaPath.get(dataVersion);
		if (path == null) return null;  // not supported by this version
		return path.getTag(data);
	}

	/**
	 * Simple but powerful helper - example usage
	 * <pre>{@code long myLong = getTagValue(vaPath, LongTag::asLong, 0L);}</pre>
	 * @param vaPath version aware nbt path
	 * @param evaluator value provider, given the tag (iff not null)
	 * @param defaultValue value to return if the tag specified by vaPath does not exist
	 * @param <TT> Tag Type
	 * @param <R> Return Type
	 * @return result of calling evaluator, or defaultValue if the tag didn't exist
	 */
	protected <TT extends Tag<?>, R> R getTagValue(VersionAware<NbtPath> vaPath, Function<TT, R> evaluator, R defaultValue) {
		TT tag = getTag(vaPath);
		return tag != null ? evaluator.apply(tag) : defaultValue;
	}

	/**
	 * @param vaPath version aware nbt path
	 * @param evaluator value provider, given the tag (iff not null)
	 * @param <TT> Tag Type
	 * @param <R> Return Type
	 * @return result of calling evaluator, or NULL if the tag didn't exist
	 */
	protected <TT extends Tag<?>, R> R getTagValue(VersionAware<NbtPath> vaPath, Function<TT, R> evaluator) {
		return getTagValue(vaPath, evaluator, null);
	}

	/**
	 * Sets the given tag, or removes it if null. If tag is not null, parent CompoundTags will be created as-needed.
	 * If the given vaPath does not support the current data version, then NO ACTION is performed.
	 * @param vaPath version aware nbt path
	 * @param tag tag value to set - if null then the value is REMOVED
	 */
	protected void setTag(VersionAware<NbtPath> vaPath, Tag<?> tag) {
		NbtPath path = vaPath.get(dataVersion);
		if (path == null) return;  // not supported by this version
		path.putTag(data, tag, tag != null);
	}

	/**
	 * Sets the given tag (if it's not null). Creates parent CompoundTags as-needed.
	 * If the given vaPath does not support the current data version, then NO ACTION is performed.
	 * @param vaPath version aware nbt path
	 * @param tag tag value to set - nothing happens if this value is null
	 */
	protected void setTagIfNotNull(VersionAware<NbtPath> vaPath, Tag<?> tag) {
		if (tag != null) {
			setTag(vaPath, tag);
		}
	}

	/**
	 * @return Index of this chunk in its owning region file or -1 if either chunk X or Z is
	 * {@link #NO_CHUNK_COORD_SENTINEL}.
	 */
	public int getIndex() {
		if (getChunkX() != NO_CHUNK_COORD_SENTINEL && getChunkZ() != NO_CHUNK_COORD_SENTINEL) {
			return McaFileBase.getChunkIndex(getChunkX(), getChunkZ());
		}
		return -1;
	}

	/**
	 * Gets the region file X coord which this chunk <em>should</em> belong to given its current {@link #getChunkX()}.
	 * Returns {@link #NO_CHUNK_COORD_SENTINEL} if {@link #getChunkX()} returns {@link #NO_CHUNK_COORD_SENTINEL}.
	 */
	public int getRegionX() {
		int x = getChunkX();
		return x != NO_CHUNK_COORD_SENTINEL ? x >> 5 : NO_CHUNK_COORD_SENTINEL;
	}

	/**
	 * Gets the region file Z coord which this chunk <em>should</em> belong to given its current {@link #getChunkZ()}.
	 * Returns {@link #NO_CHUNK_COORD_SENTINEL} if {@link #getChunkX()} returns {@link #NO_CHUNK_COORD_SENTINEL}.
	 */
	public int getRegionZ() {
		int z = getChunkZ();
		return z != NO_CHUNK_COORD_SENTINEL ? z >> 5 : NO_CHUNK_COORD_SENTINEL;
	}

	/**
	 * Gets the region file XZ coord which this chunk <em>should</em> belong to given its current {@link #getChunkXZ()}.
	 * Returns XZ({@link #NO_CHUNK_COORD_SENTINEL}, {@link #NO_CHUNK_COORD_SENTINEL}) if {@link #getChunkXZ()} returns
	 * XZ({@link #NO_CHUNK_COORD_SENTINEL}, {@link #NO_CHUNK_COORD_SENTINEL}).
	 */
	public IntPointXZ getRegionXZ() {
		return new IntPointXZ(getRegionX(), getRegionZ());
	}
}
