package io.github.ensgijs.nbt.mca.util;

import io.github.ensgijs.nbt.mca.TerrainChunk;
import io.github.ensgijs.nbt.mca.io.LoadFlags;
import io.github.ensgijs.nbt.mca.io.McaFileHelpers;
import io.github.ensgijs.nbt.mca.io.RandomAccessMcaFile;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.StringTag;
import io.github.ensgijs.nbt.util.ArgValidator;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.github.ensgijs.nbt.mca.DataVersion.JAVA_1_18_21W37A;

/**
 * Abstracts away the interactions with individual chunks and mca files.
 * <p>Early impl limitations:</p>
 * <ul>
 *     <li>Only operates on {@link TerrainChunk}</li>
 *     <li>'rw' mode does not support creating new mca files or new chunks.</li>
 * </ul>
 */
public class McaWorld implements Closeable {
    static final int DEFAULT_CHUNK_CACHE_CAPACITY = 1024;
    private final boolean isReadonly;
    private final String mode;
    private final String worldRootDir;

    // TODO: use region/poi/entities abstraction chunk type (once one exists)
    private final Map<IntPointXZ, RandomAccessMcaFile<TerrainChunk>> regionCache = new HashMap<>();
    private final Map<IntPointXZ, TerrainChunk> chunkCache;
    private long loadFlags = LoadFlags.LOAD_ALL_DATA;

