package io.github.ensgijs.nbt.mca.io;

import io.github.ensgijs.nbt.io.CompressionType;
import io.github.ensgijs.nbt.mca.McaEntitiesFile;
import io.github.ensgijs.nbt.mca.McaFileBase;
import io.github.ensgijs.nbt.mca.McaPoiFile;
import io.github.ensgijs.nbt.mca.McaRegionFile;
import io.github.ensgijs.nbt.mca.util.IntPointXZ;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides utility functions to read and write .mca files and to convert block, chunk, and region coordinates.
 * @see McaFileChunkIterator
 * @see McaFileStreamingWriter
 */
public final class McaFileHelpers {

	private McaFileHelpers() {}

	private static final Pattern MCA_FILE_PATTERN = Pattern.compile("^.*\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");
	private static final Predicate<String> IS_VALID_MCA_FILE_NAME_TESTER = MCA_FILE_PATTERN.asPredicate();

	/**
	 * This map controls the factory creation behavior of the various "auto" functions. When an auto function is
	 * given an mca file location the folder name which contains the .mca files is used to lookup the correct
	 * mca file creator from this map. For example, if an auto method were passed the path
	 * "foo/bar/creator_name/r.4.2.mca" it would call {@code MCA_CREATORS.get("creator_name").apply(4, 2)}.
	 * <p>By manipulating this map you can control the factory behavior to support new mca types or to specify
	 * that a custom creation method should be called which could even return a custom {@link McaFileBase}
	 * implementation.</p>
	 */
	public static final Map<String, BiFunction<Integer, Integer, McaFileBase<?>>> MCA_CREATORS = new HashMap<>();

	static {
		MCA_CREATORS.put("region", McaRegionFile::new);
		MCA_CREATORS.put("poi", McaPoiFile::new);
		MCA_CREATORS.put("entities", McaEntitiesFile::new);
	}

	//<editor-fold desc="Legacy 'read' Readers" defaultstate="collapsed">
	/**
	 * @see McaFileHelpers#read(File)
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @throws IOException if something during deserialization goes wrong.
	 * @deprecated switch to {@link #readAuto(String)}
	 */
	@Deprecated
	public static McaRegionFile read(String file) throws IOException {
		return read(new File(file), LoadFlags.LOAD_ALL_DATA);
	}

	/**
	 * Reads an MCA file and loads all of its chunks.
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @throws IOException if something during deserialization goes wrong.
	 * @deprecated switch to {@link #readAuto(File)}
	 */
	@Deprecated
	public static McaRegionFile read(File file) throws IOException {
		return read(file, LoadFlags.LOAD_ALL_DATA);
	}

	/**
	 * @see McaFileHelpers#read(File, long)
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @throws IOException if something during deserialization goes wrong.
	 * @deprecated switch to {@link #readAuto(String, long)}
	 */
	@Deprecated
	public static McaRegionFile read(String file, long loadFlags) throws IOException {
		return read(new File(file), loadFlags);
	}

