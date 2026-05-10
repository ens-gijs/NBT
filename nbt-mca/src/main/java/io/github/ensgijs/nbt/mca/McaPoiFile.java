package io.github.ensgijs.nbt.mca;

/**
 * POI files are best thought of as an INDEX the game uses to be able to quickly locate certain blocks.
 * However, the names of the indexed locations is not necessarily a block type but often a description of its usage
 * and one poi type may map to multiple block types (e.g. poi of 'minecraft:home' maps to any of the bed blocks).
 *
 * <p>See {@link PoiRecord} for more information and for a list of POI types and how they map to blocks.</p>
 */
public class McaPoiFile extends McaFileBase<PoiChunk> {
    public McaPoiFile(int regionX, int regionZ) {
        super(regionX, regionZ);
    }

    public McaPoiFile(int regionX, int regionZ, int defaultDataVersion) {
        super(regionX, regionZ, defaultDataVersion);
    }

    public McaPoiFile(int regionX, int regionZ, DataVersion defaultDataVersion) {
        super(regionX, regionZ, defaultDataVersion);
    }

    @Override
    public Class<PoiChunk> chunkClass() {
        return PoiChunk.class;
    }

    @Override
    public PoiChunk createChunk() {
        PoiChunk chunk = new PoiChunk(getDefaultChunkDataVersion());
        return chunk;
    }
}
