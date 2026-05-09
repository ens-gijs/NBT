package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.io.BinaryNbtSerializer;
import io.github.ensgijs.nbt.io.CompressionType;
import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.io.SilentIOException;
import io.github.ensgijs.nbt.mca.*;
import io.github.ensgijs.nbt.mca.util.ChunkIterator;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;
import io.github.ensgijs.nbt.mca.util.RegionBoundingRectangle;
import io.github.ensgijs.nbt.util.ArgValidator;
import io.github.ensgijs.nbt.util.Stopwatch;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.*;

/**
 * Provides random access read and write operations for working with MCA files.
 *
 * <p>This class can be used to modify exist or create new mca files.</p>
 *
 * <p>Using this class instead of {@link io.github.ensgijs.nbt.mca.McaFileBase} saves memory at the cost of increased IO.
 * This can be a good tradeoff when working with a large number of region files as each instance of
 * {@link RandomAccessMcaFile} has a memory footprint of a bit over 8KB while {@link io.github.ensgijs.nbt.mca.McaFileBase}
 * may use tens of MB per region file to load and keep all 1024 chunks in memory.</p>
 *
 * <p>You must remember to call {@link RandomAccessMcaFile#close()}! This is especially true if you have written any
 * chunk data - failing to call close may result in the mca file appearing to be empty to Minecraft and subsequent
 * mca file reads (though will clearly be non-empty on disk) or more likely will make the mca file appear to
 * have been corrupted. An mca file corrupted in this way could be recovered by skipping the file header (8kb)
 * and scanning the file sections directly - this library does not provide such a recovery mechanism at this time.</p>
 *
 * <p>Suggested usage pattern to ensure the file is always closed.</p>
 * <pre>{@code
 *      try(RandomAccessMcaFile<?> ra = new RandomAccessMcaFile(file)){
 *          ...
 *      }
 * }</pre>
 * @param <T> In truth, this class doesn't care what type of chunk it reads and writes - but being strict about
 *           which type of chunk is stored keeps users from shooting themselves in the foot.
 */
public class RandomAccessMcaFile<T extends ChunkBase> implements Closeable, Iterable<T> {
    private static final byte[] ZERO_FILL_BUFFER = new byte[4096];
    private final Class<T> chunkClass;
    protected final int[] chunkSectors = new int[1024];
    protected final int[] chunkTimestamps = new int[1024];
    private final RegionBoundingRectangle regionBounds;
    private final IntPointXZ regionXZ;
    private final IntPointXZ regionChunkOffsetXZ;
    private int chunksWritten;
    private int chunksRead;
    protected final RandomAccessFile raf;
    protected final SectorManager sectorManager = new SectorManager();
    protected boolean fileInitialized = false;
    protected boolean fileFinalized = false;

    protected long loadFlags = LoadFlags.LOAD_ALL_DATA;
    protected boolean autoOptimizeOnClose = false;
    protected boolean autoUpdateHandelOnWrite = true;
    protected boolean alwaysUpdateChunkLastUpdatedTimestamp = true;
    // TODO: use this flag to short-circuit file write operations if they are not necessary.
    //   Currently this flag is only ever set, never cleared.
    protected boolean isDirty = false;  // set true if any chunks were written or removed
    protected final boolean isReadOnly;

    private final Stopwatch fileInitializationStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch totalReadStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch totalWriteStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch chunkSerializationStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch fileFlushStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch fileOptimizationStopwatch = Stopwatch.createUnstarted();


    /**
     * @param chunkClass class type to operate upon
     * @param raf random access file object
     * @param regionXZ XZ coords of the region data - usually as extracted from the file name such as [1 -2] from r.1.-2.mca
     * @param mode one of the modes accepted by RandomAccessFile ("r", "rw" are the most common). This mode can be
     *             more restrictive than the mode the RandomAccessFile was actually opened with. Specifying a mode of
     *             "r" will cause all calls to {@link #write}, {@link #removeChunk}, and {@link #optimizeFile()} to
     *             fail with an exception and all calls to {@link #flush()} to silently do nothing.
     */
    public RandomAccessMcaFile(Class<T> chunkClass, RandomAccessFile raf, IntPointXZ regionXZ, String mode) {
        this.chunkClass = ArgValidator.requireValue(chunkClass, "chunkClass");
        this.raf = ArgValidator.requireValue(raf, "RandomAccessFile");
        this.regionXZ = ArgValidator.requireValue(regionXZ, "regionXZ");
        this.regionChunkOffsetXZ = regionXZ.transformRegionToChunk();
        this.regionBounds = new RegionBoundingRectangle(regionXZ.getX(), regionXZ.getZ());
        this.isReadOnly = !mode.startsWith("rw");
    }

    /**
     * @param chunkClass class type to operate upon
     * @param file Mca file to open. The file name must follow the standard naming of "r.X.Z.mca".
     * @param mode one of the modes accepted by RandomAccessFile ("r", "rw" are the most common). This mode can be
     *             more restrictive than the mode the RandomAccessFile was actually opened with. Specifying a mode of
     *             "r" will cause all calls to {@link #write} and {@link #optimizeFile()} to fail with an exception
     *             and all calls to {@link #flush()} to silently do nothing.
     */
    public RandomAccessMcaFile(Class<T> chunkClass, File file, String mode) throws IOException {
        this(chunkClass, new RandomAccessFile(file, mode), McaFileHelpers.regionXZFromFileName(file.getName()), mode);
    }

