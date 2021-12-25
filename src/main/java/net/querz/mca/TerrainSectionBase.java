package net.querz.mca;

import net.querz.nbt.tag.ByteArrayTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.querz.mca.DataVersion.JAVA_1_16_20W17A;
import static net.querz.mca.LoadFlags.*;

/**
 * Provides the base for all terrain section classes.
 * <p>
 * TODO: in 1.18+ ({@link DataVersion#JAVA_1_18_21W43A} specifically) biomes were paletted and moved into terrain sections
 * </p>
 */
public abstract class TerrainSectionBase extends SectionBase<TerrainSectionBase> {

	protected Map<String, List<PaletteIndex>> valueIndexedPalette = new HashMap<>();
	protected ListTag<CompoundTag> palette;
	protected byte[] blockLight;
	protected long[] blockStates;
	protected byte[] skyLight;

	public static byte[] createBlockLightBuffer() {
		return new byte[2048];
	}

	public static long[] createBlockStates() {
		return new long[256];
	}

	public static byte[] createSkyLightBuffer() {
		return new byte[2048];
	}

	public TerrainSectionBase(CompoundTag sectionRoot, int dataVersion) {
		this(sectionRoot, dataVersion, ALL_DATA);
	}

	public TerrainSectionBase(CompoundTag sectionRoot, int dataVersion, long loadFlags) {
		super(sectionRoot, dataVersion);
		if (dataVersion <= 0) {
			throw new IllegalArgumentException("Invalid data version - must be GT 0");
		}
		height = sectionRoot.getNumber("Y").byteValue();

		ListTag<?> rawPalette = sectionRoot.getListTag("Palette");
		if (rawPalette == null) {
			return;
		}
		palette = rawPalette.asCompoundTagList();
		for (int i = 0; i < palette.size(); i++) {
			CompoundTag data = palette.get(i);
			putValueIndexedPalette(data, i);
		}

		if ((loadFlags & BLOCK_LIGHTS) != 0) {
			ByteArrayTag blockLight = sectionRoot.getByteArrayTag("BlockLight");
			if (blockLight != null) this.blockLight = blockLight.getValue();
		}
		if ((loadFlags & BLOCK_STATES) != 0) {
			LongArrayTag blockStates = sectionRoot.getLongArrayTag("BlockStates");
			if (blockStates != null) this.blockStates = blockStates.getValue();
		}
		if ((loadFlags & SKY_LIGHT) != 0) {
			ByteArrayTag skyLight = sectionRoot.getByteArrayTag("SkyLight");
			if (skyLight != null) this.skyLight = skyLight.getValue();
		}
	}

	public TerrainSectionBase(int dataVersion) {
		super(dataVersion);
		blockLight = createBlockLightBuffer();
		blockStates = createBlockStates();
		skyLight = createSkyLightBuffer();
		palette = new ListTag<>(CompoundTag.class);
		CompoundTag air = new CompoundTag();
		air.putString("Name", "minecraft:air");
		palette.add(air);
	}

	private void assureBlockStates() {
		if (blockStates == null) blockStates = createBlockStates();
	}

	private void assurePalette() {
		if (palette == null) palette = new ListTag<>(CompoundTag.class);
	}
	
	void putValueIndexedPalette(CompoundTag data, int index) {
		PaletteIndex leaf = new PaletteIndex(data, index);
		String name = data.getString("Name");
		List<PaletteIndex> leaves = valueIndexedPalette.get(name);
		if (leaves == null) {
			leaves = new ArrayList<>(1);
			leaves.add(leaf);
			valueIndexedPalette.put(name, leaves);
		} else {
			for (PaletteIndex pal : leaves) {
				if (pal.data.equals(data)) {
					return;
				}
			}
			leaves.add(leaf);
		}
	}

	PaletteIndex getValueIndexedPalette(CompoundTag data) {
		List<PaletteIndex> leaves = valueIndexedPalette.get(data.getString("Name"));
		if (leaves == null) {
			return null;
		}
		for (PaletteIndex leaf : leaves) {
			if (leaf.data.equals(data)) {
				return leaf;
			}
		}
		return null;
	}

	private static class PaletteIndex {

		CompoundTag data;
		int index;

		PaletteIndex(CompoundTag data, int index) {
			this.data = data;
			this.index = index;
		}
	}

	/**
	 * Fetches a block state based on a block location from this section.
	 * The coordinates represent the location of the block inside of this Section.
	 * @param blockX The x-coordinate of the block in this Section
	 * @param blockY The y-coordinate of the block in this Section
	 * @param blockZ The z-coordinate of the block in this Section
	 * @return The block state data of this block.
	 */
	public CompoundTag getBlockStateAt(int blockX, int blockY, int blockZ) {
		return getBlockStateAt(getBlockIndex(blockX, blockY, blockZ));
	}

