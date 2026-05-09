package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.tag.CompoundTag;

public class PoiChunk extends PoiChunkBase<PoiRecord>{

    protected PoiChunk(int dataVersion) {
        super(dataVersion);
    }

    public PoiChunk() {
        super(DataVersion.latest().id());
    }

    public PoiChunk(CompoundTag data) {
        super(data);
    }

    public PoiChunk(CompoundTag data, long loadFlags) {
        super(data, loadFlags);
    }

    @Override
    protected PoiRecord createPoiRecord(CompoundTag recordTag) {
        return new PoiRecord(recordTag);
    }

}