    /**
     * @param chunkClass class type to operate upon
     * @param file Mca file to open. The file name must follow the standard naming of "r.X.Z.mca".
     * @param mode one of the modes accepted by RandomAccessFile ("r", "rw" are the most common). This mode can be
     *             more restrictive than the mode the RandomAccessFile was actually opened with. Specifying a mode of
     *             "r" will cause all calls to {@link #write} and {@link #optimizeFile()} to fail with an exception
     *             and all calls to {@link #flush()} to silently do nothing.
     */
    public RandomAccessMcaFile(Class<T> chunkClass, String file, String mode) throws IOException {
        this(chunkClass, new File(file), mode);
    }

    /**
     * @param chunkClass class type to operate upon
     * @param path Mca file to open. The file name must follow the standard naming of "r.X.Z.mca".
     * @param mode one of the modes accepted by RandomAccessFile ("r", "rw" are the most common). This mode can be
     *             more restrictive than the mode the RandomAccessFile was actually opened with. Specifying a mode of
     *             "r" will cause all calls to {@link #write} and {@link #optimizeFile()} to fail with an exception
     *             and all calls to {@link #flush()} to silently do nothing.
     */
    public RandomAccessMcaFile(Class<T> chunkClass, Path path, String mode) throws IOException {
        this(chunkClass, path.toFile(), mode);
    }

    /** True if opened in read only mode. */
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * @return XZ coords of the region, in region coordinates.
     */
    public IntPointXZ getRegionXZ() {
        return regionXZ;
    }

    /** LoadFlags which are passed to the chunk deserialization method. */
    public long getLoadFlags() {
        return loadFlags;
    }

    /** LoadFlags which are passed to the chunk deserialization method. */
    public RandomAccessMcaFile<T> setLoadFlags(long loadFlags) {
        this.loadFlags = loadFlags;
        return this;
    }

    /**
     * Automatically call {@link #optimizeFile()} when {@link #close()} is called.
     * <p>When set the mca file will be auto optimized (compacted) when {@link #close()} is called IFF any chunks
     * were written or removed.</p>
     */
    public boolean isAutoOptimizeOnClose() {
        return autoOptimizeOnClose;
    }

    /**
     * Automatically call {@link #optimizeFile()} when {@link #close()} is called.
     * <p>When set the mca file will be auto optimized (compacted) when {@link #close()} is called IFF any chunks
     * were written or removed.</p>
     */
    public RandomAccessMcaFile<T> setAutoOptimizeOnClose(boolean autoOptimizeOnClose) {
        this.autoOptimizeOnClose = autoOptimizeOnClose;
        return this;
    }

    /**
     * When set calls to {@link #write} will automatically call {@link ChunkBase#updateHandle()} before writing to
     * disk. If unset the library user is responsible for ensuring that the chunk handel is updated prior to calling
     * {@link #write}. Note that mca last updated timestamp data is NOT stored in the chunk handle, it is part of the
     * mca file header.
     */
    public boolean isAutoUpdateHandelOnWrite() {
        return autoUpdateHandelOnWrite;
    }


    /**
     * When set calls to {@link #write} will automatically call {@link ChunkBase#updateHandle()} before writing to
     * disk. If unset the library user is responsible for ensuring that the chunk handel is updated prior to calling
     * {@link #write}. Note that mca last updated timestamp data is NOT stored in the chunk handle, it is part of the
     * mca file header.
     */
    public RandomAccessMcaFile<T> setAutoUpdateHandelOnWrite(boolean autoUpdateHandelOnWrite) {
        this.autoUpdateHandelOnWrite = autoUpdateHandelOnWrite;
        return this;
    }

    /**
     * When set any call to {@link #write} will set the given chunks last modified timestamp by calling
     * {@link ChunkBase#setLastMCAUpdate(int)} with the current system timestamp.
     * <p>Note that if the chunk passed to write currently has no timestamp, one will be set irregardless of
     * this setting.</p>
     */
    public boolean isAlwaysUpdateChunkLastUpdatedTimestamp() {
        return alwaysUpdateChunkLastUpdatedTimestamp;
    }

    /**
     * When set any call to {@link #write} will set the given chunks last modified timestamp by calling
     * {@link ChunkBase#setLastMCAUpdate(int)} with the current system timestamp.
     * <p>Note that if the chunk passed to write currently has no timestamp, one will be set irregardless of
     * this setting.</p>
     */
    public RandomAccessMcaFile<T> setAlwaysUpdateChunkLastUpdatedTimestamp(boolean alwaysUpdateChunkLastUpdatedTimestamp) {
        this.alwaysUpdateChunkLastUpdatedTimestamp = alwaysUpdateChunkLastUpdatedTimestamp;
        return this;
    }

