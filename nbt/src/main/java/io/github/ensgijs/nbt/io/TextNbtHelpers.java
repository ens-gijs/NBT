package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;
import io.github.ensgijs.nbt.util.ArgValidator;
import io.github.ensgijs.nbt.util.JsonPrettyPrinter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utilities for converting {@link Tag}'s  and {@link NamedTag}'s to and from string NBT text data and files.
 * <p>NOTE: {@link #readTextNbtFile(File)}, and its variants, can read both uncompressed (plain text) and GZ
 * compressed files (usually ending in .gz file extension - but the extension itself is not evaluated, instead
 * the gzip magic number/bom is looked for).</p>
 * <p>NOTE: {@link #writeTextNbtFile(File, Tag)}, and its variants, can write both uncompressed (plain text) and GZ
 * compressed files. If the given file name ends in the '.gz' extension it will be written as a compressed file,
 * otherwise it will be written as plain text.</p>
 */
public final class TextNbtHelpers {
	private TextNbtHelpers() {}

	// <editor-fold desc="to/from string">
	public static String toTextNbt(NamedTag namedTag, boolean prettyPrint, boolean sortCompoundTagEntries) {
		String snbt = new TextNbtSerializer(sortCompoundTagEntries).toString(namedTag);
		return !prettyPrint ? snbt : JsonPrettyPrinter.prettyPrintJson(snbt);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbt(NamedTag namedTag, boolean prettyPrint)  {
		return toTextNbt(namedTag, prettyPrint, true);
	}

	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbtUnsorted(NamedTag namedTag, boolean prettyPrint) {
		return toTextNbt(namedTag, prettyPrint, false);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbt(NamedTag namedTag) {
		return toTextNbt(namedTag, true, true);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbtUnsorted(NamedTag namedTag) {
		return toTextNbt(namedTag, true, false);
	}

	public static String toTextNbt(Tag<?> tag, boolean prettyPrint, boolean sortCompoundTagEntries) {
		return toTextNbt(new NamedTag(null, tag), prettyPrint, sortCompoundTagEntries);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbt(Tag<?> tag, boolean prettyPrint) {
		return toTextNbt(new NamedTag(null, tag), prettyPrint, true);
	}

	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbtUnsorted(Tag<?> tag, boolean prettyPrint) {
		return toTextNbt(new NamedTag(null, tag), prettyPrint, false);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbt(Tag<?> tag) {
		return toTextNbt(new NamedTag(null, tag), true, true);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static String toTextNbtUnsorted(Tag<?> tag) {
		return toTextNbt(new NamedTag(null, tag), true, false);
	}

	public static NamedTag fromTextNbt(String string) throws IOException {
		return new TextNbtDeserializer().fromString(string);
	}
	// </editor-fold>


	private static Path writeTextNbtFile0(Path filePath, Object tag, boolean prettyPrint, boolean sortCompoundTagEntries) throws IOException {
		if (!filePath.getParent().toFile().exists()) {
			ArgValidator.check(filePath.getParent().toFile().mkdirs(),
					"Failed to create parent directory for " + filePath.toAbsolutePath());
		}
		byte[] data;
		if (tag instanceof NamedTag) {
			data = toTextNbt((NamedTag) tag, prettyPrint, sortCompoundTagEntries).getBytes(StandardCharsets.UTF_8);
		} else {
			data = toTextNbt((Tag<?>) tag, prettyPrint, sortCompoundTagEntries).getBytes(StandardCharsets.UTF_8);
		}
		if (!filePath.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".gz")) {
			Files.write(filePath, data);
		} else {
			try (GZIPOutputStream gzOut = new GZIPOutputStream(new FileOutputStream(filePath.toFile()))) {
				gzOut.write(data);
			}
		}
		return filePath;
	}

	// <editor-fold desc="write Tag<?> to file">
	public static Path writeTextNbtFile(Path filePath, Tag<?> tag, boolean prettyPrint, boolean sortCompoundTagEntries) throws IOException {
		return writeTextNbtFile0(filePath, tag, prettyPrint, sortCompoundTagEntries);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(Path filePath, Tag<?> tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(filePath, tag, prettyPrint, true);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(File file, Tag<?> tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, prettyPrint, true);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(String file, Tag<?> tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, prettyPrint, true);
	}


	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(Path filePath, Tag<?> tag) throws IOException {
		return writeTextNbtFile(filePath, tag, true, true);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(File file, Tag<?> tag) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, true, true);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(String file, Tag<?> tag) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, true, true);
	}


	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(Path filePath, Tag<?> tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(filePath, tag, prettyPrint, false);
	}

	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(File file, Tag<?> tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, prettyPrint, false);
	}

	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(String file, Tag<?> tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, prettyPrint, false);
	}


	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(Path filePath, Tag<?> tag) throws IOException {
		return writeTextNbtFile(filePath, tag, true, false);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(File file, Tag<?> tag) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, true, false);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(String file, Tag<?> tag) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, true, false);
	}
	// </editor-fold>


	// <editor-fold desc="write NamedTag to file">
	public static Path writeTextNbtFile(Path filePath, NamedTag tag, boolean prettyPrint, boolean sortCompoundTagEntries) throws IOException {
		return writeTextNbtFile0(filePath, tag, prettyPrint, sortCompoundTagEntries);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(Path filePath, NamedTag tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(filePath, tag, prettyPrint, true);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(File file, NamedTag tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, prettyPrint, true);
	}

	/**
	 * defaults to sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(String file, NamedTag tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, prettyPrint, true);
	}


	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(Path filePath, NamedTag tag) throws IOException {
		return writeTextNbtFile(filePath, tag, true, true);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(File file, NamedTag tag) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, true, true);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=true
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFile(String file, NamedTag tag) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, true, true);
	}


	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(Path filePath, NamedTag tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(filePath, tag, prettyPrint, false);
	}

	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(File file, NamedTag tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, prettyPrint, false);
	}

	/**
	 * defaults to sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(String file, NamedTag tag, boolean prettyPrint) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, prettyPrint, false);
	}


	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(Path filePath, NamedTag tag) throws IOException {
		return writeTextNbtFile(filePath, tag, true, false);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(File file, NamedTag tag) throws IOException {
		return writeTextNbtFile(file.toPath(), tag, true, false);
	}

	/**
	 * defaults to prettyPrint=true, sortCompoundTagEntries=false
	 * @see JsonPrettyPrinter#DEFAULT_SINGLE_LINE_MAX_LENGTH
	 */
	public static Path writeTextNbtFileUnsorted(String file, NamedTag tag) throws IOException {
		return writeTextNbtFile(Paths.get(file), tag, true, false);
	}
	// </editor-fold>

	// <editor-fold desc="read from file">
	public static NamedTag readTextNbt(InputStream is) throws IOException {
		try (DataInputStream dis = new DataInputStream(detectDecompression(is))) {
			return new TextNbtDeserializer().fromStream(dis);
		}
	}
	/** The given file can be either plain text (uncompressed) or gz compressed. */
	public static NamedTag readTextNbtFile(File file) throws IOException {
		return readTextNbt(new FileInputStream(file));
	}

	/** The given file can be either plain text (uncompressed) or gz compressed. */
	public static NamedTag readTextNbtFile(String file) throws IOException {
		return readTextNbtFile(new File(file));
	}

	/** The given file can be either plain text (uncompressed) or gz compressed. */
	public static NamedTag readTextNbtFile(Path path) throws IOException {
		return readTextNbtFile(path.toFile());
	}
	// </editor-fold>

	static InputStream detectDecompression(InputStream is) throws IOException {
		PushbackInputStream pbis = new PushbackInputStream(is, 2);
		int b0 = pbis.read();
		int b1 = pbis.read();
		int signature = (b0 & 0xFF) | (b1 << 8);
		if (b1 >= 0) pbis.unread(b1);
		if (b0 >= 0) pbis.unread(b0);
		if (signature == GZIPInputStream.GZIP_MAGIC) {
			return new GZIPInputStream(pbis);
		}
		return pbis;
	}
}