    public McaWorld(String worldRootDir, String mode, int chunkCacheSize) throws FileNotFoundException {
        ArgValidator.check(mode != null && mode.length() >= 1 && mode.charAt(0) == 'r');
        if (!new File(worldRootDir).exists()) {  // TODO: && mode == "r" - else create directory??
            throw new FileNotFoundException("World root directory does not exist! " + worldRootDir);
        }
        this.worldRootDir = worldRootDir;
        this.mode = mode;
        isReadonly = mode.equals("r");
        chunkCache = new LinkedHashMap<>(16, 0.75F, true) {
            // This method is called just after a new entry has been added
            // Note access order = true is specified to the map ctor so this is an LRU
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > chunkCacheSize;
            }
        };
    }
    public McaWorld(File worldRootDir, String mode, int chunkCacheSize) throws FileNotFoundException {
        this(worldRootDir.getAbsolutePath(), mode, chunkCacheSize);
    }
    public McaWorld(Path worldRootDir, String mode, int chunkCacheSize) throws FileNotFoundException {
        this(worldRootDir.toAbsolutePath().toString(), mode, chunkCacheSize);
    }
    public McaWorld(File worldRootDir, String mode) throws FileNotFoundException {
        this(worldRootDir.getAbsolutePath(), mode, DEFAULT_CHUNK_CACHE_CAPACITY);
    }
    public McaWorld(Path worldRootDir, String mode) throws FileNotFoundException {
        this(worldRootDir.toAbsolutePath().toString(), mode, DEFAULT_CHUNK_CACHE_CAPACITY);
    }
    public McaWorld(String worldRootDir, String mode) throws FileNotFoundException {
        this(worldRootDir, mode, DEFAULT_CHUNK_CACHE_CAPACITY);
    }

    public Set<IntPointXZ> touchedRegions() {
        return Collections.unmodifiableSet(regionCache.keySet());
    }

    /** LoadFlags which are passed to the chunk deserialization method. */
    public void setLoadFlags(long loadFlags) {
        // TODO: detect if bits have been added and if so clear the chunk cache
        this.loadFlags = loadFlags;
    }

    public boolean isReadonly() {
        return isReadonly;
    }

    public String mode() {
        return mode;
    }

    public String worldRootDir() {
        return worldRootDir;
    }

    public long loadFlags() {
        return loadFlags;
    }

    /**
     * Closing causes all currently opened mca files to be closed and for all cached chunk data to be released.
     * <p>May be called more than once. Object may continue to be used to access chunk data after calling close().</p>
     * @throws IOException one or more mca files threw when closing
     */
    @Override
    public void close() throws IOException {
        chunkCache.clear();
        List<IOException> closeExceptions = new ArrayList<>();
        for (RandomAccessMcaFile<?> ramf : regionCache.values()) {
            try {
                if (ramf != null)
                    ramf.close();
            } catch (IOException ex) {
                // TODO: improve this - don't printStackTrace and make throw below contain more context.
                ex.printStackTrace();
                closeExceptions.add(ex);
            }
        }
        int openCount = regionCache.size();
        regionCache.clear();
        if (!closeExceptions.isEmpty()) {
            throw new IOException("Error closing " + closeExceptions.size() + " of " + openCount + " MCA files!");
        }
    }

    public RandomAccessMcaFile<TerrainChunk> getRegion(int regionX, int regionZ) throws IOException {
        return getRegion(new IntPointXZ(regionX, regionZ));
    }

    public RandomAccessMcaFile<TerrainChunk> getRegion(IntPointXZ regionXZ) throws IOException {
        if (!regionCache.containsKey(regionXZ)) {
            String fileName = McaFileHelpers.createNameFromRegionLocation(regionXZ);
            File mcaFile = Path.of(worldRootDir, "region", fileName).toFile();
            RandomAccessMcaFile<TerrainChunk> ramf = null;
            // TODO: mode != "r" - create directory??
            if (mcaFile.exists() && Files.size(mcaFile.toPath()) > 0) {  // TODO: || !mode.equals("r")
                ramf = new RandomAccessMcaFile<>(TerrainChunk.class, mcaFile, mode);
                ramf.setLoadFlags(loadFlags);
            }
            regionCache.put(regionXZ, ramf);
        }

        return regionCache.get(regionXZ);
    }

    public TerrainChunk getChunk(int chunkX, int chunkZ) throws IOException {
        return getChunk(new IntPointXZ(chunkX, chunkZ));
    }

    public TerrainChunk getChunk(IntPointXZ chunkXZ) throws IOException {
        if (chunkCache.containsKey(chunkXZ))  // strategy allows caching of nulls
            return chunkCache.get(chunkXZ);

        var region = getRegion(chunkXZ.transformChunkToRegion());

        TerrainChunk chunk = null;
        if (region != null) {
            chunk = region.readAbsolute(chunkXZ);
        }
        // TODO: mode != "r" - create new chunk
        chunkCache.put(chunkXZ, chunk);
        return chunk;
    }

    /**
     * @param heightmap typically one of
     * <ul>
     *      <li>MOTION_BLOCKING</li>
     *  	<li>MOTION_BLOCKING_NO_LEAVES</li>
     *  	<li>OCEAN_FLOOR</li>
     *  	<li>OCEAN_FLOOR_WG</li>
     *  	<li>WORLD_SURFACE</li>
     *  	<li>WORLD_SURFACE_WG</li>
     * </ul>
     * @param xz block XZ location
     * @return top block Y location - or Integer.MIN_VALUE if chunk or heightmap name does not exist.
     * @throws IOException read error
     */
    public int getHeightAt(String heightmap, IntPointXZ xz) throws IOException {
        var chunk = getChunk(xz.transformBlockToChunk());
        // TODO: this can be more lenient || !chunk.getStatus().endsWith("full")
        if (chunk == null) return Integer.MIN_VALUE;
        var hm = chunk.getHeightMap(heightmap);
        if (hm == null) return Integer.MIN_VALUE;
        return hm.get2d(xz.x & 0xF, xz.z & 0xF);
    }

    /**
     * @param heightmap typically one of
     * <ul>
     *      <li>MOTION_BLOCKING</li>
     *  	<li>MOTION_BLOCKING_NO_LEAVES</li>
     *  	<li>OCEAN_FLOOR</li>
     *  	<li>OCEAN_FLOOR_WG</li>
     *  	<li>WORLD_SURFACE</li>
     *  	<li>WORLD_SURFACE_WG</li>
     * </ul>
     * @param x block X location
     * @param z block Z location
     * @return top block Y location - or Integer.MIN_VALUE if chunk or heightmap name does not exist.
     * @throws IOException read error
     */
    public int getHeightAt(String heightmap, int x, int z) throws IOException {
        return getHeightAt(heightmap, new IntPointXZ(x, z));
    }

    public String getBiomeAt(IntPointXYZ xyz) throws IOException {
        var chunk = getChunk(xyz.transformBlockToChunk());
        if (chunk == null) return null;

        if (!LegacyBiomes.versionHasLegacyBiomes(chunk.getDataVersion())) {
            var biomeTag = chunk.getBiomeAtByRef(xyz.x, xyz.y, xyz.z);
            return biomeTag != null ? biomeTag.getValue() : null;
        } else {
            return LegacyBiomes.keyedName(chunk.getDataVersion(), chunk.getLegacyBiomeAt(xyz.x, xyz.y, xyz.z));
        }
    }

    public String getBiomeAt(int x, int y, int z) throws IOException {
        var chunk = getChunk(x >> 4, z >> 4);
        if (chunk == null) return null;
        if (!LegacyBiomes.versionHasLegacyBiomes(chunk.getDataVersion())) {
            var biomeTag = chunk.getBiomeAtByRef(x, y, z);
            return biomeTag != null ? biomeTag.getValue() : null;
        } else {
            return LegacyBiomes.keyedName(chunk.getDataVersion(), chunk.getLegacyBiomeAt(x, y, z));
        }
    }

    /**
     * @return true if the chunk and section existed and the biome was set (true even if the value was unchanged)
     */
    public boolean setBiomeAt(IntPointXYZ xyz, String biome) throws IOException {
        if (isReadonly) throw new IOException("opened in readonly mode");
        var chunk = getChunk(xyz.x >> 4, xyz.z >> 4);
        if (chunk == null) return false;
        if (!LegacyBiomes.versionHasLegacyBiomes(chunk.getDataVersion())) {
            return chunk.setBiomeAt(xyz.x, xyz.y, xyz.z, new StringTag(biome));
        } else {
            if (xyz.y < 0 || xyz.y > 255) return false;
            int id = LegacyBiomes.id(chunk.getDataVersion(), biome);
            if (id < 0) return false;
            chunk.setLegacyBiomeAt(xyz.x, xyz.y, xyz.z, id);
            return true;
        }
    }

    /**
     * @return true if the chunk and section existed and the biome was set (true even if the value was unchanged)
     */
    public boolean setBiomeAt(int x, int y, int z, String biome) throws IOException {
        if (isReadonly) throw new IOException("opened in readonly mode");
        var chunk = getChunk(x >> 4, z >> 4);
        if (chunk == null) return false;
        if (!LegacyBiomes.versionHasLegacyBiomes(chunk.getDataVersion())) {
            return chunk.setBiomeAt(x, y, z, new StringTag(biome));
        } else {
            if (y < 0 || y > 255) return false;
            int id = LegacyBiomes.id(chunk.getDataVersion(), biome);
            if (id < 0) return false;
            chunk.setLegacyBiomeAt(x, y, z, id);
            return true;
        }
    }

    /**
     * @see BlockStateTag
     */
    public CompoundTag getBlockAt(IntPointXYZ xyz) throws IOException {
        var chunk = getChunk(xyz.transformBlockToChunk());
        return chunk != null ? chunk.getBlockAt(xyz.x, xyz.y, xyz.z) : null;
    }

    /**
     * @see BlockStateTag
     */
    public CompoundTag getBlockAt(int x, int y, int z) throws IOException {
        var chunk = getChunk(x >> 4, z >> 4);
        return chunk != null ? chunk.getBlockAt(x, y, z) : null;
    }

    /**
     * @see BlockStateTag
     */
    public CompoundTag getBlockAtByRef(IntPointXYZ xyz) throws IOException {
        var chunk = getChunk(xyz.transformBlockToChunk());
        return chunk != null ? chunk.getBlockAtByRef(xyz.x, xyz.y, xyz.z) : null;
    }

    /**
     * @see BlockStateTag
     */
    public CompoundTag getBlockAtByRef(int x, int y, int z) throws IOException {
        var chunk = getChunk(x >> 4, z >> 4);
        return chunk != null ? chunk.getBlockAtByRef(x, y, z) : null;
    }

    public String getBlockNameAt(IntPointXYZ xyz) throws IOException {
        var chunk = getChunk(xyz.transformBlockToChunk());
        if (chunk == null) return null;
        var blockTag = chunk.getBlockAtByRef(xyz.x, xyz.y, xyz.z);
        return blockTag != null ? blockTag.getString("Name") : null;
    }

    public String getBlockNameAt(int x, int y, int z) throws IOException {
        var chunk = getChunk(x >> 4, z >> 4);
        if (chunk == null) return null;
        var blockTag = chunk.getBlockAtByRef(x, y, z);
        return blockTag != null ? blockTag.getString("Name") : null;
    }


    /**
     * @param xyz block XYZ location
     * @param tag block palette tag, must contain a 'Name' StringTag
     * @return true if the chunk and section existed and the block was set (true even if the value was unchanged)
     * @see BlockStateTag
     */
    public boolean setBlockAt(IntPointXYZ xyz, CompoundTag tag) throws IOException {
        if (isReadonly) throw new IOException("opened in readonly mode");
        var chunk = getChunk(xyz.transformBlockToChunk());
        return chunk != null && chunk.setBlockAt(xyz.x, xyz.y, xyz.z, tag);
    }

    /**
     * @param x block X location
     * @param y block Y location
     * @param z block Z location
     * @param tag block palette tag, must contain a 'Name' StringTag
     * @return true if the chunk and section existed and the block was set (true even if the value was unchanged)
     * @see BlockStateTag
     */
    public boolean setBlockAt(int x, int y, int z, CompoundTag tag) throws IOException {
        if (isReadonly) throw new IOException("opened in readonly mode");
        var chunk = getChunk(x >> 4, z >> 4);
        return chunk != null && chunk.setBlockAt(x, y, z, tag);
    }
}