    /**
     * @return A diagnostic information string.
     * @see #chunkSectorTableToString()
     */
    @Override
    public String toString() {
        return String.format(
                "region %s; %s; %s; initialized %s; finalized %s; chunks[written %d; read %d]; " +
                        "timing[init %s; read %s; serialize %s; write %s; optimize %s; flush %s]; " +
                        "settings[flags %s; auto-optimize %s; auto-update-handel %s; always-update-timestamp %s]; " +
                        "sector-manager[%s]",
                regionXZ,
                regionBounds.asChunkBounds(),
                regionBounds.asBlockBounds(),
                fileInitialized,
                fileFinalized,
                chunksWritten,
                chunksRead,
                fileInitialized ? fileInitializationStopwatch : "n/a",
                fileInitialized ? totalReadStopwatch : "n/a",
                fileInitialized ? chunkSerializationStopwatch : "n/a",
                fileInitialized ? totalWriteStopwatch.subtract(chunkSerializationStopwatch) : "n/a",
                fileInitialized ? fileOptimizationStopwatch : "n/a",
                fileInitialized ? fileFlushStopwatch : "n/a",
                LoadFlags.toHexString(loadFlags),
                isAutoOptimizeOnClose(),
                isAutoOptimizeOnClose(),
                isAlwaysUpdateChunkLastUpdatedTimestamp(),
                sectorManager);
    }

    /**
     * Creates a 32x32 textual table of chunk sector information. Useful for debugging this library as well as
     * user code. Includes free-sector information.
     */
    public String chunkSectorTableToString() throws IOException {
        ensureFileInitialized();
        StringBuilder sb = new StringBuilder();
        // defaults set minimum lengths
        int maxOffsetStrLen = 2;
        int maxSizeStrLen = 1;
        for (int i = 0; i < 1024; i++) {
            SectorManager.SectorBlock sectorBlock = SectorManager.SectorBlock.unpack(chunkSectors[i]);
            int offsetStrLen = String.format("%X", sectorBlock.start).length();
            int sizeStrLen = String.format("%X", sectorBlock.size).length();
            if (maxOffsetStrLen < offsetStrLen) maxOffsetStrLen = offsetStrLen;
            if (maxSizeStrLen < sizeStrLen) maxSizeStrLen = sizeStrLen;
        }
        String sectorFormat = "%0" + maxOffsetStrLen + "X+%0" + maxSizeStrLen + "X ";
        String noSector = "-".repeat(maxOffsetStrLen + maxSizeStrLen + 1) + " ";
        String headerFormat = "%" + (maxOffsetStrLen + maxSizeStrLen) + "d  ";

        sb.append(String.format(
                " region %s; %s; %s\n",
                regionXZ,
                regionBounds.asChunkBounds(),
                regionBounds.asBlockBounds()));
        sb.append(" Z\\X");
        for (int i = 0; i < 32; i++) {
            if (i != 0 && i % 8 == 0) {
                sb.append(' ');
            }
            sb.append(String.format(headerFormat, i));
        }
        sb.append('\n');

        for (int i = 0; i < 1024; i++) {
            if (i % 32 == 0) {
                if (i > 0) sb.append('\n');
                sb.append(String.format("%2d: ", i / 32));
            } else if (i % 8 == 0) {
                sb.append(' ');
            }
            SectorManager.SectorBlock sectorBlock = SectorManager.SectorBlock.unpack(chunkSectors[i]);
            if (sectorBlock.size == 0) {
                sb.append(noSector);
            } else {
                sb.append(sectorBlock.toString(sectorFormat));
            }
        }
        sb.append('\n');
        sb.append(sectorManager);
        return sb.toString();
    }

    /**
     * Forces initialization of chunk data. This class lazily loads the mca file header tables, touching the
     * instance triggers this loading and is a no-op if already loaded. This method is useful mostly for debugging
     * or for ensuring all on-close automatic actions are taken without needing to perform a chunk read/write.
     * @return self for chaining.
     */
    public RandomAccessMcaFile<T> touch() throws IOException {
        ensureFileInitialized();
        return this;
    }

    /** Causes the mca file header tables to be read if they have not yet been read. */
    protected void ensureFileInitialized() throws IOException {
        if (fileFinalized) throw new IOException("File closed!");
        if (!fileInitialized) {
            try (Stopwatch.LapToken lap = fileInitializationStopwatch.startLap()) {
                raf.seek(0);
                final byte[] buffer = new byte[4096];
                if (raf.length() >= 4096 * 2) {  // existing file
                    ByteBuffer bb = ByteBuffer.wrap(buffer);
                    raf.read(buffer);
                    bb.position(0);
                    bb.asIntBuffer().get(chunkSectors);
                    raf.read(buffer);
                    bb.position(0);
                    bb.asIntBuffer().get(chunkTimestamps);
                } else if (!isReadOnly) {  // new file, or it existed but was empty - MC seems to do that
                    // zero out the chunk sector and timestamp tables
                    raf.setLength(0);
                    raf.write(buffer);
                    raf.write(buffer);
                }
                sectorManager.sync(chunkSectors);
                fileInitialized = true;
            }
        }
    }

