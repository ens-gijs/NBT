package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.io.NamedTag;
import io.github.ensgijs.nbt.mca.util.*;
import io.github.ensgijs.nbt.tag.*;
import io.github.ensgijs.nbt.mca.io.MoveChunkFlags;
import io.github.ensgijs.nbt.query.NbtPath;
import io.github.ensgijs.nbt.util.ArgValidator;

import java.util.*;

import static io.github.ensgijs.nbt.mca.DataVersion.*;
import static io.github.ensgijs.nbt.mca.io.LoadFlags.*;
import static io.github.ensgijs.nbt.mca.io.MoveChunkFlags.*;

/**
 * Represents a Terrain data mca chunk. Terrain chunks are composed of a set of {@link TerrainSection} where any empty/null
 * section is filled with air blocks by the game. When altering existing chunks for MC 1.14+, be sure to have read and
 * understood the documentation on {@link PoiRecord} to avoid problems with villagers, nether portal linking,
 * lodestones, bees, and probably more as Minecraft continues to evolve.
 */
public abstract class TerrainChunkBase<T extends TerrainSectionBase> extends SectionedChunkBase<T> {

	protected long lastUpdateTick;
	/** Tick when the chunk was last saved. */
	public static final VersionAware<NbtPath> LAST_UPDATE_TICK_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.LastUpdate"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("LastUpdate"));

	protected long inhabitedTimeTicks;
	/** Cumulative amount of time players have spent in this chunk in ticks. */
	public static final VersionAware<NbtPath> INHABITED_TIME_TICKS_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.InhabitedTime"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("InhabitedTime"));

	protected int[] legacyBiomes;
	/**
	 * Only populated for data versions &lt; JAVA_1_18_21W39A. For later data versions use
	 * {@link PalettizedCuboid} and load biomes from {@link TerrainSectionBase#getBiomes()}.
	 * @see <a href=https://minecraft.fandom.com/wiki/Biome/IDs_before_1.13>minecraft.fandom.com/wiki/Biome/IDs_before_1.13</a>
	 * @see <a href=https://minecraft.fandom.com/wiki/Biome/ID>minecraft.fandom.com/wiki/Biome/ID</a>
	 */
	public static final VersionAware<NbtPath> LEGACY_BIOMES_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.Biomes"))  // ByteArrayTag
			.register(JAVA_1_13_18W06A.id(), NbtPath.of("Level.Biomes"))  // IntArrayTag
			.register(JAVA_1_18_21W37A.id(), null);  // biomes are now paletted and live in a similar container structure in sections[].biomes

	protected IntArrayTag legacyHeightMap;
	public static final VersionAware<NbtPath> LEGACY_HEIGHT_MAP_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.HeightMap"))
			.register(JAVA_1_13_18W06A.id(), null);

	protected CompoundTag heightMaps;
	/**
	 * {@link CompoundTag} mapping various heightmap names to 256 (16x16) values, long[] packed,
	 * min bits per value of 9. Heightmap values are "number of blocks above bottom of world", this is not
	 * the same as block Y position. To compute the block Y value use {@code highestBlockY =
	 * (chunk.yPos * 16) - 1 + heightmap_entry_value}.
	 * <ul>
	 *      <li>MOTION_BLOCKING</li>
	 *  	<li>MOTION_BLOCKING_NO_LEAVES</li>
	 *  	<li>OCEAN_FLOOR</li>
	 *  	<li>OCEAN_FLOOR_WG</li>
	 *  	<li>WORLD_SURFACE</li>
	 *  	<li>WORLD_SURFACE_WG</li>
	 * </ul>
	 * @since {@link DataVersion#JAVA_1_13_18W06A}
	 * @see LongArrayTagPackedIntegers
	 */
	public static final VersionAware<NbtPath> HEIGHT_MAPS_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_13_18W06A.id(), NbtPath.of("Level.Heightmaps"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("Heightmaps"));

