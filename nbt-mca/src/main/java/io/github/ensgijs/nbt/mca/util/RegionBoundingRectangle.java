package io.github.ensgijs.nbt.mca.util;

import java.util.Collection;
import java.util.function.ToIntFunction;

public class RegionBoundingRectangle extends ChunkBoundingRectangle {
    /**
     * Bounds of the maximum size of a minecraft world.
     * Note that there are 368 XZ blocks (23 XZ chunks) outside the world border that are still within these region bounds.
     * <p>Remember that the max bound is exclusive (don't treat getMaxRegionXZ as in bounds).</p>
     * @see ChunkBoundingRectangle#MAX_WORLD_BORDER_BOUNDS
     */
    public static final RegionBoundingRectangle MAX_WORLD_REGION_BOUNDS =
            new RegionBoundingRectangle(-58594, -58594, 58594 * 2);

    public RegionBoundingRectangle(int regionX, int regionZ) {
        this(regionX, regionZ, 1);
    }

    public RegionBoundingRectangle(int regionX, int regionZ, int widthXZ) {
        super(regionX << 5, regionZ << 5, widthXZ << 5);
    }

    public RegionBoundingRectangle translateRegions(int x, int z) {
        return new RegionBoundingRectangle(
                getMinRegionX() + x,
                getMinRegionZ() + z,
                getWidthRegionXZ()
        );
    }

    /**
     * Computes a new region which shares the same center but is {@code size} larger in each direction.
     * Total width/height grows by 2x this value.
     * @param size amount to shrink by, must be GE0.
     * @return new larger rectangle
     */
    public RegionBoundingRectangle growRegions(int size) {
        if (size == 0) return this;
        if (size < 0) throw new IllegalArgumentException();
        return new RegionBoundingRectangle(getMinRegionX() - size, getMinRegionZ() - size, getWidthRegionXZ() + 2 * size);
    }

    /**
     * Computes a new region which shares the same center but is {@code size} smaller in each direction.
     * Total width/height reduces by 2x this value.
     * @param size amount to shrink by, must be GE0.
     * @return new rectangle if resulting width/height > 1, else null.
     */
    public RegionBoundingRectangle shrinkRegions(int size) {
        if (size == 0) return this;
        if (size < 0) throw new IllegalArgumentException("size must be GE 0");
        int newSize = getWidthRegionXZ() - 2 * size;
        if (newSize <= 0) return null;
        return new RegionBoundingRectangle(getMinRegionX() + size, getMinRegionZ() + size, newSize);
    }

    public boolean containsRegion(int regionX, int regionZ) {
        return containsBlock(regionX << 9, regionZ << 9);
    }

    public boolean containsRegion(IntPointXZ regionXZ) {
        return containsBlock(regionXZ.getX() << 9, regionXZ.getZ() << 9);
    }

    public final int getMinRegionX() {
        return minBlockX >> 9;
    }

    public final int getMinRegionZ() {
        return minBlockZ >> 9;
    }

    /** exclusive */
    public final int getMaxRegionX() {
        return maxBlockX >> 9;
    }

    /** exclusive */
    public final int getMaxRegionZ() {
        return maxBlockZ >> 9;
    }

    public final int getWidthRegionXZ() {
        return widthBlockXZ >> 9;
    }

    public static RegionBoundingRectangle forChunk(int x, int z) {
        return new RegionBoundingRectangle(x >> 5, z >> 5);
    }

    public static RegionBoundingRectangle forBlock(int x, int z) {
        return new RegionBoundingRectangle(x >> 9, z >> 9);
    }

    public ChunkBoundingRectangle asChunkBounds() {
        return new ChunkBoundingRectangle(getMinChunkX(), getMinChunkZ(), getWidthChunkXZ());
    }

    @Override
    public String toString() {
        return String.format("regions[%d..%d, %d..%d]",
                getMinRegionX(), getMaxRegionX() - 1, getMinRegionZ(), getMaxRegionZ() - 1);
    }

    public static RegionBoundingRectangle of(Collection<IntPointXZ> regions) {
        return of(regions, IntPointXZ::getX, IntPointXZ::getZ);
    }

    public static <T> RegionBoundingRectangle of(Collection<T> regions, ToIntFunction<T> xGetter, ToIntFunction<T> zGetter) {
        if (regions == null || regions.isEmpty())
            return null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (T xz : regions) {
            int x = xGetter.applyAsInt(xz);
            int z = zGetter.applyAsInt(xz);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        return new RegionBoundingRectangle(minX, minZ, Math.max(maxX - minX, maxZ - minZ) + 1);
    }
}