    /**
     * Finalizes and closes this mca file. It's safe to call this method multiple times.
     * @see #setAutoOptimizeOnClose(boolean)
     * @see #optimizeFile()
     */
    @Override
    public void close() throws IOException {
        if (fileFinalized) return;
        try {
            if (!isReadOnly && fileInitialized) {
                if (isAutoOptimizeOnClose())
                    optimizeFile();
                flush();
            }
        } finally {
            raf.close();
            sectorManager.freeSectors.clear();
            fileFinalized = true;
        }
    }

    /**
     * Forces immediate write of the chunk index and timestamp tables (file header information).
     * @see #touch()
     */
    public void flush() throws IOException {
        if (!fileInitialized || isReadOnly)
            return;
        if (fileFinalized)
            throw new IOException("File closed!");
        try (Stopwatch.LapToken lap = fileFlushStopwatch.startLap()) {
            raf.seek(0);
            ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(chunkSectors);
            raf.write(byteBuffer.array());
            intBuffer.clear();
            intBuffer.put(chunkTimestamps);
            raf.write(byteBuffer.array());
        }
    }

    /**
     * Compacts the chunk data in the mca file by removing unused file sectors. This class will attempt to reuse any
     * free space within the chunk data as you write chunks, there's no need to call this method except before/during
     * close.
     * <p>If you called {@link #removeChunk(int)} this is the method that will actually remove that chunk data from the
     * file and cause the file to shrink in size.
     * Note that there are other actions which can introduce unused sectors in the mca file - for example if you
     * read, modify, and write a chunk in such a way that it takes more or less sectors to store this will also
     * introduce unused space.</p>
     * @return Number of unused bytes that were removed from the file. The file is now this much smaller.
     * @see #setAutoOptimizeOnClose(boolean)
     */
    public int optimizeFile() throws IOException {
        ensureFileInitialized();
        if (isReadOnly)
            throw new IOException("File was opened in read-only mode.");
        int bytesRemoved = 0;
        try (Stopwatch.LapToken lap = fileOptimizationStopwatch.startLap()) {
            bytesRemoved = sectorManager.optimizeFile(raf, chunkSectors);
        }
        return bytesRemoved;
    }

    /**
     * Marks the specified chunk for removal and makes its file sectors available for saving other chunks into.
     * <p>Does not actually erase the chunk data in the mca file during this call - this is a very lightweight call.</p>
     * @return True if the chunk previously existed.
     * @see #optimizeFile()
     * @see #setAutoOptimizeOnClose(boolean)
     */
    public boolean removeChunk(int chunkIndex) throws IOException {
        if (isReadOnly)
            throw new IOException("File was opened in read-only mode.");
        if (hasChunk(chunkIndex)) {
            isDirty = true;
            sectorManager.release(SectorManager.SectorBlock.unpack(chunkSectors[chunkIndex]));
            chunkSectors[chunkIndex] = 0;
            chunkTimestamps[chunkIndex] = 0;
            return true;
        }
        return false;
    }

    /**
     * Marks the specified chunk for removal and makes its file sectors available for saving other chunks into.
     * <p>Does not actually erase the chunk data in the mca file during this call - this is a very lightweight call.</p>
     * @return True if the chunk previously existed.
     * @see #optimizeFile()
     * @see #setAutoOptimizeOnClose(boolean)
     */
    public boolean removeChunkRelative(int x, int z) throws IOException {
        if (x < 0 || x >= 32 || z < 0 || z >= 32)
            throw new IndexOutOfBoundsException();
        return removeChunk(McaRegionFile.getChunkIndex(x, z));
    }

    /**
     * Marks the specified chunk for removal and makes its file sectors available for saving other chunks into.
     * <p>Does not actually erase the chunk data in the mca file during this call - this is a very lightweight call.</p>
     * @return True if the chunk previously existed.
     * @see #optimizeFile()
     * @see #setAutoOptimizeOnClose(boolean)
     */
    public boolean removeChunkRelative(IntPointXZ xz) throws IOException {
        return removeChunkRelative(xz.getX(), xz.getZ());
    }

    /**
     * Marks the specified chunk for removal and makes its file sectors available for saving other chunks into.
     * <p>Does not actually erase the chunk data in the mca file during this call - this is a very lightweight call.</p>
     * @return True if the chunk previously existed.
     * @see #optimizeFile()
     * @see #setAutoOptimizeOnClose(boolean)
     */
    public boolean removeChunkAbsolute(int x, int z) throws IOException {
        return this.regionBounds.containsChunk(x, z) && removeChunk(McaRegionFile.getChunkIndex(x, z));
    }

    /**
     * Marks the specified chunk for removal and makes its file sectors available for saving other chunks into.
     * <p>Does not actually erase the chunk data in the mca file during this call - this is a very lightweight call.</p>
     * @return True if the chunk previously existed.
     * @see #optimizeFile()
     * @see #setAutoOptimizeOnClose(boolean)
     */
    public boolean removeChunkAbsolute(IntPointXZ xz) throws IOException {
        return removeChunkAbsolute(xz.getX(), xz.getZ());
    }


