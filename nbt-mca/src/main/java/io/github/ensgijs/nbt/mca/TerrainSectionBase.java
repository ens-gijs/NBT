package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.TextNbtParser;
import io.github.ensgijs.nbt.tag.*;
import io.github.ensgijs.nbt.mca.util.PalettizedCuboid;

import static io.github.ensgijs.nbt.mca.DataVersion.*;
import static io.github.ensgijs.nbt.mca.io.LoadFlags.*;

/**
 * Provides the base for all terrain section classes.
 */
public abstract class TerrainSectionBase extends SectionBase<TerrainSectionBase> {
    protected static final CompoundTag AIR_PALETTE_TAG = TextNbtParser.parseInline("{Name: \"minecraft:air\"}");
    /** Use with care! Be sure to clone this value when used or really bad bugs are going to happen. */
    protected static final CompoundTag DEFAULT_BLOCK_SATES_TAG = new PalettizedCuboid<>(16, AIR_PALETTE_TAG).toCompoundTag();
    /** Use with care! Be sure to clone this value when used or really bad bugs are going to happen. */
    protected static final CompoundTag DEFAULT_BIOMES_TAG = new PalettizedCuboid<>(4, new StringTag("minecraft:plains")).toCompoundTag();

    /** Only populated for MC version &lt; 1.13  - 4096 (16^3) block id's */
    protected byte[] legacyBlockIds;
    /** Only populated for MC version &lt; 1.13  - 4096 (16^3) block data values */
    protected byte[] legacyBlockDataValues;

    /**
     * Only populated for MC version &gt;= JAVA_1_13_17W47A; note bit packing changed in JAVA_1_16_20W17A
     * @see PalettizedCuboid
     * @since {@link DataVersion#JAVA_1_13_17W47A}
     */
    protected PalettizedCuboid<CompoundTag> blockStates;
    /**
     * Only populated for MC version &gt;= JAVA_1_18_21W37A (~ 1.18 pre1)
     * @see PalettizedCuboid
     * @since {@link DataVersion#JAVA_1_18_21W37A}
     */
    protected PalettizedCuboid<StringTag> biomes;

    protected byte[] blockLight;
    protected byte[] skyLight;

    public static byte[] createBlockLightBuffer() {
        return new byte[2048];
    }

    public static byte[] createSkyLightBuffer() {
        return new byte[2048];
    }

    public TerrainSectionBase(CompoundTag sectionRoot, int dataVersion) {
        this(sectionRoot, dataVersion, LOAD_ALL_DATA);
    }

    public TerrainSectionBase(CompoundTag sectionRoot, int dataVersion, long loadFlags) {
        super(sectionRoot, dataVersion, loadFlags);
    }

    protected void initReferences(final long loadFlags) {
        sectionY = data.getNumber("Y").byteValue();
        if ((loadFlags & BIOMES) != 0) {
            // Prior to JAVA_1_18_21W37A biomes were stored at the chunk level in a ByteArrayTag and used fixed ID's
            // Currently they are stored in a palette object at the section level
            if (dataVersion >= JAVA_1_18_21W37A.id()) {
                biomes = PalettizedCuboid.fromCompoundTag(data.getCompoundTag("biomes"), 4, dataVersion);
            }
        }
        if ((loadFlags & BLOCK_LIGHTS) != 0) {
            ByteArrayTag blockLight = data.getByteArrayTag("BlockLight");
            if (blockLight != null) this.blockLight = blockLight.getValue();
        }
        if ((loadFlags & BLOCK_STATES) != 0) {
            // Block palettes were added in 1.13 - prior to this the "Blocks" ByteArrayTag was used with fixed id's
            // In JAVA_1_16_20W17A palette data bit packing scheme changed
            // In JAVA_1_18_21W37A the section tag structure changed significantly and 'BlockStates' and 'Palette' were moved inside 'block_states' and renamed.
            if (dataVersion < JAVA_1_13_17W47A.id()) {
                ByteArrayTag legacyBlockIds = data.getByteArrayTag("Blocks");
                if (legacyBlockIds != null) this.legacyBlockIds = legacyBlockIds.getValue();
                ByteArrayTag legacyBlockDataValues = data.getByteArrayTag("Data");
                if (legacyBlockDataValues != null) this.legacyBlockDataValues = legacyBlockDataValues.getValue();
            } else if (dataVersion <= JAVA_1_18_21W37A.id()) {
                if (data.containsKey("Palette")) {
                    ListTag<CompoundTag> palette = data.getListTag("Palette").asCompoundTagList();
                    LongArrayTag blockStatesTag = data.getLongArrayTag("BlockStates");  // may be null
                    // up-convert to the modern block_states structure to simplify handling
                    CompoundTag paletteContainerTag = new CompoundTag(2);
                    paletteContainerTag.put("palette", palette);
                    if (blockStatesTag != null && blockStatesTag.length() > 0)
                        paletteContainerTag.put("data", blockStatesTag);
                    this.blockStates = PalettizedCuboid.fromCompoundTag(paletteContainerTag, 16, dataVersion);
                }
            } else {
                blockStates = PalettizedCuboid.fromCompoundTag(data.getCompoundTag("block_states"), 16, dataVersion);
            }
        }
        if ((loadFlags & SKY_LIGHT) != 0) {
            ByteArrayTag skyLight = data.getByteArrayTag("SkyLight");
            if (skyLight != null) this.skyLight = skyLight.getValue();
        }
    }