	/**
	 * Reads an MCA file and loads all of its chunks.
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @throws IOException if something during deserialization goes wrong.
	 * @deprecated switch to {@link #readAuto(File, long)}
	 */
	@Deprecated
	public static McaRegionFile read(File file, long loadFlags) throws IOException {
		IntPointXZ xz = regionXZFromFileName(file.getName());
		McaRegionFile mcaFile = new McaRegionFile(xz.getX(), xz.getZ());
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			mcaFile.deserialize(raf, loadFlags);
			return mcaFile;
		}
	}

	//</editor-fold>

	//<editor-fold desc="POI MCA Readers (/poi/r.X.Z.mca files added in MC 1.14)">

	public static McaPoiFile readPoi(String file) throws IOException {
		return readPoi(new File(file), LoadFlags.LOAD_ALL_DATA);
	}

	public static McaPoiFile readPoi(Path file) throws IOException {
		return readPoi(file.toFile(),  LoadFlags.LOAD_ALL_DATA);
	}

	public static McaPoiFile readPoi(File file) throws IOException {
		return readPoi(file,  LoadFlags.LOAD_ALL_DATA);
	}

	public static McaPoiFile readPoi(String file, long loadFlags) throws IOException {
		return readPoi(new File(file), loadFlags);
	}

	public static McaPoiFile readPoi(Path file, long loadFlags) throws IOException {
		return readPoi(file.toFile(), loadFlags);
	}

	public static McaPoiFile readPoi(File file, long loadFlags) throws IOException {
		IntPointXZ xz = regionXZFromFileName(file.getName());
		McaPoiFile mcaFile = new McaPoiFile(xz.getX(), xz.getZ());
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			mcaFile.deserialize(raf, loadFlags);
			return mcaFile;
		}
	}

	//</editor-fold>

	//<editor-fold desc="Entities MCA Readers (/entities/r.X.Z.mca files added in MC 1.17)">

	public static McaEntitiesFile readEntities(String file) throws IOException {
		return readEntities(new File(file), LoadFlags.LOAD_ALL_DATA);
	}

	public static McaEntitiesFile readEntities(Path file) throws IOException {
		return readEntities(file.toFile(),  LoadFlags.LOAD_ALL_DATA);
	}

	public static McaEntitiesFile readEntities(File file) throws IOException {
		return readEntities(file,  LoadFlags.LOAD_ALL_DATA);
	}

	public static McaEntitiesFile readEntities(String file, long loadFlags) throws IOException {
		return readEntities(new File(file), loadFlags);
	}

	public static McaEntitiesFile readEntities(Path file, long loadFlags) throws IOException {
		return readEntities(file.toFile(), loadFlags);
	}

	public static McaEntitiesFile readEntities(File file, long loadFlags) throws IOException {
		IntPointXZ xz = regionXZFromFileName(file.getName());
		McaEntitiesFile mcaFile = new McaEntitiesFile(xz.getX(), xz.getZ());
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			mcaFile.deserialize(raf, loadFlags);
			return mcaFile;
		}
	}

	//</editor-fold>

	//<editor-fold desc="Auto MCA Readers">

	/**
	 * @see McaFileHelpers#readAuto(File)
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @throws IOException if something during deserialization goes wrong.
	 */
	public static <T extends McaFileBase<?>> T readAuto(String file) throws IOException {
		return readAuto(new File(file), LoadFlags.LOAD_ALL_DATA);
	}

	/**
	 * Reads an MCA file and loads all of its chunks.
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @throws IOException if something during deserialization goes wrong.
	 */
	public static <T extends McaFileBase<?>> T readAuto(File file) throws IOException {
		return readAuto(file, LoadFlags.LOAD_ALL_DATA);
	}

	/**
	 * Reads an MCA file and loads all of its chunks.
	 * @param path The path to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @throws IOException if something during deserialization goes wrong.
	 */
	public static <T extends McaFileBase<?>> T readAuto(Path path) throws IOException {
		return readAuto(path, LoadFlags.LOAD_ALL_DATA);
	}

	/**
	 * @see McaFileHelpers#readAuto(File, long)
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @throws IOException if something during deserialization goes wrong.
	 */
	public static <T extends McaFileBase<?>> T readAuto(String file, long loadFlags) throws IOException {
		return readAuto(new File(file), loadFlags);
	}

	/**
	 * @see McaFileHelpers#readAuto(File, long)
	 * @param path The path to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data.
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @throws IOException if something during deserialization goes wrong.
	 */
	public static <T extends McaFileBase<?>> T readAuto(Path path, long loadFlags) throws IOException {
		return readAuto(path.toFile(), loadFlags);
	}

	/**
	 * Reads an MCA file and loads all of its chunks.
	 * @param file The file to read the data from.
	 * @return An in-memory representation of the MCA file with decompressed chunk data
	 * @param loadFlags A logical or of {@link LoadFlags} constants indicating what data should be loaded
	 * @throws IOException if something during deserialization goes wrong.
	 */
	public static <T extends McaFileBase<?>> T readAuto(File file, long loadFlags) throws IOException {
		T mcaFile = autoMCAFile(file);
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			mcaFile.deserialize(raf, loadFlags);
			return mcaFile;
		}
	}

	//</editor-fold>

	//<editor-fold desc="Writers">

	/**
	 * Calls {@link McaFileHelpers#write(McaFileBase, File, boolean)} without changing the timestamps.
	 * @see McaFileHelpers#write(McaFileBase, File, boolean)
	 * @param mcaFile The data of the MCA file to write.
	 * @param file The file to write to.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something goes wrong during serialization.
	 */
	public static int write(McaFileBase<?> mcaFile, String file) throws IOException {
		return write(mcaFile, new File(file), false);
	}

	/**
	 * Calls {@link McaFileHelpers#write(McaFileBase, File, boolean)} without changing the timestamps.
	 * @see McaFileHelpers#write(McaFileBase, File, boolean)
	 * @param mcaFile The data of the MCA file to write.
	 * @param file The file to write to.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something goes wrong during serialization.
	 */
	public static int write(McaFileBase<?> mcaFile, File file) throws IOException {
		return write(mcaFile, file, false);
	}

	/**
	 * Calls {@link McaFileHelpers#write(McaFileBase, File, boolean)} without changing the timestamps.
	 * @see McaFileHelpers#write(McaFileBase, File, boolean)
	 * @param mcaFile The data of the MCA file to write.
	 * @param path The file to write to.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something goes wrong during serialization.
	 */
	public static int write(McaFileBase<?> mcaFile, Path path) throws IOException {
		return write(mcaFile, path.toFile(), false);
	}

	/**
	 * @see McaFileHelpers#write(McaFileBase, File, boolean)
	 * @param mcaFile The data of the MCA file to write.
	 * @param path The file to write to.
	 * @param changeLastUpdate Whether to adjust the timestamps of when the file was saved.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something goes wrong during serialization.
	 */
	public static int write(McaFileBase<?> mcaFile, Path path, boolean changeLastUpdate) throws IOException {
		return write(mcaFile, path.toFile(), changeLastUpdate);
	}

	/**
	 * @see McaFileHelpers#write(McaFileBase, File, boolean)
	 * @param mcaFile The data of the MCA file to write.
	 * @param file The file to write to.
	 * @param changeLastUpdate Whether to adjust the timestamps of when the file was saved.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something goes wrong during serialization.
	 */
	public static int write(McaFileBase<?> mcaFile, String file, boolean changeLastUpdate) throws IOException {
		return write(mcaFile, new File(file), changeLastUpdate);
	}

	/**
	 * Writes an {@code McaFileBase} object to disk. It optionally adjusts the timestamps
	 * when the file was last saved to the current date and time or leaves them at
	 * the value set by either loading an already existing MCA file or setting them manually.<br>
	 * If the file already exists, it is completely overwritten by the new file (no modification).
	 * @param mcaFile The data of the MCA file to write.
	 * @param file The file to write to.
	 * @param changeLastUpdate Whether to adjust the timestamps of when the file was saved.
	 * @return The amount of chunks written to the file.
	 * @throws IOException If something goes wrong during serialization.
	 */
	public static int write(McaFileBase<?> mcaFile, File file, boolean changeLastUpdate) throws IOException {
		File to = file;
		if (file.exists()) {
			to = File.createTempFile(to.getName(), null);
			to.deleteOnExit();  // attempt to make sure the temp file is cleaned up if we fail before we move it
		}
		int chunks;
		try (RandomAccessFile raf = new RandomAccessFile(to, "rw")) {
			chunks = mcaFile.serialize(raf, CompressionType.ZLIB, changeLastUpdate);
		}

		// TODO(bug): This logic is flawed - why would we ever want an empty region file?
		// 		Why only produce an empty region file if it didn't exist before?
		//		Shouldn't trying to write an empty region file be an error case the caller should know about?
		//		Should the (existing) region file be DELETED if we try to write an empty one?
		// Proposal: always write to a temp file, if chunks is empty we throw a custom NoChunksWrittenIOException
		//		and leave the original alone. NoChunksWrittenIOException could be fully file aware and provide
		//		a utility function to delete the destination file - but this is a non-standard offering for exceptions,
		//		though it might make things cleaner for the caller.
		if (chunks > 0 && to != file) {
			Files.move(to.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return chunks;
	}

	//</editor-fold>

	//<editor-fold desc="Region File Name Generators">

	/**
	 * Turns the chunks coordinates into region coordinates and calls
	 * {@link McaFileHelpers#createNameFromRegionLocation(int, int)}
	 * @param chunkX The x-value of the location of the chunk.
	 * @param chunkZ The z-value of the location of the chunk.
	 * @return A mca filename in the format "r.{regionX}.{regionZ}.mca"
	 */
	public static String createNameFromChunkLocation(int chunkX, int chunkZ) {
		return createNameFromRegionLocation( chunkToRegion(chunkX), chunkToRegion(chunkZ));
	}

	/**
	 * Turns the block coordinates into region coordinates and calls
	 * {@link McaFileHelpers#createNameFromRegionLocation(int, int)}
	 * @param blockX The x-value of the location of the block.
	 * @param blockZ The z-value of the location of the block.
	 * @return A mca filename in the format "r.{regionX}.{regionZ}.mca"
	 */
	public static String createNameFromBlockLocation(int blockX, int blockZ) {
		return createNameFromRegionLocation(blockToRegion(blockX), blockToRegion(blockZ));
	}

	/**
	 * Creates a filename string from provided region coordinates.
	 * @param regionX The x-value of the location of the region.
	 * @param regionZ The z-value of the location of the region.
	 * @return A mca filename in the format "r.{regionX}.{regionZ}.mca"
	 */
	public static String createNameFromRegionLocation(int regionX, int regionZ) {
		return "r." + regionX + "." + regionZ + ".mca";
	}

	public static String createNameFromRegionLocation(IntPointXZ regionXZ) {
		return "r." + regionXZ.getX() + "." + regionXZ.getZ() + ".mca";
	}

	//</editor-fold>

	//<editor-fold desc="Coordinate Helpers">

	/**
	 * Turns a block coordinate value into a chunk coordinate value.
	 * @param block The block coordinate value.
	 * @return The chunk coordinate value.
	 */
	public static int blockToChunk(int block) {
		return block >> 4;
	}

	/**
	 * Turns a block coordinate value into a region coordinate value.
	 * @param block The block coordinate value.
	 * @return The region coordinate value.
	 */
	public static int blockToRegion(int block) {
		return block >> 9;
	}

	/**
	 * Turns a chunk coordinate value into a region coordinate value.
	 * @param chunk The chunk coordinate value.
	 * @return The region coordinate value.
	 */
	public static int chunkToRegion(int chunk) {
		return chunk >> 5;
	}

	/**
	 * Turns a region coordinate value into a chunk coordinate value.
	 * @param region The region coordinate value.
	 * @return The chunk coordinate value.
	 */
	public static int regionToChunk(int region) {
		return region << 5;
	}

	/**
	 * Turns a region coordinate value into a block coordinate value.
	 * @param region The region coordinate value.
	 * @return The block coordinate value.
	 */
	public static int regionToBlock(int region) {
		return region << 9;
	}

	/**
	 * Turns a chunk coordinate value into a block coordinate value.
	 * @param chunk The chunk coordinate value.
	 * @return The block coordinate value.
	 */
	public static int chunkToBlock(int chunk) {
		return chunk << 4;
	}

	/**
	 * Turns an absolute block coordinate into a chunk relative one [0..15]
	 * @param block The absolute block coordinate.
	 * @return Block coordinate relative to its chunk.
	 */
	public static int blockAbsoluteToChunkRelative(int block) {
		return block & 0xF;
	}

	/**
	 * Turns an absolute block coordinate into a chunk relative one [0..16)
	 * @param block The absolute block coordinate.
	 * @return Block coordinate relative to its chunk.
	 */
	public static double blockAbsoluteToChunkRelative(double block) {
		double bin = block % 16;
		return bin >= 0 ? bin : 16 + bin;
	}

	//</editor-fold>

	/**
	 * Creates a {@link IntPointXZ} initialized with its X and Z extracted from the given file name.
	 * Ex. "r.1.-4.mca" returns XZ(1, -4)
	 */
	public static IntPointXZ regionXZFromFileName(String name) {
		final Matcher m = MCA_FILE_PATTERN.matcher(name);
		if (!m.find()) {
			throw new IllegalArgumentException("invalid mca file name (expect name match '*.<X>.<Z>.mca'): "
					+ name);
		}
		return new IntPointXZ(Integer.parseInt(m.group("regionX")), Integer.parseInt(m.group("regionZ")));
	}

	private static void throwCannotDetermineMcaType(Exception cause) {
		throw new IllegalArgumentException(
				"Unable to determine mca file type. Expect the mca file to have a parent folder with one of the following names: "
						+ MCA_CREATORS.keySet().stream().sorted().collect(Collectors.joining(", ")), cause);
	}

	/**
	 * @see #autoMCAFile(Path)
	 * @see #autoMCAFile(String, Path) 
	 */
	public static <T extends McaFileBase<?>> T autoMCAFile(File file) {
		return autoMCAFile(file.toPath());
	}

	/**
	 * Detects and creates a concretion (implementer) of {@link McaFileBase}. The actual type returned is determined by
	 * the name of the folder containing the .mca file.
	 * <p><b>Usage suggestion</b>  when the caller fully controls passed file name:
	 * <pre>{@code McaEntitiesFile entitiesMca = McaFileHelpers.autoMCAFile(Paths.get("entities/r.0.0.mca"));}</pre>
	 * <p><b>Usage suggestion</b> when the caller expects a specific return type but does not control the passed file name:
	 * <pre>{@code try {
	 *   McaEntitiesFile entitiesMca = McaFileHelpers.autoMCAFile(filename);
	 * } catch (ClassCastException expected) {
	 *   // got an unexpected type
	 * }}</pre>
	 * <p><b>Usage suggestion</b>  when the caller may not know what return type to expect:
	 * <pre>{@code McaFileBase<?> mcaFile = McaFileHelpers.autoMCAFile(filename);
	 * if (mcaFile instanceof McaRegionFile) {
	 *   // process region mca file
	 *   McaRegionFile regionMca = (McaRegionFile) mcaFile;
	 * } else if (mcaFile instanceof McaPoiFile) {
	 *   // process poi mca file
	 *   McaPoiFile poiMca = (McaPoiFile) mcaFile;
	 * } else if (mcaFile instanceof McaEntitiesFile) {
	 *   // process entities mca file
	 *   McaEntitiesFile entitiesMca = (McaEntitiesFile) mcaFile;
	 * } else {
	 *   // unsupported type / don't care about this type, etc.
	 * }}</pre>
	 *
	 * @see #autoMCAFile(String, Path)
	 */
	public static <T extends McaFileBase<?>> T autoMCAFile(Path path) {
		return autoMCAFile(path.getParent().getFileName().toString(), path);
	}

	/**
	 * Creates and initializes a new {@link McaFileBase} implementation. The actual type returned is determined by
	 * the value of useCreatorName, {@link #autoMCAFile(Path)} determines this value from the file path.
	 * However it is sometimes useful to force which creator to used, for example when reading an old region mca file
	 * with the new feature rich {@link McaEntitiesFile}. For a list of valid creator names query the keys of
	 * {@link #MCA_CREATORS} - the default list contains "region", "poi", and "entities".
	 *
	 * @param useCreatorName creator name to use to read the given file
	 * @param path The file does not need to exist but the given path must have at least 2 parts.
	 *             Required parts: "mca_type/mca_file"
	 *             where mca_type (such as "region", "poi", "entities") is used to determine which
	 *             {@link #MCA_CREATORS} to call and mca_file is the .mca file such as "r.0.0.mca".
	 * @param <T> {@link McaFileBase} type - do note that any {@link ClassCastException} errors will be thrown
	 *           at the location of assignment, not from within this call.
	 * @return Instantiated and initialized concretion of {@link McaFileBase}. Never Null.
	 * @throws IllegalArgumentException Thrown when the mca type could not be determined from the path, when there
	 * is no {@link #MCA_CREATORS} mapped to the mca type, or when the regions X and Z locations could not be
	 * extracted from the filename.
	 * @throws NullPointerException Thrown when a custom creator did not produce a result.
	 * @see #autoMCAFile(Path) 
	 */
	@SuppressWarnings("unchecked")
	public static <T extends McaFileBase<?>> T autoMCAFile(String useCreatorName, Path path) {
		BiFunction<Integer, Integer, McaFileBase<?>> creator = null;
		try {
			creator = MCA_CREATORS.get(useCreatorName);
			if (creator == null) throwCannotDetermineMcaType(null);
		} catch (Exception ex) {
			throwCannotDetermineMcaType(ex);
		}
		final Matcher m = MCA_FILE_PATTERN.matcher(path.getFileName().toString());
		if (!m.find()) {
			throw new IllegalArgumentException("invalid mca file name (expect name match '*.<X>.<Z>.mca'): " + path);
		}
		final int x = Integer.parseInt(m.group("regionX"));
		final int z = Integer.parseInt(m.group("regionZ"));
		T mcaFile = (T) creator.apply(x, z);
		if (mcaFile == null) {
			throw new NullPointerException("Creator for " + useCreatorName + " did not produce a result for " + path);
		}
		return mcaFile;
	}

	public static boolean isValidMcaFileName(String fileName) {
		return IS_VALID_MCA_FILE_NAME_TESTER.test(fileName);
	}

	public static boolean isValidMcaFileName(File file) {
		return IS_VALID_MCA_FILE_NAME_TESTER.test(file.getName());
	}
}