    /** @return True if the chunk exists. */
    public boolean hasChunk(int chunkIndex) throws IOException {
        ensureFileInitialized();
        return (chunkSectors[chunkIndex] & 0xFF) > 0;
    }

    /** @return True if the chunk exists. */
    public boolean hasChunkRelative(int x, int z) throws IOException {
        if (x < 0 || x >= 32 || z < 0 || z >= 32)
            throw new IndexOutOfBoundsException();
        return hasChunk(McaRegionFile.getChunkIndex(x, z));
    }

    /** @return True if the chunk exists. */
    public boolean hasChunkRelative(IntPointXZ xz) throws IOException {
        return hasChunkRelative(xz.getX(), xz.getZ());
    }

    /** @return True if the chunk exists. */
    public boolean hasChunkAbsolute(int x, int z) throws IOException {
        return this.regionBounds.containsChunk(x, z) && hasChunk(McaRegionFile.getChunkIndex(x, z));
    }

    /** @return True if the chunk exists. */
    public boolean hasChunkAbsolute(IntPointXZ xz) throws IOException {
        return hasChunkAbsolute(xz.getX(), xz.getZ());
    }


    /** @return Chunk timestamp, in epoch seconds, if chunk exists else -1. */
    public int getChunkTimestamp(int chunkIndex) throws IOException {
        ensureFileInitialized();
        return hasChunk(chunkIndex) ? chunkTimestamps[chunkIndex] : -1;
    }

    /** @return Chunk timestamp, in epoch seconds, if chunk exists else -1. */
    public int getChunkTimestampRelative(int x, int z) throws IOException {
        if (x < 0 || x >= 32 || z < 0 || z >= 32)
            throw new IndexOutOfBoundsException();
        return getChunkTimestamp(McaRegionFile.getChunkIndex(x, z));
    }

    /** @return Chunk timestamp, in epoch seconds, if chunk exists else -1. */
    public int getChunkTimestampRelative(IntPointXZ xz) throws IOException {
        return getChunkTimestampRelative(xz.getX(), xz.getZ());
    }

    /** @return Chunk timestamp, in epoch seconds, if chunk exists else -1. */
    public int getChunkTimestampAbsolute(int x, int z) throws IOException {
        return this.regionBounds.containsChunk(x, z) ? getChunkTimestamp(McaRegionFile.getChunkIndex(x, z)) : -1;
    }

    /** @return Chunk timestamp, in epoch seconds, if chunk exists else -1. */
    public int getChunkTimestampAbsolute(IntPointXZ xz) throws IOException {
        return getChunkTimestampAbsolute(xz.getX(), xz.getZ());
    }


    /**
     * Reads the specified chunk if it exists.
     * @return The chunk if it exists, else null.
     */
    public T read(int chunkIndex) throws IOException {
        return read(chunkIndex, loadFlags);
    }

    /**
     * Reads the specified chunk if it exists.
     * @return The chunk if it exists, else null.
     */
    public T read(int chunkIndex, long loadFlags) throws IOException {
        if (chunkIndex < 0 || chunkIndex >= 1024)
            throw new IndexOutOfBoundsException();
        ensureFileInitialized();
        try (var lap = totalReadStopwatch.startLap()) {
            int sectorOffset = chunkSectors[chunkIndex] >>> 8;
            int sectorSize = chunkSectors[chunkIndex] & 0xFF;
            if (sectorSize == 0) return null;
            if (raf.length() < (sectorOffset + sectorSize) * 4096L) {
                throw new EOFException();
            }
            raf.seek(sectorOffset * 4096L);  // +2 for the file header
            int chunkByteSize = raf.readInt();
            if (chunkByteSize > (sectorSize * 4096) - 4) {
                throw new CorruptMcaFileException(String.format(
                        "MCA file header sector size %d (%d bytes) for chunk %04d (at 0x%X) is too small to hold %d bytes!",
                        sectorSize, sectorSize * 4096, chunkIndex, sectorOffset * 4096L, chunkByteSize));
            }

            T chunk;
            try {
                chunk = chunkClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                // TODO should wrap with a custom chunk creation exception...
                // given that this error is something exclusively under the control of the library user I'm OK(ish) with this hacky wrap and throw
                throw new RuntimeException(ex);
            }
            IntPointXZ chunkXZ = McaRegionFile.getRelativeChunkXZ(chunkIndex).add(regionChunkOffsetXZ);
            chunksRead ++;
            chunk.deserialize(raf, loadFlags, chunkTimestamps[chunkIndex], chunkXZ.getX(), chunkXZ.getZ());
            return chunk;
        }
    }

    /**
     * Reads the specified chunk if it exists.
     * @return The chunk if it exists, else null.
     */
    public T readRelative(int x, int z) throws IOException {
        if (x < 0 || x >= 32 || z < 0 || z >= 32)
            throw new IndexOutOfBoundsException();
        return read(McaFileBase.getChunkIndex(x, z));
    }

    /**
     * Reads the specified chunk if it exists.
     * @return The chunk if it exists, else null.
     */
    public T readRelative(IntPointXZ xz) throws IOException {
        return readRelative(xz.getX(), xz.getZ());
    }