	private CompoundTag getBlockStateAt(int index) {
		int paletteIndex = getPaletteIndex(index);
		return palette.get(paletteIndex);
	}

	/**
	 * Attempts to add a block state for a specific block location in this Section.
	 * @param blockX The x-coordinate of the block in this Section
	 * @param blockY The y-coordinate of the block in this Section
	 * @param blockZ The z-coordinate of the block in this Section
	 * @param state The block state to be set
	 * @param cleanup When <code>true</code>, it will force a cleanup the palette of this section.
	 *                This option should only be used moderately to avoid unnecessary recalculation of the palette indices.
	 *                Recalculating the Palette should only be executed once right before saving the Section to file.
	 * @return True if {@link TerrainSectionBase#cleanupPaletteAndBlockStates()} was run as a result of this call.
	 * 		Note that it is possible that {@link TerrainSectionBase#cleanupPaletteAndBlockStates()} needed to be called even if
	 * 		the {@code cleanup} argument was {@code false}. In summary if the last call made to this function returns
	 * 		{@code true} you can skip the call to {@link TerrainSectionBase#cleanupPaletteAndBlockStates()}.
	 */
	public boolean setBlockStateAt(int blockX, int blockY, int blockZ, CompoundTag state, boolean cleanup) {
		assurePalette();
		int paletteSizeBefore = palette.size();
		int paletteIndex = addToPalette(state);
		//power of 2 --> bits must increase, but only if the palette size changed
		//otherwise we would attempt to update all blockstates and the entire palette
		//every time an existing blockstate was added while having 2^x blockstates in the palette
		if (paletteSizeBefore != palette.size() && (paletteIndex & (paletteIndex - 1)) == 0) {
			assureBlockStates();
			adjustBlockStateBits(null, blockStates);
			cleanup = true;
		}

		setPaletteIndex(getBlockIndex(blockX, blockY, blockZ), paletteIndex, blockStates);

		if (cleanup) {
			cleanupPaletteAndBlockStates();
			return true;
		}
		return false;
	}