	protected CompoundTag carvingMasks;
	public static final VersionAware<NbtPath> CARVING_MASKS_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_13_18W19A.id(), NbtPath.of("Level.CarvingMasks"))  // CompoundTag containing named ByteArrayTag's
			.register(JAVA_1_18_2_22W03A.id(), NbtPath.of("Level.CarvingMasks"))  // CompoundTag containing named LongArrayTag's
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("CarvingMasks"));  // CompoundTag containing named LongArrayTag's

	protected ListTag<CompoundTag> entities;  // usage changed for chunk versions >= 2724 (1.17) after which entities are only stored in terrain chunks during world generation.
	public static final VersionAware<NbtPath> ENTITIES_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.Entities"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("entities"));

	protected ListTag<CompoundTag> tileEntities;
	public static final VersionAware<NbtPath> TILE_ENTITIES_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.TileEntities"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("block_entities"));

	protected ListTag<CompoundTag> tileTicks;
	public static final VersionAware<NbtPath> TILE_TICKS_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.TileTicks"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("block_ticks"));

	protected ListTag<ListTag<?>> toBeTicked;
	public static final VersionAware<NbtPath> TO_BE_TICKED_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_13_18W06A.id(), NbtPath.of("Level.ToBeTicked"))
			.register(JAVA_1_18_21W43A.id(), null);  // unsure when this was removed - but notes on JAVA_1_18_21W43A say it was also "moved to block_ticks" - but the mca scans last saw it in JAVA_1_14_PRE2

	protected ListTag<CompoundTag> liquidTicks;
	public static final VersionAware<NbtPath> LIQUID_TICKS_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.LiquidTicks"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("fluid_ticks"));

	protected ListTag<ListTag<?>> liquidsToBeTicked;
	public static final VersionAware<NbtPath> LIQUIDS_TO_BE_TICKED_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.LiquidsToBeTicked"))
			.register(JAVA_1_18_21W43A.id(), null);  // unsure when this was removed - but notes on JAVA_1_18_21W43A say it was also "moved to block_ticks" - but the mca scans last saw it in JAVA_1_14_PRE2

	protected ListTag<ListTag<?>> lights;
	public static final VersionAware<NbtPath> LIGHTS_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_13_18W06A.id(), NbtPath.of("Level.Lights"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("Lights"));

	protected ListTag<ListTag<?>> postProcessing;
	public static final VersionAware<NbtPath> POST_PROCESSING_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_13_18W06A.id(), NbtPath.of("Level.PostProcessing"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("PostProcessing"));

	protected String status;
	public static final VersionAware<NbtPath> STATUS_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_13_18W06A.id(), NbtPath.of("Level.Status"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("Status"));

	protected CompoundTag structures;
	public static final VersionAware<NbtPath> STRUCTURES_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.Structures"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("structures"));
	/** Relative to {@link #STRUCTURES_PATH} */
	public static final VersionAware<NbtPath> STRUCTURES_STARTS_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Starts"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("starts"));
	/** Relative to {@link #STRUCTURES_PATH} */
	public static final VersionAware<NbtPath> STRUCTURES_REFERENCES_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("References"));

	public static final VersionAware<NbtPath> IS_LIGHT_POPULATED_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.LightPopulated"))
			.register(JAVA_1_13_18W06A.id(), null);  // probably replaced by Level.Status progression

	protected Boolean isLightOn;
	public static final VersionAware<NbtPath> IS_LIGHT_ON_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_14_19W02A.id(), NbtPath.of("Level.isLightOn"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("isLightOn"));

	protected Boolean isTerrainPopulated;
	public static final VersionAware<NbtPath> TERRAIN_POPULATED_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.TerrainPopulated"))
			.register(JAVA_1_13_18W06A.id(), null);  // replaced by Level.Status progression

	protected Boolean hasLegacyStructureData;
	public static final VersionAware<NbtPath> HAS_LEGACY_STRUCTURE_DATA_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.hasLegacyStructureData"))
			.register(JAVA_1_13_18W20C.id(), null);  // might not be exactly correct

	protected CompoundTag upgradeData;
	public static final VersionAware<NbtPath> UPGRADE_DATA_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.UpgradeData"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("UpgradeData"));

	public static final VersionAware<NbtPath> SECTIONS_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.Sections"))
			.register(JAVA_1_18_21W37A.id(), NbtPath.of("sections"));

	public static final VersionAware<NbtPath> X_POS_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.xPos"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("xPos"));

	public static final VersionAware<NbtPath> Z_POS_PATH = new VersionAware<NbtPath>()
			.register(0, NbtPath.of("Level.zPos"))
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("zPos"));

	/**
	 * Represents world bottom - note there may exist a dummy chunk -1 below this depending on MC flavor and current chunk state.
	 * @since {@link DataVersion#JAVA_1_18_21W43A}
	 */
	protected int yPos = NO_CHUNK_COORD_SENTINEL;
	public static final VersionAware<NbtPath> Y_POS_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("yPos"));
	public static final VersionAware<Integer> DEFAULT_WORLD_BOTTOM_Y_POS = new VersionAware<Integer>()
			.register(0, 0)
			.register(JAVA_1_18_21W43A.id(), -4);  // TODO: IDK when exactly they actually enabled deep worlds

	/** @since {@link DataVersion#JAVA_1_18_21W43A} */
	protected CompoundTag belowZeroRetrogen;
	public static final VersionAware<NbtPath> BELOW_ZERO_RETROGEN_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("below_zero_retrogen"));

	/** @since {@link DataVersion#JAVA_1_18_21W43A} */
	protected CompoundTag blendingData;
	public static final VersionAware<NbtPath> BLENDING_DATA_PATH = new VersionAware<NbtPath>()
			.register(JAVA_1_18_21W43A.id(), NbtPath.of("blending_data"));


	protected TerrainChunkBase(int dataVersion) {
		super(dataVersion);
	}

	/**
	 * Create a new chunk based on raw base data from a Terrain region file.
	 * @param data The raw base data to be used.
	 */
	public TerrainChunkBase(CompoundTag data) {
		super(data);
	}

	public TerrainChunkBase(CompoundTag data, long loadFlags) {
		super(data, loadFlags);
	}

	@Override
	protected void initMembers() {
		// give this a reasonable default
		yPos = DEFAULT_WORLD_BOTTOM_Y_POS.get(dataVersion);
	}

	@Override
	protected void initReferences(final long loadFlags) {
		if (dataVersion < JAVA_1_18_21W39A.id()) {
			if (data.getCompoundTag("Level") == null) {
				throw new IllegalArgumentException("data does not contain \"Level\" tag");
			}
		}

		inhabitedTimeTicks = getTagValue(INHABITED_TIME_TICKS_PATH, LongTag::asLong, 0L);
		lastUpdateTick = getTagValue(LAST_UPDATE_TICK_PATH, LongTag::asLong, 0L);
		if (dataVersion < JAVA_1_18_21W37A.id() && (loadFlags & BIOMES) != 0) {
			if (dataVersion >= DataVersion.JAVA_1_13_18W06A.id()) {
				legacyBiomes = getTagValue(LEGACY_BIOMES_PATH, IntArrayTag::getValue);
			} else {
				byte[] byteBiomes = getTagValue(LEGACY_BIOMES_PATH, ByteArrayTag::getValue);
				legacyBiomes = new int[byteBiomes.length];
				for (int i = 0; i < legacyBiomes.length; i++) {
					legacyBiomes[i] = byteBiomes[i];
				}
			}
			if (legacyBiomes != null && legacyBiomes.length == 0) legacyBiomes = null;
		} // palette biomes are stored at the section, not chunk, level.

		if ((loadFlags & HEIGHTMAPS) != 0) {
			legacyHeightMap = getTag(LEGACY_HEIGHT_MAP_PATH);
			heightMaps = getTag(HEIGHT_MAPS_PATH);
		}
		if ((loadFlags & CARVING_MASKS) != 0) {
			carvingMasks = getTag(CARVING_MASKS_PATH);
		}
		if ((loadFlags & ENTITIES) != 0) {
			entities = getTag(ENTITIES_PATH);
		}
		if ((loadFlags & TILE_ENTITIES) != 0) {
			tileEntities = getTag(TILE_ENTITIES_PATH);
		}
		if ((loadFlags & TILE_TICKS) != 0) {
			tileTicks = getTag(TILE_TICKS_PATH);
		}
		if ((loadFlags & TO_BE_TICKED) != 0) {
			toBeTicked = getTag(TO_BE_TICKED_PATH);
		}
		if ((loadFlags & LIGHTS) != 0) {
			lights = getTag(LIGHTS_PATH);
		}
		if ((loadFlags & LIQUID_TICKS) != 0) {
			liquidTicks = getTag(LIQUID_TICKS_PATH);
		}
		if ((loadFlags & LIQUIDS_TO_BE_TICKED) != 0) {
			liquidsToBeTicked = getTag(LIQUIDS_TO_BE_TICKED_PATH);
		}
		if ((loadFlags & POST_PROCESSING) != 0) {
			postProcessing = getTag(POST_PROCESSING_PATH);
		}

		status = getTagValue(STATUS_PATH, StringTag::getValue);
		isLightOn = getTagValue(IS_LIGHT_ON_PATH, ByteTag::asBoolean);
		isTerrainPopulated = getTagValue(TERRAIN_POPULATED_PATH, ByteTag::asBoolean);

		// TODO: add load flag for this
		upgradeData = getTag(UPGRADE_DATA_PATH);

		if ((loadFlags & STRUCTURES) != 0) {
			structures = getTag(STRUCTURES_PATH);
			hasLegacyStructureData = getTagValue(HAS_LEGACY_STRUCTURE_DATA_PATH, ByteTag::asBoolean);
		}

		// chunkXZ may be pre-populated with a solid guess so don't overwrite that guess if we don't have values.
		if (X_POS_PATH.get(dataVersion).exists(data)) {
			chunkX = getTagValue(X_POS_PATH, t -> ((NumberTag<?>)t).asInt());
		}
		if (Z_POS_PATH.get(dataVersion).exists(data)) {
			chunkZ = getTagValue(Z_POS_PATH, t -> ((NumberTag<?>)t).asInt());
		}

		yPos = getTagValue(Y_POS_PATH, t -> ((NumberTag<?>)t).asInt(), DEFAULT_WORLD_BOTTOM_Y_POS.get(dataVersion));

		boolean loadSections = ((loadFlags & (BLOCK_LIGHTS|BLOCK_STATES|SKY_LIGHT)) != 0)
				|| (dataVersion >= JAVA_1_18_21W37A.id() && ((loadFlags & BIOMES) != 0));
		if (loadSections) {
			try {
				ListTag<CompoundTag> sections = getTag(SECTIONS_PATH);
				if (sections != null) {
					for (CompoundTag section : sections) {
						T newSection = createSection(section, dataVersion, loadFlags);
						putSection(newSection.getSectionY(), newSection, false);
					}
				}
			} catch (Exception ex) {
				throw new RuntimeException("Chunk " + getChunkX() + " " + getChunkZ() + "\n" + ex.getMessage(), ex);
			}
		}
		if ((loadFlags & WORLD_UPGRADE_HINTS) != 0) {
			belowZeroRetrogen = getTag(BELOW_ZERO_RETROGEN_PATH);
			blendingData = getTag(BLENDING_DATA_PATH);
		}
	}

	protected abstract T createSection(CompoundTag section, int dataVersion, long loadFlags);

	/** {@inheritDoc} */
	public String getMcaType() {
		return "region";
	}

	/**
	 * May only be used for data versions LT 2203 which includes all of 1.14
	 * and up until 19w36a (a 1.15 weekly snapshot).
	 * <p>Note: 2D biomes have a resolution of 1x256x1 blocks.</p>
	 * @deprecated unsupported after {@link DataVersion#JAVA_1_15_19W35A} use {@link #getLegacyBiomeAt(int, int, int)} instead for 1.15 and beyond
	 */
	@Deprecated
	public int getLegacyBiomeAt(int blockX, int blockZ) {
		if (dataVersion > JAVA_1_15_19W35A.id())
			throw new VersionLacksSupportException(dataVersion, null, JAVA_1_15_19W35A,
					"cannot get biome using Chunk#getBiomeAt(int,int) from biome data with DataVersion of 2203 or higher (1.15+), use Chunk#getBiomeAt(int,int,int) instead");
		if (legacyBiomes == null || legacyBiomes.length != 256) {
			return -1;
		}
		return legacyBiomes[getLegacy2dBiomeIndex(blockX, blockZ)];
	}

	/**
	 * Fetches a biome id at a specific block in this chunk.
	 * The coordinates can be absolute coordinates or relative to the region or chunk.
	 * <p>Note: 3D biomes have a resolution of 4x4x4 blocks.</p>
	 * @param blockX The x-coordinate of the block.
	 * @param blockY The y-coordinate of the block.
	 * @param blockZ The z-coordinate of the block.
	 * @return The biome id or -1 if the biomes are not correctly initialized.
	 * @deprecated unsupported after {@link DataVersion#JAVA_1_17_1}
	 */
	public int getLegacyBiomeAt(int blockX, int blockY, int blockZ) {
		if (dataVersion > JAVA_1_17_1.id())
			throw new VersionLacksSupportException(dataVersion, null, JAVA_1_17_1, "legacy biomes");
		if (dataVersion >= JAVA_1_15_19W36A.id()) {  // 3D biomes
			if (legacyBiomes == null || legacyBiomes.length != 1024) {
				return -1;
			}
			int biomeX = (blockX & 0xF) >> 2;
			int biomeY = (blockY & 0xF) >> 2;
			int biomeZ = (blockZ & 0xF) >> 2;

			return legacyBiomes[getLegacy3dBiomeIndex(biomeX, biomeY, biomeZ)];
		} else {  // 2D biomes
			return getLegacyBiomeAt(blockX, blockZ);
		}
	}

	/**
	 * Should only be used for data versions LT 2203 which includes all of 1.14
	 * and up until 19w35a (a 1.15 weekly snapshot).
	 * <p>Note: 2D biomes have a resolution of 1x256x1 blocks.</p>
	 * @deprecated unsupported after {@link DataVersion#JAVA_1_17_1}
	 * @see #setLegacyBiomeAt(int, int, int, int)
	 */
	@Deprecated
	public void setLegacyBiomeAt(int blockX, int blockZ, int biomeID) {
		checkRaw();
		if (dataVersion > JAVA_1_17_1.id())
			throw new VersionLacksSupportException(dataVersion, null, JAVA_1_17_1, "2D legacy biomes");
		if (dataVersion < JAVA_1_15_19W36A.id()) {  // 2D biomes
			if (legacyBiomes == null || legacyBiomes.length != 256) {
				legacyBiomes = new int[256];
				Arrays.fill(legacyBiomes, -1);
			}
			legacyBiomes[getLegacy2dBiomeIndex(blockX, blockZ)] = biomeID;
		} else {  // 3D biomes
			if (legacyBiomes == null || legacyBiomes.length != 1024) {
				legacyBiomes = new int[1024];
				Arrays.fill(legacyBiomes, -1);
			}

			int biomeX = (blockX & 0xF) >> 2;
			int biomeZ = (blockZ & 0xF) >> 2;

			for (int y = 0; y < 64; y++) {
				legacyBiomes[getLegacy3dBiomeIndex(biomeX, y, biomeZ)] = biomeID;
			}
		}
	}

	/**
	 * <p>Note: 3D biomes have a resolution of 4x4x4 blocks.</p>
	 * @since {@link DataVersion#JAVA_1_15_19W36A}
	 * @deprecated unsupported after {@link DataVersion#JAVA_1_17_1}
	 * @see #setLegacyBiomeAt(int, int, int, int)
	 */
	@Deprecated
	public void setLegacyBiomeAt(int blockX, int blockY, int blockZ, int biomeID) {
		if (dataVersion < JAVA_1_15_19W36A.id() || dataVersion >= JAVA_1_18_21W37A.id())
			throw new VersionLacksSupportException(dataVersion, JAVA_1_15_19W36A, JAVA_1_18_21W37A.previous(), "3D legacy biomes");
		if (legacyBiomes == null || legacyBiomes.length != 1024) {
			legacyBiomes = new int[1024];
			Arrays.fill(legacyBiomes, -1);
		}

		int biomeX = (blockX & 0x0F) >> 2;
		int biomeY = blockY >> 2;
		int biomeZ = (blockZ & 0x0F) >> 2;
		legacyBiomes[getLegacy3dBiomeIndex(biomeX, biomeY, biomeZ)] = biomeID;
	}

	protected int getLegacy2dBiomeIndex(int blockX, int blockZ) {
		return (blockZ & 0xF) * 16 + (blockX & 0xF);
	}
	protected int getLegacy3dBiomeIndex(int biomeX, int biomeY, int biomeZ) {
		return biomeY * 16 + biomeZ * 4 + biomeX;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setDataVersion(int dataVersion) {
		super.setDataVersion(dataVersion);
		for (T section : this) {
			if (section != null) {
				section.syncDataVersion(dataVersion);
			}
		}
	}

	/**
	 * @return The generation station of this chunk.
	 */
	public String getStatus() {
		return status != null ? status : getTagValue(STATUS_PATH, StringTag::getValue);
	}

	/**
	 * Sets the generation status of this chunk.
	 * @param status The generation status of this chunk.
	 */
	public void setStatus(String status) {
		checkRaw();
		this.status = status;
	}

	// TODO(javadoc)
	public Boolean getLightOn() {
		return isLightOn;
	}

	// TODO(javadoc)
	public void setLightOn(Boolean lightOn) {
		isLightOn = lightOn;
	}

	// TODO(javadoc)
	public Boolean getTerrainPopulated() {
		return isTerrainPopulated;
	}

	// TODO(javadoc)
	public void setTerrainPopulated(Boolean terrainPopulated) {
		isTerrainPopulated = terrainPopulated;
	}

	// TODO(javadoc)
	public Boolean getHasLegacyStructureData() {
		return hasLegacyStructureData;
	}

	// TODO(javadoc)
	public void setHasLegacyStructureData(Boolean hasLegacyStructureData) {
		this.hasLegacyStructureData = hasLegacyStructureData;
	}

	// TODO(javadoc)
	public CompoundTag getUpgradeData() {
		return upgradeData;
	}

	// TODO(javadoc)
	public void setUpgradeData(CompoundTag upgradeData) {
		this.upgradeData = upgradeData;
	}

	// 2048 bytes recording the amount of block-emitted light in each block. Makes load times faster compared to recomputing at load time. 4 bits per block.


	/** Tick when the chunk was last saved. */
	public long getLastUpdateTick() {
		return lastUpdateTick;
	}

	/** Sets the tick when the chunk was last saved. */
	public void setLastUpdateTick(long lastUpdateTick) {
		this.lastUpdateTick = lastUpdateTick;
	}

	/**
	 * @return The cumulative amount of time players have spent in this chunk in ticks.
	 */
	public long getInhabitedTimeTicks() {
		return inhabitedTimeTicks;
	}

	/**
	 * Sets the cumulative amount of time players have spent in this chunk in ticks.
	 * @param inhabitedTimeTicks The time in ticks.
	 */
	public void setInhabitedTimeTicks(long inhabitedTimeTicks) {
		checkRaw();
		this.inhabitedTimeTicks = inhabitedTimeTicks;
	}

	/**
	 * @return A matrix of biome IDs for all block columns in this chunk.
	 */
	public int[] getLegacyBiomes() {
		return legacyBiomes;
	}

	/**
	 * Sets the biome IDs for this chunk.
	 * <p>Note: 2D biomes have a resolution of 1x256x1 blocks.</p>
	 * <p>Note: 3D biomes have a resolution of 4x4x4 blocks.</p>
	 * @param legacyBiomes The biome ID matrix of this chunk. Must have a length of {@code 1024} for 1.15+ or {@code 256}
	 *                  for prior versions.
	 * @throws IllegalArgumentException When the biome matrix is {@code null} or does not have a version appropriate length.
	 */
	public void setLegacyBiomes(int[] legacyBiomes) {
		checkRaw();
		if (dataVersion >= JAVA_1_17_1.id())
			throw new VersionLacksSupportException(dataVersion, null, JAVA_1_17_1, "2D/3D legacy biomes");
		if (legacyBiomes != null) {
			final int requiredSize = dataVersion >= JAVA_1_15_19W36A.id() ? 1024 : 256;
			if (legacyBiomes.length != requiredSize) {
				throw new IllegalArgumentException("biomes array must have a length of " + requiredSize);
			}
		}
		this.legacyBiomes = legacyBiomes;
	}

	/** {@inheritDoc} */
	@Override
	public int getWorldMinBlockY() {
		return getChunkY() * 16;
	}

	/**
	 * @return The height maps of this chunk.
	 */
	public CompoundTag getHeightMaps() {
		return heightMaps;
	}

	/**
	 * Sets the height maps of this chunk.
	 * @param heightMaps The height maps.
	 */
	public void setHeightMaps(CompoundTag heightMaps) {
		checkRaw();
		this.heightMaps = heightMaps;
	}

	/**
	 * 256 (16x16) values. Values are shifted to read as block-y value. A value of {@link #getWorldMinBlockY()} - 1
	 * indicates no block present (void).
	 * @param name typically one of
	 * <ul>
	 *      <li>MOTION_BLOCKING</li>
	 *  	<li>MOTION_BLOCKING_NO_LEAVES</li>
	 *  	<li>OCEAN_FLOOR</li>
	 *  	<li>OCEAN_FLOOR_WG</li>
	 *  	<li>WORLD_SURFACE</li>
	 *  	<li>WORLD_SURFACE_WG</li>
	 * </ul>
	 * @return {@link LongArrayTagPackedIntegers} configured to yield block Y values.
	 * @since {@link DataVersion#JAVA_1_13_18W06A}
	 */
	public LongArrayTagPackedIntegers getHeightMap(String name) {
		if (getHeightMaps() == null)
			return null;
		var hm = getHeightMaps().getLongArrayTag(name);
		if (hm == null)
			return null;
		final int minY = getWorldMinBlockY() - 1;
		final int maxY = getWorldMaxBlockY();
		return LongArrayTagPackedIntegers.builder()
				.dataVersion(dataVersion)
				.minBitsPerValue(Math.max(9, LongArrayTagPackedIntegers.calculateBitsRequired(maxY - minY)))
				.valueOffset(minY)
				.length(256)
				.build(hm);
	}

	public IntArrayTag getLegacyHeightMap() {
		return legacyHeightMap;
	}

	public void setLegacyHeightMap(IntArrayTag legacyHeightMap) {
		this.legacyHeightMap = legacyHeightMap;
	}

	/**
	 * Returns a copy of the palette value at the specified position in this chunk.
	 * <p>Modifying the returned value can be done safely, it will have no effect on this chunk.</p>
	 * <p>To avoid the overhead of making a copy use {@link #getBiomeAtByRef(int, int, int)} instead.</p>
	 *
	 * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
	 * @return the element at the specified position in this chunk or NULL if Y is above/below build height.
	 * @since {@link DataVersion#JAVA_1_18_21W37A}
	 */
	public StringTag getBiomeAt(int x, int y, int z) {
		checkRaw();
		if (dataVersion < JAVA_1_18_21W37A.id())
			throw new VersionLacksSupportException(dataVersion, JAVA_1_18_21W37A, null, "3D palette biomes");
		var section = getSection(y / 16);
		if (section == null) return null;
		return section.getBiomes().get((x & 0xF) / 4, (y & 0xF) / 4, (z & 0xF) / 4);
	}

	/**
	 * Returns the palette value at the specified position in this chunk.
	 * <p><b>WARNING if the returned value is modified it modifies every value which references the same palette
	 * entry within the same chunk section!</b></p>
	 *
	 * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
	 * @return the element at the specified position in this chunk or NULL if Y is above/below build height.
	 * @since {@link DataVersion#JAVA_1_18_21W37A}
	 */
	public StringTag getBiomeAtByRef(int x, int y, int z) {
		checkRaw();
		if (dataVersion < JAVA_1_18_21W37A.id())
			throw new VersionLacksSupportException(dataVersion, JAVA_1_18_21W37A, null, "3D palette biomes");
		var section = getSection(y / 16);
		if (section == null) return null;
		return section.getBiomes().getByRef((x & 0xF) / 4, (y & 0xF) / 4, (z & 0xF) / 4);
	}

	/**
	 * Replaces the element at the specified position in this chunk with
	 * the specified element.
	 *
	 * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
	 * @return true if the section existed and the biome was set (true even if the value was unchanged)
	 * @since {@link DataVersion#JAVA_1_18_21W37A}
	 */
	public boolean setBiomeAt(int x, int y, int z, StringTag tag) {
		checkRaw();
		if (dataVersion < JAVA_1_18_21W37A.id())
			throw new VersionLacksSupportException(dataVersion, JAVA_1_18_21W37A, null, "3D palette biomes");
		var section = getSection(y / 16);
		if (section != null) {
			section.getBiomes().set((x & 0xF) / 4, (y & 0xF) / 4, (z & 0xF) / 4, tag);
			return true;
		}
		return false;
	}

	/**
	 * Returns a copy of the block palette value at the specified position in this chunk.
	 * <p>Modifying the returned value can be done safely, it will have no effect on this chunk.</p>
	 * <p>To avoid the overhead of making a copy use {@link #getBlockAtByRef(int, int, int)} instead.</p>
	 *
	 * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
	 * @return the element at the specified position in this chunk or NULL if Y is above/below build height.
	 * @since {@link DataVersion#JAVA_1_13_17W47A}
	 * @see BlockStateTag
	 */
	public CompoundTag getBlockAt(int x, int y, int z) {
		checkRaw();
		if (dataVersion < JAVA_1_13_17W47A.id())
			throw new VersionLacksSupportException(dataVersion, JAVA_1_13_17W47A, null, "block palettes");
		var section = getSection(y / 16);
		if (section == null) return null;
		var bs = section.getBlockStates();
		return bs != null ? bs.get(x & 0xF, y & 0xF, z & 0xF) : null;
	}

	/**
	 * Returns the block palette value at the specified position in this chunk.
	 * <p><b>WARNING if the returned value is modified it modifies every value which references the same palette
	 * entry within the same chunk section!</b></p>
	 *
	 * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
	 * @return the element at the specified position in this chunk or NULL if Y is above/below build height.
	 * @since {@link DataVersion#JAVA_1_13_17W47A}
	 * @see BlockStateTag
	 */
	public CompoundTag getBlockAtByRef(int x, int y, int z) {
		checkRaw();
		if (dataVersion < JAVA_1_13_17W47A.id())
			throw new VersionLacksSupportException(dataVersion, JAVA_1_13_17W47A, null, "block palettes");
		var section = getSection(y / 16);
		if (section == null) return null;
		var bs = section.getBlockStates();
		return bs != null ? bs.getByRef(x & 0xF, y & 0xF, z & 0xF) : null;
	}

	/** nullable */
	public String getBlockNameAt(int x, int y, int z) {
		CompoundTag blockTag = getBlockAtByRef(x, y, z);
		return blockTag != null ? blockTag.getString("Name") : null;
	}

	/**
	 * Sets the block at the specified location to be defined by tag.
	 *
	 * <p>Never throws IndexOutOfBoundsException. XYZ are always wrapped into bounds.</p>
	 * @param tag block palette tag, must contain a 'Name' StringTag
	 * @return true if the section existed and the block was set (true even if the value was unchanged)
	 * @since {@link DataVersion#JAVA_1_13_17W47A}
	 * @see BlockStateTag
	 */
	public boolean setBlockAt(int x, int y, int z, CompoundTag tag) {
		checkRaw();
		if (dataVersion < JAVA_1_13_17W47A.id())
			throw new VersionLacksSupportException(dataVersion, JAVA_1_13_17W47A, null, "block palettes");
		ArgValidator.check(tag.containsKey("Name", StringTag.class), "block palette tag must contain a 'Name' StringTag");
		var section = getSection(y / 16);
		if (section != null) {
			section.getBlockStates().set(x & 0xF, y & 0xF, z & 0xF, tag);
			return true;
		}
		return false;
	}

	/**
	 * @return The carving masks of this chunk.
	 */
	public CompoundTag getCarvingMasks() {
		return carvingMasks;
	}

	/**
	 * Sets the carving masks of this chunk.
	 * @param carvingMasks The carving masks.
	 */
	public void setCarvingMasks(CompoundTag carvingMasks) {
		checkRaw();
		this.carvingMasks = carvingMasks;
	}

	/**
	 * @return The entities of this chunk. May be null.
	 */
	public ListTag<CompoundTag> getEntities() {
		return entities;
	}

	/**
	 * Sets the entities of this chunk.
	 * @param entities The entities.
	 */
	public void setEntities(ListTag<CompoundTag> entities) {
		checkRaw();
		this.entities = entities;
	}

	/**
	 * @return The tile entities of this chunk.
	 */
	public ListTag<CompoundTag> getTileEntities() {
		return tileEntities;
	}

	/**
	 * Sets the tile entities of this chunk.
	 * @param tileEntities The tile entities of this chunk.
	 */
	public void setTileEntities(ListTag<CompoundTag> tileEntities) {
		checkRaw();
		this.tileEntities = tileEntities;
	}

	/**
	 * @return The tile ticks of this chunk.
	 */
	public ListTag<CompoundTag> getTileTicks() {
		return tileTicks;
	}

	/**
	 * Sets the tile ticks of this chunk.
	 * @param tileTicks Thee tile ticks.
	 */
	public void setTileTicks(ListTag<CompoundTag> tileTicks) {
		checkRaw();
		this.tileTicks = tileTicks;
	}

	/**
	 * @return The liquid ticks of this chunk.
	 */
	public ListTag<CompoundTag> getLiquidTicks() {
		return liquidTicks;
	}

	/**
	 * Sets the liquid ticks of this chunk.
	 * @param liquidTicks The liquid ticks.
	 */
	public void setLiquidTicks(ListTag<CompoundTag> liquidTicks) {
		checkRaw();
		this.liquidTicks = liquidTicks;
	}

	/**
	 * @return The light sources in this chunk.
	 */
	public ListTag<ListTag<?>> getLights() {
		return lights;
	}

	/**
	 * Sets the light sources in this chunk.
	 * @param lights The light sources.
	 */
	public void setLights(ListTag<ListTag<?>> lights) {
		checkRaw();
		this.lights = lights;
	}

	/**
	 * @return The liquids to be ticked in this chunk.
	 */
	public ListTag<ListTag<?>> getLiquidsToBeTicked() {
		return liquidsToBeTicked;
	}

	/**
	 * Sets the liquids to be ticked in this chunk.
	 * @param liquidsToBeTicked The liquids to be ticked.
	 */
	public void setLiquidsToBeTicked(ListTag<ListTag<?>> liquidsToBeTicked) {
		checkRaw();
		this.liquidsToBeTicked = liquidsToBeTicked;
	}

	/**
	 * @return Stuff to be ticked in this chunk.
	 */
	public ListTag<ListTag<?>> getToBeTicked() {
		return toBeTicked;
	}

	/**
	 * Sets stuff to be ticked in this chunk.
	 * @param toBeTicked The stuff to be ticked.
	 */
	public void setToBeTicked(ListTag<ListTag<?>> toBeTicked) {
		checkRaw();
		this.toBeTicked = toBeTicked;
	}

	/**
	 * @return Things that are in post processing in this chunk.
	 */
	public ListTag<ListTag<?>> getPostProcessing() {
		return postProcessing;
	}

	/**
	 * Sets things to be post processed in this chunk.
	 * @param postProcessing The things to be post processed.
	 */
	public void setPostProcessing(ListTag<ListTag<?>> postProcessing) {
		checkRaw();
		this.postProcessing = postProcessing;
	}

	/**
	 * @return Data about structures in this chunk.
	 */
	public CompoundTag getStructures() {
		return structures;
	}

	/**
	 * Sets data about structures in this chunk.
	 * @param structures The data about structures.
	 */
	public void setStructures(CompoundTag structures) {
		checkRaw();
		this.structures = structures;
	}

	/**
	 * Gets the world-bottom section y in the chunk.
	 */
	public int getChunkY() {
		if (yPos != NO_CHUNK_COORD_SENTINEL) return yPos;
		return DEFAULT_WORLD_BOTTOM_Y_POS.get(dataVersion);
	}

	/**
	 * @since {@link DataVersion#JAVA_1_18_21W43A}
	 */
	public CompoundTag getBelowZeroRetrogen() {
		return belowZeroRetrogen;
	}

	/**
	 * @since {@link DataVersion#JAVA_1_18_21W43A}
	 */
	public CompoundTag getBlendingData() {
		return blendingData;
	}


	/** {@inheritDoc} */
	@Override
	public boolean moveChunkImplemented() {
		return raw || ((this.chunkX != NO_CHUNK_COORD_SENTINEL && this.chunkZ != NO_CHUNK_COORD_SENTINEL) &&
				(data != null || (getStructures() != null && getTileEntities() != null && getTileTicks() != null && getLiquidTicks() != null)));
	}

	/** {@inheritDoc} */
	@Override
	public boolean moveChunkHasFullVersionSupport() {
		// TODO: Only strongly validated at 1.20.4 - but I believe all versions are supported but should validate
		return true;
	}

	// For RAW support
	private ListTag<CompoundTag> tagOrFetch(ListTag<CompoundTag> tag, VersionAware<NbtPath> path) {
		if (tag != null) return tag;
		return getTag(path);
	}
	private CompoundTag tagOrFetch(CompoundTag tag, VersionAware<NbtPath> path) {
		if (tag != null) return tag;
		return getTag(path);
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public boolean moveChunk(int newChunkX, int newChunkZ, long moveChunkFlags, boolean force) {
		if (!moveChunkImplemented())
			throw new UnsupportedOperationException("Missing the data required to move this chunk!");
		if (!RegionBoundingRectangle.MAX_WORLD_BORDER_BOUNDS.containsChunk(chunkX, chunkZ)) {
			throw new IllegalArgumentException("Chunk XZ must be within the maximum world bounds.");
		}
		if (this.chunkX == newChunkX && this.chunkZ == newChunkZ) return false;

		IntPointXZ chunkDeltaXZ;
		if (raw) {
			// read the old data values so we can compute deltaXZ
			this.chunkX = getTagValue(X_POS_PATH, IntTag::asInt);
			this.chunkZ = getTagValue(Z_POS_PATH, IntTag::asInt);
			setTag(X_POS_PATH, new IntTag(newChunkX));
			setTag(Z_POS_PATH, new IntTag(newChunkZ));
		}
		chunkDeltaXZ = new IntPointXZ(newChunkX - this.chunkX, newChunkZ - this.chunkZ);
		this.chunkX = newChunkX;
		this.chunkZ = newChunkZ;

		boolean changed = false;
		ChunkBoundingRectangle cbr = new ChunkBoundingRectangle(newChunkX, newChunkZ);
		changed |= fixTileLocations(moveChunkFlags, cbr, tagOrFetch(getTileEntities(), TILE_ENTITIES_PATH));
		changed |= fixTileLocations(moveChunkFlags, cbr, tagOrFetch(getTileTicks(), TILE_TICKS_PATH));
		changed |= fixTileLocations(moveChunkFlags, cbr, tagOrFetch(getLiquidTicks(), LIQUID_TICKS_PATH));
		changed |= moveStructures(moveChunkFlags, tagOrFetch(getStructures(), STRUCTURES_PATH), chunkDeltaXZ);

		CompoundTag upgradeTag = tagOrFetch(getUpgradeData(), UPGRADE_DATA_PATH);
		if (upgradeTag != null && !upgradeTag.isEmpty()) {
			if ((moveChunkFlags & DISCARD_UPGRADE_DATA) > 0) {
				upgradeTag.clear();
				changed = true;
			} else {
				for (NamedTag entry : upgradeTag) {
					if (entry.getTag() instanceof ListTag && ((ListTag<?>) entry.getTag()).getTypeClass().equals(CompoundTag.class)) {
						changed |= fixTileLocations(moveChunkFlags, cbr, (ListTag<CompoundTag>) entry.getTag());
					}
				}
			}
		}
		changed |= fixEntitiesLocations(moveChunkFlags, cbr, tagOrFetch(getEntities(), ENTITIES_PATH));

		if (changed) {
			if ((moveChunkFlags & MoveChunkFlags.AUTOMATICALLY_UPDATE_HANDLE) > 0) {
				updateHandle();
			}
			return true;
		}
		return false;

	}

	protected boolean fixEntitiesLocations(long moveChunkFlags, ChunkBoundingRectangle cbr, ListTag<CompoundTag> entitiesTagList) {
		return EntitiesChunkBase.fixEntityLocations(dataVersion, moveChunkFlags, entitiesTagList, cbr);
	}

	protected boolean fixTileLocations(long moveChunkFlags, ChunkBoundingRectangle cbr, ListTag<CompoundTag> tagList) {
		boolean changed = false;
		if (tagList == null) {
			return false;
		}
		for (CompoundTag tag : tagList) {
			int x = tag.getInt("x");
			int z = tag.getInt("z");
			if (!cbr.containsBlock(x, z)) {
				changed = true;
				tag.putInt("x", cbr.relocateX(x));
				tag.putInt("z", cbr.relocateZ(z));
			}
		}
		return changed;
	}

	private static final long REMOVE_SENTINEL = 0x8FFFFFFF_8FFFFFFFL;
	protected boolean moveStructures(long moveChunkFlags, CompoundTag structuresTag, IntPointXZ chunkDeltaXZ) {
		final CompoundTag references = STRUCTURES_REFERENCES_PATH.get(dataVersion).get(structuresTag);
		final CompoundTag starts = STRUCTURES_STARTS_PATH.get(dataVersion).get(structuresTag);
		boolean changed = false;

		// Discard structures if directed to do so.
		if ((moveChunkFlags & DISCARD_STRUCTURE_DATA) > 0) {
			if (references != null && !references.isEmpty()) {
				references.clear();
				changed = true;
			}
			if (starts != null && !starts.isEmpty()) {
				starts.clear();
				changed = true;
			}
			return changed;
		}

		// Establish regional bounds iff we are to discard out of region zones
		final ChunkBoundingRectangle clippingRect;
		if ((moveChunkFlags & DISCARD_STRUCTURE_REFERENCES_OUTSIDE_REGION) > 0) {
			clippingRect = RegionBoundingRectangle.forChunk(chunkX, chunkZ);
		} else {
			clippingRect = null;
		}

		// Fix structure reference locations (XZ packed into a long)
		if (references != null && !references.isEmpty()) {
			for (Tag<?> tag : references.values()) {
				boolean haveRemovals = false;
				long[] longs = ((LongArrayTag) tag).getValue();
				for (int i = 0; i < longs.length; i++) {
					IntPointXZ newXZ = IntPointXZ.unpack(longs[i]).add(chunkDeltaXZ);
					if (clippingRect != null && !clippingRect.containsChunk(newXZ)) {
						longs[i] = REMOVE_SENTINEL;
						haveRemovals = true;
					} else {
						longs[i] = IntPointXZ.pack(newXZ);
					}
				}
				if (haveRemovals) {
					((LongArrayTag) tag).setValue(
							Arrays.stream(longs)
									.filter(l -> l != REMOVE_SENTINEL)
									.toArray()
					);
				}
			}
			changed = true;
		}

		// Iterate and fix structure 'starts' - starts define the area a structure does, or will, occupy
		// and defines what exists in each, what I'll call, zone of that structure.
		if (starts != null && !starts.isEmpty()) {
			IntPointXZ blockDeltaXZ = chunkDeltaXZ.transformChunkToBlock();
			for (NamedTag startsEntry : starts) {
				moveStructureStart((CompoundTag) startsEntry.getTag(), chunkDeltaXZ, blockDeltaXZ, clippingRect);
			}
		}
		return changed;
	}

	/**
	 * NOTE: The given boundsTag tag will be emptied (have a new length of zero) if the move results in an
	 * out-of-bounds BB (rbr must be non-null for this to happen). In such a case true is always returned.
	 * So, if true is returned the caller MUST check the length of the boundsTag and take appropriate action!
	 * @return true if bounds changed
	 */
	protected static boolean moveBoundingBox(IntArrayTag boundsTag, IntPointXZ blockDeltaXZ, ChunkBoundingRectangle clippingRect) {
		boolean changed = false;
		if (boundsTag != null) {
			int[] bounds = boundsTag.getValue();
			if (!blockDeltaXZ.isZero()) {
				bounds[0] = bounds[0] + blockDeltaXZ.getX();
				bounds[2] = bounds[2] + blockDeltaXZ.getZ();
				bounds[3] = bounds[3] + blockDeltaXZ.getX();
				bounds[5] = bounds[5] + blockDeltaXZ.getZ();
				changed = true;
			}
			if (clippingRect != null && !clippingRect.constrain(bounds)) {
				boundsTag.setValue(new int[0]);
				return true;
			}
		}
		return changed;
	}

	/**
	 * Moves a single structure start record.
	 * @see <a href=https://minecraft.fandom.com/wiki/Chunk_format>wiki Chunk_format</a>
	 */
	@SuppressWarnings("unchecked")
	protected boolean moveStructureStart(CompoundTag startsTag, IntPointXZ chunkDeltaXZ, IntPointXZ blockDeltaXZ, ChunkBoundingRectangle clippingRect) {
		if ("INVALID".equals(startsTag.getString("id"))) return false;
		boolean changed = false;

		// If the overall bounding box is invalid then discard and invalidate the entire structure.
		// I don't see how this scenario is possible in practice for well formatted chunks.
		// FYI the BB tag doesn't exist for all structures at this level.
		IntArrayTag startsBbTag = startsTag.getIntArrayTag("BB");
		if (moveBoundingBox(startsBbTag, blockDeltaXZ, clippingRect)) {
			if (startsBbTag.length() == 0) {
				startsTag.clear();
				startsTag.putString("id", "INVALID");
				return true;
			}
			changed = true;
		}

		if (startsTag.containsKey("ChunkX") && chunkDeltaXZ.getX() != 0) {
			startsTag.putInt("ChunkX", chunkDeltaXZ.getX() + startsTag.getInt("ChunkX"));
			changed = true;
		}
		if (startsTag.containsKey("ChunkZ") && chunkDeltaXZ.getZ() != 0) {
			startsTag.putInt("ChunkZ", chunkDeltaXZ.getZ() + startsTag.getInt("ChunkZ"));
			changed = true;
		}

		// List of chunks that have had their piece of the structure created.
		// Unsure when this tag shows up - maybe during generation - maybe only for specific structures.
		if (startsTag.containsKey("Processed")) {
			ListTag<CompoundTag> processedListTag = startsTag.getCompoundList("Processed");
			Iterator<CompoundTag> processedIter = processedListTag.iterator();
			while (processedIter.hasNext()) {
				CompoundTag processedTag = processedIter.next();

				if (processedTag.containsKey("X") && blockDeltaXZ.getX() != 0) {
					processedTag.putInt("X", blockDeltaXZ.getX() + processedTag.getInt("X"));
					changed = true;
				}
				if (processedTag.containsKey("Z") && blockDeltaXZ.getZ() != 0) {
					processedTag.putInt("Z", blockDeltaXZ.getZ() + processedTag.getInt("Z"));
					changed = true;
				}
				if (clippingRect != null && !clippingRect.containsBlock(processedTag.getInt("X"), processedTag.getInt("Z"))) {
					processedIter.remove();
					changed = true;
				}
			}
		}

		if (startsTag.containsKey("Children")) {
			ListTag<CompoundTag> childrenListTag = startsTag.getCompoundList("Children");
			Iterator<CompoundTag> childIter = childrenListTag.iterator();
			while (childIter.hasNext()) {
				CompoundTag childTag = childIter.next();
				// bounding box of structure part - note some block geometry may overhang these bounds (like roofs)
				IntArrayTag childBbTag = childTag.getIntArrayTag("BB");
				if (moveBoundingBox(childBbTag, blockDeltaXZ, clippingRect)) {
					changed = true;
					if (childBbTag.length() == 0) {
						childIter.remove();
						continue;
					}
				}

				// List of entrances/exits from the room. Probably for structure generation to know
				// where additional structure parts can be placed to continue to grow the structure.
				if (childTag.containsKey("Entrances")) {
					ListTag<IntArrayTag> entrancesListTag = childTag.getListTagAutoCast("Entrances");
					Iterator<IntArrayTag> entranceIter = entrancesListTag.iterator();
					while (entranceIter.hasNext()) {
						IntArrayTag entranceBbTag = entranceIter.next();
						if (moveBoundingBox(entranceBbTag, blockDeltaXZ, clippingRect)) {
							changed = true;
							if (entranceBbTag.length() == 0) {
								entranceIter.remove();
							}
						}
					}
				}

				// coordinate origin of structure part
				if (childTag.containsKey("PosX") && blockDeltaXZ.getX() != 0) {
					childTag.putInt("PosX", blockDeltaXZ.getX() + childTag.getInt("PosX"));
					changed = true;
				}
				if (childTag.containsKey("PosZ") && blockDeltaXZ.getZ() != 0) {
					childTag.putInt("PosZ", blockDeltaXZ.getZ() + childTag.getInt("PosZ"));
					changed = true;
				}

				// coordinate origin of ocean ruin or shipwreck
				if (childTag.containsKey("TPX") && blockDeltaXZ.getX() != 0) {
					childTag.putInt("TPX", blockDeltaXZ.getX() + childTag.getInt("TPX"));
					changed = true;
				}
				if (childTag.containsKey("TPZ") && blockDeltaXZ.getZ() != 0) {
					childTag.putInt("TPZ", blockDeltaXZ.getZ() + childTag.getInt("TPZ"));
					changed = true;
				}

				// Anything using jigsaw blocks has a 'junctions' record
				if (childTag.containsKey("junctions")) {
					ListTag<CompoundTag> junctionsListTag = childTag.getCompoundList("junctions");
					Iterator<CompoundTag> junctionIter = junctionsListTag.iterator();
					while (junctionIter.hasNext()) {
						CompoundTag junctionTag = junctionIter.next();
						if (blockDeltaXZ.getX() != 0) {
							junctionTag.putInt("source_x", blockDeltaXZ.getX() + junctionTag.getInt("source_x"));
							changed = true;
						}
						if (blockDeltaXZ.getZ() != 0) {
							junctionTag.putInt("source_z", blockDeltaXZ.getZ() + junctionTag.getInt("source_z"));
							changed = true;
						}
						if (clippingRect != null && !clippingRect.containsBlock(junctionTag.getInt("source_x"), junctionTag.getInt("source_z"))) {
							junctionIter.remove();
							changed = true;
						}
					}
					// TODO: unsure how to behave if the junctions list is emptied - maybe remove the child?
					//   Scenario to look for: village/pillager outpost that abuts a region bound and has a junction
					//   crossing that bound where on one side there's a leaf node BB that hangs over the bound
					//   and only has junctions into one of the two regions.
				}
			}
			// TODO: unsure if this logic is needed, or if there are structures which have no Children, so leaving it out for now
//			if (childrenListTag.isEmpty() && !startsTag.containsKey("BB")) {
//				startsTag.clear();
//				startsTag.putString("id", "INVALID");
//				return true;
//			}
		}

		return changed;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CompoundTag updateHandle() {
		if (raw) {
			return data;
		}
		this.data = super.updateHandle();
		setTag(LAST_UPDATE_TICK_PATH, new LongTag(lastUpdateTick));
		setTag(INHABITED_TIME_TICKS_PATH, new LongTag(inhabitedTimeTicks));
		if (legacyBiomes != null && dataVersion < JAVA_1_18_21W37A.id()) {
			final int requiredSize = dataVersion <= 0 || dataVersion >= JAVA_1_15_19W36A.id() ? 1024 : 256;
			if (legacyBiomes.length != requiredSize)
				throw new IllegalStateException(
						String.format("Biomes array must be %d bytes for version %d, array size is %d",
								requiredSize, dataVersion, legacyBiomes.length));

			if (dataVersion >= DataVersion.JAVA_1_13_18W06A.id()) {
				setTag(LEGACY_BIOMES_PATH, new IntArrayTag(legacyBiomes));
			} else {
				byte[] byteBiomes = new byte[legacyBiomes.length];
				for (int i = 0; i < legacyBiomes.length; i++) {
					byteBiomes[i] = (byte) legacyBiomes[i];
				}
				setTag(LEGACY_BIOMES_PATH, new ByteArrayTag(byteBiomes));
			}
		}
		setTagIfNotNull(LEGACY_HEIGHT_MAP_PATH, legacyHeightMap);
		setTagIfNotNull(HEIGHT_MAPS_PATH, heightMaps);
		setTagIfNotNull(CARVING_MASKS_PATH, carvingMasks);
		setTagIfNotNull(ENTITIES_PATH, entities);
		setTagIfNotNull(TILE_ENTITIES_PATH, tileEntities);
		setTagIfNotNull(TILE_TICKS_PATH, tileTicks);
		setTagIfNotNull(LIQUID_TICKS_PATH, liquidTicks);
		setTagIfNotNull(LIGHTS_PATH, lights);
		setTagIfNotNull(LIQUIDS_TO_BE_TICKED_PATH, liquidsToBeTicked);
		setTagIfNotNull(TO_BE_TICKED_PATH, toBeTicked);
		setTagIfNotNull(POST_PROCESSING_PATH, postProcessing);
		if (status != null) setTag(STATUS_PATH, new StringTag(status));
		if (isLightOn != null) setTag(IS_LIGHT_ON_PATH, new ByteTag(isLightOn));
		if (isTerrainPopulated != null) setTag(TERRAIN_POPULATED_PATH, new ByteTag(isTerrainPopulated));
		setTagIfNotNull(STRUCTURES_PATH, structures);
		if (hasLegacyStructureData != null) setTag(HAS_LEGACY_STRUCTURE_DATA_PATH, new ByteTag(hasLegacyStructureData));

		// TODO: This logic does not respect original load flags! However, this is a long standing bug so
		// 		 simply "fixing" it may break consumers... I no longer care about existing consumers and
		//       need to figure out what that "fix" I was referring to was -.-
		ListTag<CompoundTag> sections = new ListTag<>(CompoundTag.class);
		for (T section : this) {
			if (section != null) {
				sections.add(section.updateHandle());  // contract of iterator assures correctness of "height" aka section-y
			}
		}
		setTag(SECTIONS_PATH, sections);

		setTag(X_POS_PATH, new IntTag(getChunkX()));
		setTag(Z_POS_PATH, new IntTag(getChunkZ()));
		if (dataVersion >= JAVA_1_18_21W43A.id()) {
			setTag(Y_POS_PATH, new IntTag(getChunkY()));
			setTagIfNotNull(BELOW_ZERO_RETROGEN_PATH, belowZeroRetrogen);
			setTagIfNotNull(BLENDING_DATA_PATH, blendingData);
		}
		return data;
	}

	@Override
	public CompoundTag updateHandle(int xPos, int zPos) {
		if (raw) {
			return data;
		}
		// TODO: moveChunk or die if given xPos != chunkX and same for z?
		updateHandle();
		if (xPos != NO_CHUNK_COORD_SENTINEL)
			setTag(X_POS_PATH, new IntTag(chunkX = xPos));
		// Y_POS_PATH is set in updateHandle() - was added in 1.18
		if (zPos != NO_CHUNK_COORD_SENTINEL)
			setTag(Z_POS_PATH, new IntTag(chunkZ = zPos));
		return data;
	}
}