    /**
     * Reads the specified chunk if it exists.
     * @return The chunk if it exists, else null.
     */
    public T readAbsolute(int x, int z) throws IOException {
        if (!this.regionBounds.containsChunk(x, z))
            throw new IndexOutOfBoundsException();
        return read(McaFileBase.getChunkIndex(x, z));
    }

    /**
     * Reads the specified chunk if it exists.
     * @return The chunk if it exists, else null.
     */
    public T readAbsolute(IntPointXZ xz) throws IOException {
        return readAbsolute(xz.getX(), xz.getZ());
    }

    /**
     * Writes the given chunks.
     * @param chunks not null and all chunks must exist within bounds of this region file.
     * @see #removeChunk
     */
    @SafeVarargs
    public final void write(T... chunks) throws IOException {
        for (T chunk : chunks) {
            write(chunk);
        }
    }

    /**
     * Writes the given chunk.
     * @param chunk not null and chunk must exist within bounds of this region file.
     * @see #removeChunk
     */
    public void write(T chunk) throws IOException {
        ArgValidator.requireValue(chunk);
        if (isReadOnly)
            throw new IOException("File was opened in read-only mode.");
        if (chunk.getChunkX() == ChunkBase.NO_CHUNK_COORD_SENTINEL || chunk.getChunkZ() == ChunkBase.NO_CHUNK_COORD_SENTINEL) {
            throw new IllegalArgumentException("Chunk XZ must be set!");
        }
        if (!this.regionBounds.containsChunk(chunk.getChunkX(), chunk.getChunkZ()))
            throw new IndexOutOfBoundsException(String.format(
                    "ChunkXZ(%s) does not exist within regionXZ(%s) inclusive bounds %s!",
                    chunk.getChunkXZ(),
                    regionXZ,
                    regionBounds.asChunkBounds()));
        ensureFileInitialized();
        isDirty = true;
        if (isAlwaysUpdateChunkLastUpdatedTimestamp() || chunk.getLastMCAUpdate() <= 0) {
            chunk.setLastMCAUpdate((int) (System.currentTimeMillis() / 1000));
        }

        try (Stopwatch.LapToken lap1 = totalWriteStopwatch.startLap()) {
            final int index = chunk.getIndex();
            final int oldSectorOffset = chunkSectors[index] >>> 8;
            final int oldSectorSize = chunkSectors[index] & 0xFF;
            ByteArrayOutputStream baos;
            SectorManager.SectorBlock writeToSector;
            int totalBytes;
            final int newSectorSize;
            chunksWritten ++;

            try (Stopwatch.LapToken lap2 = chunkSerializationStopwatch.startLap()) {
                baos = new ByteArrayOutputStream(Math.min(2, oldSectorSize) * 4096);
                new BinaryNbtSerializer(CompressionType.ZLIB).toStream(
                        new NamedTag(null, isAutoUpdateHandelOnWrite() ? chunk.updateHandle() : chunk.getHandle()), baos);
            }
            // Note 'totalBytes' is count 4 larger than the value written at the chunk sector offset because it includes the byte size data too
            totalBytes = baos.size() + 4 /*size*/ + 1 /*compression sig*/;
            newSectorSize = (totalBytes >> 12) + (totalBytes % 4096 == 0 ? 0 : 1);
            if (newSectorSize > 255) throw new IOException("Chunk " + chunk.getChunkXZ() + " to large! 1MB maximum");

            if (oldSectorSize == 0) {  // chunk has never been written to file
                writeToSector = sectorManager.allocate(newSectorSize);
            } else if (newSectorSize == oldSectorSize) {  // new chunk data fits in the old slot like a glove
                writeToSector = new SectorManager.SectorBlock(oldSectorOffset, newSectorSize);
            } else if (newSectorSize < oldSectorSize) {  // new chunk data still fits but there's extra room now
                writeToSector = new SectorManager.SectorBlock(oldSectorOffset, newSectorSize);
                sectorManager.release(oldSectorOffset + newSectorSize, oldSectorSize - newSectorSize);
            } else {  // new chunk data is too large to fit in the old slot so alloc a new one
                writeToSector = sectorManager.allocate(newSectorSize);
                sectorManager.release(oldSectorOffset, oldSectorSize);
            }
            writeToSector.seekTo(raf);
            raf.writeInt(totalBytes - 4);  // don't count the int we are writing here in the byte size
            raf.write(CompressionType.ZLIB.getID());
            raf.write(baos.toByteArray());
            chunkSectors[index] = writeToSector.pack();
            chunkTimestamps[index] = chunk.getLastMCAUpdate();

            long roundedEos = writeToSector.end() * 4096L;
            while (roundedEos > raf.getFilePointer()) {
                int gap = (int) Math.min(roundedEos - raf.getFilePointer(), ZERO_FILL_BUFFER.length);
                raf.write(ZERO_FILL_BUFFER, 0, gap);
            }
            if (raf.getFilePointer() % 4096 != 0)
                throw new IllegalStateException();
        }
    }

    /**
     * @return the chunk XZ coords of the minimum chunk (north-west corner) in this region.
     */
    public IntPointXZ getRegionChunkOffsetXZ() {
        return regionChunkOffsetXZ;
    }

