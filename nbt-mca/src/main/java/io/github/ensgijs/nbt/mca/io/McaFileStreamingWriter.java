package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.io.CompressionType;
import io.github.ensgijs.nbt.mca.ChunkBase;
import io.github.ensgijs.nbt.util.ArgValidator;
import io.github.ensgijs.nbt.util.Stopwatch;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Path;

/**
 * Provides a streaming data sink for writing a region file. Chunks can be written in any order.
 * Attempting to write a chunk (XZ) that has already been written will throw {@link IOException}.
 * <p>You must remember to call {@link McaFileStreamingWriter#close()}! Close writes the file
 * index and without this index the region will appear to contain no chunk data.</p>
 * @see McaFileHelpers
 * @see McaFileChunkIterator
 */
public class McaFileStreamingWriter implements Closeable {
    private static final byte[] ZERO_FILL_BUFFER = new byte[4096];
    private final int[] chunkSectors = new int[1024];
    private final int[] chunkTimestamps = new int[1024];
    private final RandomAccessFile raf;
    private final Stopwatch fileInitializationStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch totalWriteStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch chunkSerializationStopwatch = Stopwatch.createUnstarted();
    private final Stopwatch fileCloseStopwatch = Stopwatch.createUnstarted();
    private int chunksWritten = 0;
    private boolean fileInitialized = false;
    private boolean fileFinalized = false;

    public McaFileStreamingWriter(RandomAccessFile raf) {
        ArgValidator.requireValue(raf);
        this.raf = raf;
    }
    public McaFileStreamingWriter(File file) throws IOException {
        this(new RandomAccessFile(file, "rw"));
    }
    public McaFileStreamingWriter(String file) throws IOException {
        this(new File(file));
    }
    public McaFileStreamingWriter(Path path) throws IOException {
        this(path.toFile());
    }

    public void write(ChunkBase chunk) throws IOException {
        ArgValidator.requireValue(chunk);
        if (!fileInitialized) {
            try (Stopwatch.LapToken lap = fileInitializationStopwatch.startLap()) {
                raf.setLength(0);
                raf.seek(0);
                // zero out the chunk sector and timestamp tables
                raf.write(ZERO_FILL_BUFFER);
                raf.write(ZERO_FILL_BUFFER);
                fileInitialized = true;
            }
        }
        try (Stopwatch.LapToken lap1 = totalWriteStopwatch.startLap()) {
            if (chunk.getChunkX() == ChunkBase.NO_CHUNK_COORD_SENTINEL || chunk.getChunkZ() == ChunkBase.NO_CHUNK_COORD_SENTINEL) {
                throw new IllegalArgumentException("Chunk XZ must be set!");
            }
            final int index = chunk.getIndex();
            if (chunkSectors[index] != 0)
                throw new IOException("Chunk " + chunk.getChunkXZ() + " (index: " + index + ") has already been written!");

            if (raf.getFilePointer() % 4096 != 0)
                throw new IllegalStateException();
            final int startSector = (int) (raf.getFilePointer() >> 12);

            int bytesWritten;
            try (Stopwatch.LapToken lap2 = chunkSerializationStopwatch.startLap()) {
                bytesWritten = chunk.serialize(raf, chunk.getChunkX(), chunk.getChunkZ(), CompressionType.ZLIB, true);
            }

            // compute the count of 4kb sectors the chunk data occupies
            int sectors = (bytesWritten >> 12) + (bytesWritten % 4096 == 0 ? 0 : 1);
            if (sectors > 255) throw new IOException("Chunk " + chunk.getChunkXZ() + " to large! 1MB maximum");
            long roundedEof = ((long) (startSector + sectors) << 12);
            while (roundedEof > raf.getFilePointer()) {
                int gap = (int) Math.min(roundedEof - raf.getFilePointer(), ZERO_FILL_BUFFER.length);
                raf.write(ZERO_FILL_BUFFER, 0, gap);
            }
            if (raf.getFilePointer() % 4096 != 0)
                throw new IllegalStateException();
            chunkSectors[index] = (startSector << 8) | sectors;
            chunkTimestamps[index] = chunk.getLastMCAUpdate();
            chunksWritten++;
        }
    }

    @Override
    public void close() throws IOException {
        if (fileFinalized) return;
        try (Stopwatch.LapToken lap = fileCloseStopwatch.startLap()) {
            raf.seek(0);
            ByteBuffer byteBuffer = ByteBuffer.allocate(4096);
            byteBuffer.order(ByteOrder.BIG_ENDIAN);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(chunkSectors);
            raf.write(byteBuffer.array());
            intBuffer.clear();
            intBuffer.put(chunkTimestamps);
            raf.write(byteBuffer.array());
            raf.close();
            fileFinalized = true;
        }
    }

    @Override
    public String toString() {
        return String.format("chunks written: %4d; timing[total %s; serialize %s; write %s; init %s; finalize %s]",
                chunksWritten,
                elapsed(),
                chunkSerializationStopwatch,
                totalWriteStopwatch.subtract(chunkSerializationStopwatch),
                fileInitialized ? fileInitializationStopwatch : "n/a",
                fileFinalized ? fileCloseStopwatch : "n/a");
    }

    /**
     * @return A copy of a Stopwatch which represents the total time taken so far.
     */
    public Stopwatch elapsed() {
        return totalWriteStopwatch.add(fileInitializationStopwatch, fileCloseStopwatch);
    }
}
