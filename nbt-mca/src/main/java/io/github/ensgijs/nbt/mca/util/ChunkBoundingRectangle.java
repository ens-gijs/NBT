package io.github.ensgijs.nbt.mca.util;

import java.util.Collection;
import java.util.function.ToIntFunction;

public class ChunkBoundingRectangle extends BlockAlignedBoundingRectangle {
    /**
     * Maximum size of the world border.
     * Note that there are 368 XZ blocks (23 XZ chunks) outside the world border that can possibly be generated.
     * <p>Remember that the max bound is exclusive (don't treat getMaxChunkXZ as in bounds).</p>
     * @see RegionBoundingRectangle#MAX_WORLD_REGION_BOUNDS
     */
    public static final ChunkBoundingRectangle MAX_WORLD_BORDER_BOUNDS =
            new ChunkBoundingRectangle(-1874999, -1874999, 1874999 * 2);

    public ChunkBoundingRectangle(int chunkX, int chunkZ) {
        this(chunkX, chunkZ, 1);
    }

    public ChunkBoundingRectangle(int chunkX, int chunkZ, int chunkWidthXZ) {
        super(chunkX << 4, chunkZ << 4, chunkWidthXZ << 4);
    }

    public ChunkBoundingRectangle translateChunks(int x, int z) {
        return new ChunkBoundingRectangle(
                getMinChunkX() + x,
                getMinChunkZ() + z,
                getWidthChunkXZ()
        );
    }

    /**
     * Computes a new region which shares the same center but is {@code size} larger in each direction.
     * Total width/height grows by 2x this value.
     * @param size amount to shrink by, must be GE0.
     * @return new larger rectangle
     */
    public ChunkBoundingRectangle growChunks(int size) {
        if (size == 0) return this;
        if (size < 0) throw new IllegalArgumentException();
        return new ChunkBoundingRectangle(getMinChunkX() - size, getMinChunkZ() - size, getWidthChunkXZ() + 2 * size);
    }

    /**
     * Computes a new region which shares the same center but is {@code size} smaller in each direction.
     * Total width/height reduces by 2x this value.
     * @param size amount to shrink by, must be GE0.
     * @return new rectangle if resulting width/height > 1, else null.
     */
    public ChunkBoundingRectangle shrinkChunks(int size) {
        if (size == 0) return this;
        if (size < 0) throw new IllegalArgumentException("size must be GE 0");
        int newSize = getWidthChunkXZ() - 2 * size;
        if (newSize <= 0) return null;
        return new ChunkBoundingRectangle(getMinChunkX() + size, getMinChunkZ() + size, newSize);
    }

    public int getMinChunkX() {
        return minBlockX >> 4;
    }

    public int getMinChunkZ() {
        return minBlockZ >> 4;
    }

    /** exclusive */
    public int getMaxChunkX() {
        return maxBlockX >> 4;
    }

    /** exclusive */
    public int getMaxChunkZ() {
        return maxBlockZ >> 4;
    }

    public int getWidthChunkXZ() {
        return widthBlockXZ >> 4;
    }

    public boolean containsChunk(int chunkX, int chunkZ) {
        return containsBlock(chunkX << 4, chunkZ << 4);
    }

    public boolean containsChunk(IntPointXZ chunkXZ) {
        return containsBlock(chunkXZ.getX() << 4, chunkXZ.getZ() << 4);
    }

    /**
     * Calculates a new absolute X such that the given absolute X is inside this chunk
     * at the same relative location as the given X was relative to its source chunk.
     *
     * @param blockX in block coordinates
     * @return an X within the bounds of this chunk
     */
    public final int relocateX(int blockX) {
        return minBlockX | (blockX & 0xF);
    }

    /**
     * Calculates a new absolute Z such that the given absolute Z is inside this chunk
     * at the same relative location as the given Z was relative to its source chunk.
     *
     * @param blockZ in block coordinates
     * @return an Z within the bounds of this chunk
     */
    public final int relocateZ(int blockZ) {
        return minBlockZ | (blockZ & 0xF);
    }

    public final IntPointXZ relocate(IntPointXZ blockXZ) {
        return new IntPointXZ(minBlockX | (blockXZ.getX() & 0xF), minBlockZ | (blockXZ.getZ() & 0xF));
    }

    public final IntPointXZ relocate(int blockX, int blockZ) {
        return new IntPointXZ(minBlockX | (blockX & 0xF), minBlockZ | (blockZ & 0xF));
    }

    /**
     * Calculates a new absolute X such that the given absolute X is inside this chunk
     * at the same relative location as the given X was relative to its source chunk.
     *
     * @param blockX in block coordinates
     * @return an X within the bounds of this chunk
     */
    public final double relocateX(double blockX) {
        double bin = blockX % 16;
        return (bin >= 0 ? minBlockX : maxBlockX) + bin;
    }

    /**
     * Calculates a new absolute Z such that the given absolute Z is inside this chunk
     * at the same relative location as the given Z was relative to its source chunk.
     *
     * @param blockZ in block coordinates
     * @return an Z within the bounds of this chunk
     */
    public final double relocateZ(double blockZ) {
        double bin = blockZ % 16;
        return (bin >= 0 ? minBlockZ : maxBlockZ) + bin;
    }

    public BlockAlignedBoundingRectangle asBlockBounds() {
        return new BlockAlignedBoundingRectangle(getMinBlockX(), getMinBlockZ(), getWidthBlockXZ());
    }

    @Override
    public String toString() {
        return String.format("chunks[%d..%d, %d..%d]",
                getMinChunkX(), getMaxChunkX() - 1, getMinChunkZ(), getMaxChunkZ() - 1);
    }

    public static ChunkBoundingRectangle of(Collection<IntPointXZ> chunks) {
        return of(chunks, IntPointXZ::getX, IntPointXZ::getZ);
    }

    public static <T> ChunkBoundingRectangle of(Collection<T> chunks, ToIntFunction<T> xGetter, ToIntFunction<T> zGetter) {
        if (chunks == null || chunks.isEmpty())
            return null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (T xz : chunks) {
            int x = xGetter.applyAsInt(xz);
            int z = zGetter.applyAsInt(xz);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        return new ChunkBoundingRectangle(minX, minZ, Math.max(maxX - minX, maxZ - minZ) + 1);
    }
}