    // TODO: find a common code location for these helpers
    public IntPointXZ indexToRelativeXZ(int index) {
        if (index < 0 || index >= 1024)
            throw new IndexOutOfBoundsException();
        return new IntPointXZ(index & 0x1F, (index >> 5) & 0x1F);
    }

    public IntPointXZ indexToAbsoluteXZ(int index) {
        return regionChunkOffsetXZ.add(indexToRelativeXZ(index));
    }

    @Override
    public Iterator<T> iterator() {
        return chunkIterator();
    }

    // This class assumes known usage patterns and does not defend against doing stupid things.
    // Making this class safe for general use would require tracking allocs to make sure releases
    // are valid and complete/correct - but that would add unneeded bloat and memory use for this
    // application.
    static class SectorManager {

        static class SectorBlock {
            int start;
            int size;
            SectorBlock(SectorBlock other) {
                this.start = other.start;
                this.size = other.size;
            }
            SectorBlock(int start, int size) {
                this.start = start;
                this.size = size;
            }
            int end() {
                return start + size;
            }
            void expandToInclude(int value) {
                if (value < start) {
                    size += start - value;
                    start = value;
                } else if (value > end()) {
                    size = value - start;
                }
            }
            boolean merge(SectorBlock other) {
                final int thisEnd = this.end();
                final int otherEnd = other.end();
                if (this.start > otherEnd || other.start > thisEnd)
                    return false;
                this.expandToInclude(other.start);
                this.expandToInclude(otherEnd);
                return true;
            }
            public int pack() throws IOException {
                if (size < 0 || size > 255)
                    throw new IOException("Invalid chunk data sector size!");
                return start << 8 | size;
            }
            public static SectorBlock unpack(int packed) {
                return new SectorBlock(packed >> 8, packed & 0xFF);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof SectorBlock other)) {
                    return false;
                }
                return other.start == this.start && other.size == this.size;
            }

            @Override
            public String toString() {
                return toString("0x%X+%X");
            }

            /** @param format must have two %d placeholders, first is start, second is size */
            public String toString(String format) {
                return String.format(format, start, size);
            }