	/**
	 * Returns the index of the block data in the palette.
	 * @param blockStateIndex The index of the block in this section, ranging from 0-4095.
	 * @return The index of the block data in the palette.
	 */
	public int getPaletteIndex(int blockStateIndex) {
		assureBlockStates();
		int bits = blockStates.length >> 6;

		if (dataVersion > 0 && dataVersion < JAVA_1_16_20W17A.id()) {
			double blockStatesIndex = blockStateIndex / (4096D / blockStates.length);
			int longIndex = (int) blockStatesIndex;
			int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);
			if (startBit + bits > 64) {
				long prev = bitRange(blockStates[longIndex], startBit, 64);
				long next = bitRange(blockStates[longIndex + 1], 0, startBit + bits - 64);
				return (int) ((next << 64 - startBit) + prev);
			} else {
				return (int) bitRange(blockStates[longIndex], startBit, startBit + bits);
			}
		} else {
			int indicesPerLong = (int) (64D / bits);
			int blockStatesIndex = blockStateIndex / indicesPerLong;
			int startBit = (blockStateIndex % indicesPerLong) * bits;
			return (int) bitRange(blockStates[blockStatesIndex], startBit, startBit + bits);
		}
	}

	/**
	 * Sets the index of the block data in the BlockStates. Does not adjust the size of the BlockStates array.
	 * @param blockIndex The index of the block in this section, ranging from 0-4095.
	 * @param paletteIndex The block state to be set (index of block data in the palette).
	 * @param blockStates The block states to be updated.
	 */
	public void setPaletteIndex(int blockIndex, int paletteIndex, long[] blockStates) {
		Objects.requireNonNull(blockStates, "blockStates must not be null");
		int bits = blockStates.length >> 6;

		if (dataVersion < JAVA_1_16_20W17A.id()) {
			double blockStatesIndex = blockIndex / (4096D / blockStates.length);
			int longIndex = (int) blockStatesIndex;
			int startBit = (int) ((blockStatesIndex - Math.floor(longIndex)) * 64D);
			if (startBit + bits > 64) {
				blockStates[longIndex] = updateBits(blockStates[longIndex], paletteIndex, startBit, 64);
				blockStates[longIndex + 1] = updateBits(blockStates[longIndex + 1], paletteIndex, startBit - 64, startBit + bits - 64);
			} else {
				blockStates[longIndex] = updateBits(blockStates[longIndex], paletteIndex, startBit, startBit + bits);
			}
		} else {
			int indicesPerLong = (int) (64D / bits);
			int blockStatesIndex = blockIndex / indicesPerLong;
			int startBit = (blockIndex % indicesPerLong) * bits;
			blockStates[blockStatesIndex] = updateBits(blockStates[blockStatesIndex], paletteIndex, startBit, startBit + bits);
		}
	}

	private void upgradeFromBefore20W17A(final int targetVersion) {
		int newBits = 32 - Integer.numberOfLeadingZeros(palette.size() - 1);
		newBits = Math.max(newBits, 4);
		long[] newBlockStates;

		int newLength = (int) Math.ceil(4096D / (Math.floor(64D / newBits)));
		newBlockStates = newBits == blockStates.length / 64 ? blockStates : new long[newLength];

		for (int i = 0; i < 4096; i++) {
			setPaletteIndex(i, getPaletteIndex(i), newBlockStates);
		}
		this.blockStates = newBlockStates;
	}

	@Override
	protected void syncDataVersion(int newDataVersion) {
		if (dataVersion < JAVA_1_16_20W17A.id() && newDataVersion >= JAVA_1_16_20W17A.id()) {
			upgradeFromBefore20W17A(newDataVersion);
		} else if (dataVersion >= JAVA_1_16_20W17A.id() && newDataVersion < JAVA_1_16_20W17A.id()) {
			throw new IllegalArgumentException(
					String.format("cannot downgrade data version from %d to %d because it crosses version %s",
							dataVersion, newDataVersion, JAVA_1_16_20W17A));
		}
		super.syncDataVersion(newDataVersion);
	}

	/**
	 * Fetches the palette of this Section.
	 * @return The palette of this Section.
	 */
	public ListTag<CompoundTag> getPalette() {
		return palette;
	}

	int addToPalette(CompoundTag data) {
		PaletteIndex index;
		if ((index = getValueIndexedPalette(data)) != null) {
			return index.index;
		}
		palette.add(data);
		putValueIndexedPalette(data, palette.size() - 1);
		return palette.size() - 1;
	}

	int getBlockIndex(int blockX, int blockY, int blockZ) {
		return (blockY & 0xF) * 256 + (blockZ & 0xF) * 16 + (blockX & 0xF);
	}

	static long updateBits(long n, long m, int i, int j) {
		//replace i to j in n with j - i bits of m
		long mShifted = i > 0 ? (m & ((1L << j - i) - 1)) << i : (m & ((1L << j - i) - 1)) >>> -i;
		return ((n & ((j > 63 ? 0 : (~0L << j)) | (i < 0 ? 0 : ((1L << i) - 1L)))) | mShifted);
	}

	static long bitRange(long value, int from, int to) {
		int waste = 64 - to;
		return (value << waste) >>> (waste + from);
	}

	/**
	 * This method recalculates the palette and its indices.
	 * This should only be used moderately to avoid unnecessary recalculation of the palette indices.
	 * Recalculating the Palette should only be executed once right before saving the Section to file.
	 */
	public void cleanupPaletteAndBlockStates() {
		if (blockStates != null && palette != null) {
			Map<Integer, Integer> oldToNewMapping = cleanupPalette();
			adjustBlockStateBits(oldToNewMapping, blockStates);
		}
	}

	private Map<Integer, Integer> cleanupPalette() {
		//create index - palette mapping
		Map<Integer, Integer> allIndices = new HashMap<>();
		for (int i = 0; i < 4096; i++) {
			int paletteIndex = getPaletteIndex(i);
			allIndices.put(paletteIndex, paletteIndex);
		}
		//delete unused blocks from palette
		//start at index 1 because we need to keep minecraft:air
		int index = 1;
		valueIndexedPalette = new HashMap<>(valueIndexedPalette.size());
		putValueIndexedPalette(palette.get(0), 0);
		for (int i = 1; i < palette.size(); i++) {
			if (!allIndices.containsKey(index)) {
				palette.remove(i);
				i--;
			} else {
				putValueIndexedPalette(palette.get(i), i);
				allIndices.put(index, i);
			}
			index++;
		}

		return allIndices;
	}

	void adjustBlockStateBits(Map<Integer, Integer> oldToNewMapping, long[] blockStates) {
		//increases or decreases the amount of bits used per BlockState
		//based on the size of the palette. oldToNewMapping can be used to update indices
		//if the palette had been cleaned up before using MCAFile#cleanupPalette().

		int newBits = 32 - Integer.numberOfLeadingZeros(palette.size() - 1);
		newBits = Math.max(newBits, 4);

		long[] newBlockStates;

		if (dataVersion < JAVA_1_16_20W17A.id()) {
			newBlockStates = newBits == blockStates.length / 64 ? blockStates : new long[newBits * 64];
		} else {
			int newLength = (int) Math.ceil(4096D / (Math.floor(64D / newBits)));
			newBlockStates = newBits == blockStates.length / 64 ? blockStates : new long[newLength];
		}
		if (oldToNewMapping != null) {
			for (int i = 0; i < 4096; i++) {
				setPaletteIndex(i, oldToNewMapping.get(getPaletteIndex(i)), newBlockStates);
			}
		} else {
			for (int i = 0; i < 4096; i++) {
				setPaletteIndex(i, getPaletteIndex(i), newBlockStates);
			}
		}
		this.blockStates = newBlockStates;
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
	 * @return The indices of the block states of this Section.
	 */
	public long[] getBlockStates() {
		return blockStates;
	}

	/**
	 * Sets the block state indices to a custom value.
	 * @param blockStates The block state indices.
	 * @throws NullPointerException If <code>blockStates</code> is <code>null</code>
	 * @throws IllegalArgumentException When <code>blockStates</code>' length is &lt; 256 or &gt; 4096 and is not a multiple of 64
	 */
	public void setBlockStates(long[] blockStates) {
		if (blockStates == null) {
			throw new NullPointerException("BlockStates cannot be null");
		} else if (blockStates.length % 64 != 0 || blockStates.length < 256 || blockStates.length > 4096) {
			throw new IllegalArgumentException("BlockStates must have a length > 255 and < 4097 and must be divisible by 64");
		}
		this.blockStates = blockStates;
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

	/**
	 * Updates the raw CompoundTag that this Section is based on.
	 * @return A reference to the raw CompoundTag this Section is based on
	 */
	@Override
	public CompoundTag updateHandle() {
		checkY(height);
		data.putByte("Y", (byte) height);
		if (palette != null) {
			data.put("Palette", palette);
		}
		if (blockLight != null) {
			data.putByteArray("BlockLight", blockLight);
		}
		if (blockStates != null) {
			data.putLongArray("BlockStates", blockStates);
		}
		if (skyLight != null) {
			data.putByteArray("SkyLight", skyLight);
		}
		return data;
	}

	/**
	 * Creates an iterable that iterates over all blocks in this section, in order of their indices.
	 * XYZ can be calculated with the following formulas:
	 * <pre>
	 * {@code
	 * x = index & 0xF;
	 * z = (index >> 4) & 0xF;
	 * y = index >> 8;
	 * }
	 * </pre>
	 * The CompoundTags are references to this Section's Palette and should only be modified if the intention is to
	 * modify ALL blocks of the same type in this Section at the same time.
	 */
	public BlockStateIterator blocksStates() {
		return new BlockStateIteratorImpl(this);
	}

	protected static class BlockStateIteratorImpl implements BlockStateIterator {

		private final TerrainSectionBase section;
		private final int sectionWorldY;
		private int currentIndex;
		private CompoundTag currentTag;
		private boolean dirty;

		public BlockStateIteratorImpl(TerrainSectionBase section) {
			this.section = section;
			this.sectionWorldY = section.getHeight() * 16;
			currentIndex = -1;
		}

		@Override
		public boolean hasNext() {
			return currentIndex < 4095;
		}

		@Override
		public CompoundTag next() {
			return currentTag = section.getBlockStateAt(++currentIndex);
		}

		@Override
		public Iterator<CompoundTag> iterator() {
			return this;
		}

		@Override
		public void setBlockStateAtCurrent(CompoundTag state) {
			Objects.requireNonNull(state);
			if (currentTag != state) {
				dirty = !section.setBlockStateAt(currentX(), currentY(), currentZ(), state, false);
			}
		}

		@Override
		public void cleanupPaletteAndBlockStatesIfDirty() {
			if (dirty) section.cleanupPaletteAndBlockStates();
		}

		@Override
		public int currentIndex() {
			return currentIndex;
		}

		@Override
		public int currentX() {
			return currentIndex & 0xF;
		}

		@Override
		public int currentZ() {
			return (currentIndex >> 4) & 0xF;
		}

		@Override
		public int currentY() {
			return currentIndex >> 8;
		}

		@Override
		public int currentBlockY() {
			return sectionWorldY + (currentIndex >> 8);
		}
	}

	/**
	 * Streams all blocks in this section, in order of their indices.
	 * XYZ can be calculated with the following formulas:
	 * <pre>
	 * {@code
	 * x = index & 0xF;
	 * z = (index >> 4) & 0xF;
	 * y = index >> 8;
	 * }
	 * </pre>
	 * The CompoundTags are references to this Section's Palette and should only be modified if the intention is to
	 * modify ALL blocks of the same type in this Section at the same time.
	 */
	public Stream<CompoundTag> streamBlocksStates() {
		return StreamSupport.stream(blocksStates().spliterator(), false);
	}
}