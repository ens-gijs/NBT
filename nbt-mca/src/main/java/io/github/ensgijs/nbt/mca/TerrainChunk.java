package io.github.ensgijs.nbt.mca;

import io.github.ensgijs.nbt.tag.CompoundTag;

/**
 * Represents a TERRAIN data mca chunk (from mca files that come from the /region save folder).
 * Terrain chunks are composed of a set of {@link TerrainSection} where any empty/null
 * section is filled with air blocks by the game. When altering existing chunks for MC 1.14+, be sure to have read and
 * understood the documentation on {@link PoiRecord} to avoid problems with villagers, nether portal linking,
 * lodestones, bees, and probably more as Minecraft continues to evolve.
 *
 * <p><i>It is my (Ross / Ens) hope that in the future this class can be repurposed to serve as an abstraction
 * layer over all the various chunk types (terrain, poi, entity - at the time of writing) and that it
 * can take care of keeping them all in sync. But I've already put a lot of time into this library and need
 * to return to other things so for now that goal must remain unrealized.</i></p>
 */
public class TerrainChunk extends TerrainChunkBase<TerrainSection> {
	/**
	 * The default chunk data version used when no custom version is supplied.
	 * @deprecated Use {@code DataVersion.latest().id()} instead.
	 */
	@Deprecated
	public static final int DEFAULT_DATA_VERSION = DataVersion.latest().id();

	protected TerrainChunk(int dataVersion) {
		super(dataVersion);
	}

	public TerrainChunk(CompoundTag data) {
		super(data);
	}

	public TerrainChunk(CompoundTag data, long loadFlags) {
		super(data, loadFlags);
	}

	public TerrainChunk() {
		super(DataVersion.latest().id());
		data = new CompoundTag();
	}

	@Override
	protected TerrainSection createSection(CompoundTag section, int dataVersion, long loadFlags) {
		return new TerrainSection(section, dataVersion, loadFlags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TerrainSection createSection(int sectionY) throws IllegalArgumentException {
		if (containsSection(sectionY)) throw new IllegalArgumentException("section already exists at section-y " + sectionY);
		TerrainSection section = new TerrainSection(dataVersion);
		if (sectionY != SectionBase.NO_SECTION_Y_SENTINEL) {
			putSection(sectionY, section);  // sets section height & validates range
		}
		return section;
	}

	public TerrainSection createSection() {
		return createSection(SectionBase.NO_SECTION_Y_SENTINEL);
	}

	/**
	 * @deprecated Dangerous - assumes the latest full release data version defined by {@link DataVersion}
	 * prefer using {@link McaFileBase#createChunk()} or {@link McaFileBase#createChunkIfMissing(int, int)}.
	 */
	@Deprecated
	public static TerrainChunk newChunk() {
		return newChunk(DataVersion.latest().id());
	}

	public static TerrainChunk newChunk(int dataVersion) {
		TerrainChunk c = new TerrainChunk(dataVersion);
		c.data = new CompoundTag();
		if (dataVersion < DataVersion.JAVA_1_18_21W39A.id()) {
			c.data.put("Level", new CompoundTag());
		}
		c.status = "mobs_spawned";
		return c;
	}
}