    public TerrainSectionBase(int dataVersion) {
        super(dataVersion);
        blockLight = createBlockLightBuffer();
        skyLight = createSkyLightBuffer();

        if (dataVersion >= JAVA_1_13_17W47A.id()) {
            // blockStatesTag normalized to 1.18+
            blockStates = PalettizedCuboid.fromCompoundTag(DEFAULT_BLOCK_SATES_TAG.clone(), 16, dataVersion);
        } else {
            legacyBlockIds = new byte[2048];
            legacyBlockDataValues = new byte[2048];
        }
        if (dataVersion >= JAVA_1_18_21W37A.id()) {
            biomes = PalettizedCuboid.fromCompoundTag(DEFAULT_BIOMES_TAG.clone(), 4, dataVersion);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void syncDataVersion(int newDataVersion) {
        super.syncDataVersion(newDataVersion);
        if (blockStates != null) blockStates.setDataVersion(newDataVersion);
        if (biomes != null) biomes.setDataVersion(newDataVersion);
    }

    /**
     * @return The block light array of this Section
     */
    public byte[] getBlockLight() {
        return blockLight;
    }

    /**
     * Sets the block light array for this section.
     * @param blockLight The block light array
     * @throws IllegalArgumentException When the length of the array is not 2048
     */
    public void setBlockLight(byte[] blockLight) {
        if (blockLight != null && blockLight.length != 2048) {
            throw new IllegalArgumentException("BlockLight array must have a length of 2048");
        }
        this.blockLight = blockLight;
    }

    /**
     * @return The sky light values of this Section
     */
    public byte[] getSkyLight() {
        return skyLight;
    }

    /**
     * Sets the sky light values of this section.
     * @param skyLight The custom sky light values
     * @throws IllegalArgumentException If the length of the array is not 2048
     */
    public void setSkyLight(byte[] skyLight) {
        if (skyLight != null && skyLight.length != 2048) {
            throw new IllegalArgumentException("SkyLight array must have a length of 2048");
        }
        this.skyLight = skyLight;
    }

    /** Only populated for MC version &lt; 1.13  - 4096 (16^3) block data values */
    public byte[] getLegacyBlockDataValues() {
        return legacyBlockDataValues;
    }

    public TerrainSectionBase setLegacyBlockDataValues(byte[] legacyBlockDataValues) {
        if (dataVersion >= JAVA_1_13_17W47A.id()) {
            throw new VersionLacksSupportException(dataVersion, null, JAVA_1_13_17W47A.previous(), "legacyBlockDataValues");
        }
        this.legacyBlockDataValues = legacyBlockDataValues;
        return this;
    }

    /**
     * @since {@link DataVersion#JAVA_1_13_17W47A}
     */
    public PalettizedCuboid<CompoundTag> getBlockStates() {
        return blockStates;
    }

    /**
     * @since {@link DataVersion#JAVA_1_13_17W47A}
     */
    public TerrainSectionBase setBlockStates(PalettizedCuboid<CompoundTag> blockStates) {
        if (dataVersion < JAVA_1_13_17W47A.id()) {
            throw new VersionLacksSupportException(dataVersion, JAVA_1_13_17W47A, null, "palettized blockStates");
        }
        this.blockStates = blockStates.clone();
        return this;
    }

    /**
     * @since {@link DataVersion#JAVA_1_18_21W37A}
     */
    public PalettizedCuboid<StringTag> getBiomes() {
        return biomes;
    }

    /**
     * @since {@link DataVersion#JAVA_1_18_21W37A}
     */
    public TerrainSectionBase setBiomes(PalettizedCuboid<StringTag> biomes) {
        if (dataVersion < JAVA_1_18_21W37A.id()) {
            throw new VersionLacksSupportException(dataVersion, JAVA_1_18_21W37A, null, "palettized biomes");
        }
        this.biomes = biomes.clone();
        return this;
    }

    /**
     * Null if MC version &gt; 1.12.2
     * See https://minecraft-ids.grahamedgecombe.com/
     */
    public byte[] getLegacyBlockIds() {
        return legacyBlockIds;
    }

    /** unsupported if MC version &gt; 1.12.2 */
    public TerrainSectionBase setLegacyBlockIds(byte[] legacyBlockIds) {
        if (dataVersion >= JAVA_1_13_17W47A.id())
            throw new VersionLacksSupportException(dataVersion, null, JAVA_1_13_17W47A.previous(),
                    "Legacy block id usage was replaced with block palettes in MC 1.13!");
        this.legacyBlockIds = legacyBlockIds;
        return this;
    }

    /**
     * Null if MC version &gt; 1.12.2
     * See https://minecraft-ids.grahamedgecombe.com/
     */
    public byte[] legacyBlockDataValues() {
        return legacyBlockDataValues;
    }

    /** unsupported if MC version &gt; 1.12.2 */
    public TerrainSectionBase legacyBlockDataValues(byte[] legacyBlockDataValues) {
        if (dataVersion >= JAVA_1_13_17W47A.id())
            throw new VersionLacksSupportException(dataVersion, null, JAVA_1_13_17W47A.previous(),
                    "Legacy block id usage was replaced with block palettes in MC 1.13!");
        this.legacyBlockDataValues = legacyBlockDataValues;
        return this;
    }

    /**
     * Updates the raw CompoundTag that this Section is based on.
     * @return A reference to the raw CompoundTag this Section is based on
     */
    @Override
    public CompoundTag updateHandle() {
        checkY(sectionY);
        this.data = super.updateHandle();
        data.putByte("Y", (byte) sectionY);
        if (dataVersion < JAVA_1_13_17W47A.id()) {
            if (legacyBlockIds != null) {
                data.putByteArray("Blocks", legacyBlockIds);
            }
            if (legacyBlockDataValues != null) {
                data.putByteArray("Data", legacyBlockDataValues);
            }
        } else if (dataVersion < JAVA_1_18_21W37A.id()) {
            if (blockStates != null) {
                CompoundTag blockStatesTag = blockStates.updateHandle();
                data.put("Palette", blockStatesTag.getListTag("palette"));
                if (blockStatesTag.containsKey("data")) {
                    data.putLongArray("BlockStates", blockStatesTag.getLongArray("data"));
                } else {
                    data.remove("BlockStates");
                }
            }
        } else {
            if (blockStates != null) {
                data.put("block_states", blockStates.updateHandle());
            }
        }
        if (biomes != null && dataVersion >= JAVA_1_18_21W37A.id()) {
            data.put("biomes", biomes.updateHandle());
        }
        if (blockLight != null) {
            data.putByteArray("BlockLight", blockLight);
        }
        if (skyLight != null) {
            data.putByteArray("SkyLight", skyLight);
        }
        return data;
    }
}
