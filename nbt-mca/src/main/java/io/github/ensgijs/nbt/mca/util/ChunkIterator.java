package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.mca.ChunkBase;
import io.github.ensgijs.nbt.mca.McaFileBase;

import java.util.Iterator;

/**
 * Enhanced iterator for iterating over {@link ChunkBase} data.
 * All 1024 chunks will be returned by successive calls to {@link #next()}, even
 * those which are {@code null}.
 * See {@link McaFileBase#iterator()}
 */
public interface ChunkIterator<I extends ChunkBase> extends Iterator<I> {
    /**
     * Replaces the current chunk with the one given by calling {@link McaFileBase#setChunk(int, ChunkBase)}
     * with the {@link #currentIndex()}. Take care as the given chunk is NOT copied by this call.
     * @param chunk Chunk to set, may be null.
     */
    void set(I chunk);

    /**
     * @return Current chunk index (in range 0-1023)
     */
    int currentIndex();

    /**
     * Note this value is calculated from the iterators position and is therefore known even if the chunk is null.
     *
     * @return Current chunk x within region in range [0-31]
     */
    default int currentX(){
        return currentIndex() & 0x1F;
    }

    /**
     * Note this value is calculated from the iterators position and is therefore known even if the chunk is null.
     *
     * @return Current chunk z within region in range [0-31]
     */
    default int currentZ() {
        return (currentIndex() >> 5) & 0x1F;
    }

    /**
     * Note this value is calculated from the iterators position and is therefore known even if the chunk is null.
     *
     * @return Current chunk xz within region, both x and z will be in range [0-31]
     */
    default IntPointXZ currentXZ() {
        return new IntPointXZ(currentX(), currentZ());
    }

    /**
     * Note this value is calculated from the iterators position not read from {@link ChunkBase#getChunkX()} and is
     * therefore known even if the chunk is null. If the chunk is not null and there is a mismatch between this
     * value and that returned by {@link ChunkBase#getChunkX()} then the chunk is "out of place" and should be
     * moved / corrected.
     *
     * @return Current chunk x in absolute coordinates (not block coordinates).
     * @see ChunkBase#moveChunk(int, int, long)
     */
    int currentAbsoluteX();

    /**
     * Note this value is calculated from the iterators position not read from {@link ChunkBase#getChunkZ()} and is
     * therefore known even if the chunk is null. If the chunk is not null and there is a mismatch between this
     * value and that returned by {@link ChunkBase#getChunkZ()} then the chunk is "out of place" and should be
     * moved / corrected.
     *
     * @return Current chunk z in absolute coordinates (not block coordinates)
     * @see ChunkBase#moveChunk(int, int, long)
     */
    int currentAbsoluteZ();

    default IntPointXZ currentAbsoluteXZ() {
        return new IntPointXZ(currentAbsoluteX(), currentAbsoluteZ());
    }
}