            public void seekTo(RandomAccessFile raf) throws IOException {
                raf.seek(start * 4096L);
            }
        }
        final LinkedList<SectorBlock> freeSectors = new LinkedList<>();
        int appendAtSector = 2;

        void sync(int[] sectorTable) throws CorruptMcaFileException {
            if (sectorTable.length != 1024) throw new IllegalArgumentException();
            freeSectors.clear();
            appendAtSector = 2;
            List<SectorBlock> usedSectorBlocks = new ArrayList<>(1024);
            for (int i = 0; i < 1024; i++) {
                int sectorStart = sectorTable[i] >> 8;
                int sectorSize = sectorTable[i] & 0xFF;
                if (sectorSize > 0) {
                    if (sectorStart < 2)
                        throw new CorruptMcaFileException();
                    usedSectorBlocks.add(new SectorBlock(sectorStart, sectorSize));
                }
            }
            if (!usedSectorBlocks.isEmpty()) {
                usedSectorBlocks.sort(Comparator.comparingInt(a -> a.start));
                SectorBlock previous = usedSectorBlocks.get(0);
                if (previous.start > 2) {
                    freeSectors.add(new SectorBlock(2, previous.start - 2));
                }
                for (int i = 1; i < usedSectorBlocks.size(); i++) {
                    SectorBlock current = usedSectorBlocks.get(i);
                    if (previous.end() != current.start) {
                        freeSectors.add(new SectorBlock(previous.end(), current.start - previous.end()));
                    }
                    previous = current;
                }
                appendAtSector = Math.max(appendAtSector, previous.end());
            }
        }

        SectorBlock allocate(int requestedSectorSize) {
            // does not attempt to find a best-fit, simply the first fit.
            ListIterator<SectorBlock> iter = freeSectors.listIterator();
            SectorBlock found = null;
            while (iter.hasNext()) {
                SectorBlock sb = iter.next();
                if (sb.size == requestedSectorSize) {
                    found = sb;
                    iter.remove();
                    break;
                } else if (sb.size > requestedSectorSize) {
                    found = new SectorBlock(sb.start, requestedSectorSize);
                    sb.size -= requestedSectorSize;
                    sb.start += requestedSectorSize;
                    break;
                }
            }
            if (found == null) {
                found = new SectorBlock(appendAtSector, requestedSectorSize);
                appendAtSector += requestedSectorSize;
            }
            return found;
        }

        void release(int start, int size) {
            release(new SectorBlock(start, size));
        }

        public void release(SectorBlock sectorBlock) {
            if (sectorBlock.size == 0) return;
            ListIterator<SectorBlock> iter = freeSectors.listIterator();
            boolean released = false;
            while (iter.hasNext()) {
                SectorBlock sb = iter.next();
                if (sb.merge(sectorBlock)) {
                    released = true;
                    break;
                }
                if (sectorBlock.end() < sb.start) {
                    iter.previous();
                    iter.add(new SectorBlock(sectorBlock));
                    released = true;
                    break;
                }
            }
            if (!released) {
                freeSectors.addLast(new SectorBlock(sectorBlock));
            }
            if (freeSectors.getLast().end() == appendAtSector) {
                SectorBlock sb = freeSectors.removeLast();
                appendAtSector = sb.start;
            }
        }

        /** @return Number of unused bytes that were removed from the file. The file is now this much smaller. */
        public int optimizeFile(RandomAccessFile raf, int[] chunkSectors) throws IOException {
            if (freeSectors.isEmpty()) {
                return truncate(raf);
            }
            List<SectorBlock> sectorsToMove = new ArrayList<>(1024);
            SectorBlock[] sectors = new SectorBlock[1024];
            final int firstFreeSector = freeSectors.getFirst().start;
            int largestChunkInSectors = 0;
            for (int i = 0; i < 1024; i++) {
                SectorBlock sectorBlock = SectorBlock.unpack(chunkSectors[i]);
                sectors[i] = sectorBlock;
                if (sectorBlock.size > 0) {
                    if (sectorBlock.start < 2)
                        throw new CorruptMcaFileException();
                    if (sectorBlock.start > firstFreeSector) {
                        sectorsToMove.add(sectorBlock);
                        if (largestChunkInSectors < sectorBlock.size)
                            largestChunkInSectors = sectorBlock.size;
                    }
                }
            }
            if (largestChunkInSectors == 0) {
                appendAtSector = 2;
                return truncate(raf);
            }

            // by here all of these blocks have to be moved
            // sort them in ascending location order so the relocation logic can be simplified
            sectorsToMove.sort(Comparator.comparingInt(a -> a.start));

            int nextSectorStart = firstFreeSector;
            // This will never be GT 1MB and is usually LE 16KB
            byte[] buffer = new byte[largestChunkInSectors * 4096];
            for (SectorBlock sb : sectorsToMove) {
                sb.seekTo(raf);
                int sectorSizeBytes = sb.size * 4096;
                int read = raf.read(buffer, 0, sectorSizeBytes);
                if (read < 0)
                    throw new EOFException();
                if (read != sectorSizeBytes)
                    throw new IOException("Failed to read the required number of bytes!");
                sb.start = nextSectorStart;
                sb.seekTo(raf);
                raf.write(buffer, 0, sectorSizeBytes);
                nextSectorStart += sb.size;
            }

            // refresh the chunk sectors table
            for (int i = 0; i < 1024; i++) {
                chunkSectors[i] = sectors[i].pack();
            }

            // sync sector manager state
            freeSectors.clear();
            appendAtSector = sectorsToMove.get(sectorsToMove.size() - 1).end();
            return truncate(raf);
        }

        /** @return Number of unused bytes that were removed from the file. The file is now this much smaller. */
        int truncate(RandomAccessFile raf) throws IOException {
            final long oldLength = raf.length();
            final long newLength = appendAtSector * 4096L;
            if (newLength > raf.length())
                throw new IOException("File is already smaller than " + newLength);
            raf.setLength(newLength);
            return (int) (oldLength - newLength);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("eof-sector ");
            sb.append(String.format("0x%X", appendAtSector));
            sb.append("; free-sectors");
            if (!freeSectors.isEmpty()) {
                sb.append("(count ").append(freeSectors.size());
                sb.append("; sum ").append(freeSectors.stream().mapToInt(s -> s.size).sum());
                sb.append(')');
            }
            sb.append('[');
            boolean first = true;
            for (SectorBlock fs : freeSectors) {
                if (!first) sb.append(", ");
                else first = false;
                sb.append(fs);
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public ChunkIterator<T> chunkIterator() {
        return new ChunkIter<>(this, loadFlags);
    }

    public ChunkIterator<T> chunkIterator(long loadFlags) {
        return new ChunkIter<>(this, loadFlags);
    }

    protected static class ChunkIter<T extends ChunkBase> implements ChunkIterator<T> {
        private final RandomAccessMcaFile<T> ramf;
        private final long loadFlags;
        private int nextIndex = 0;

        public ChunkIter(RandomAccessMcaFile<T> ramf, long loadFlags) {
            this.ramf = ramf;
            this.loadFlags = loadFlags;
        }

        @Override
        public void set(T chunk) {
            try {
                ramf.write(chunk);
            } catch (IOException ex) {
                throw new SilentIOException(ex);
            }
        }

        @Override
        public int currentIndex() {
            if (nextIndex == 0) throw new NoSuchElementException();
            return nextIndex - 1;
        }

        @Override
        public IntPointXZ currentAbsoluteXZ() {
            return ramf.indexToAbsoluteXZ(nextIndex - 1);
        }

        @Override
        public int currentAbsoluteX() {
            return currentAbsoluteXZ().getX();
        }

        @Override
        public int currentAbsoluteZ() {
            return currentAbsoluteXZ().getZ();
        }

        @Override
        public boolean hasNext() {
            return nextIndex < 1024;
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            try {
                return ramf.read(nextIndex++, loadFlags);
            } catch (IOException ex) {
                throw new SilentIOException(ex);
            }
        }
    }
}
