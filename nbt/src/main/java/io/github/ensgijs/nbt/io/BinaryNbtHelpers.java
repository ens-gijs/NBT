package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;

import java.io.*;
import java.nio.file.Path;

/** Utilities for reading and writing {@link Tag}'s to and from binary NBT data. */
public final class BinaryNbtHelpers {
	private BinaryNbtHelpers() {}

	// <editor-fold desc="Big Endian read/write (MC Java)">
	public static Path write(NamedTag tag, File file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			new BinaryNbtSerializer(compression, sortCompoundTagEntries).toStream(tag, fos);
		}
		return file.toPath();
	}

	public static Path write(NamedTag tag, File file, CompressionType compression) throws IOException {
		return write(tag, file, compression, false);
	}

	public static Path write(NamedTag tag, String file, CompressionType compression) throws IOException {
		return write(tag, new File(file), compression, false);
	}

	public static Path write(NamedTag tag, Path path, CompressionType compression) throws IOException {
		return write(tag, path.toFile(), compression, false);
	}

	public static Path write(Tag<?> tag, File file, CompressionType compression) throws IOException {
		return write(new NamedTag(null, tag), file, compression, false);
	}

	public static Path write(Tag<?> tag, String file, CompressionType compression) throws IOException {
		return write(new NamedTag(null, tag), new File(file), compression, false);
	}

	public static Path write(Tag<?> tag, Path path, CompressionType compression) throws IOException {
		return write(new NamedTag(null, tag), path.toFile(), compression, false);
	}

	public static Path write(NamedTag tag, String file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return write(tag, new File(file), compression, sortCompoundTagEntries);
	}

	public static Path write(NamedTag tag, Path path, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return write(tag, path.toFile(), compression, sortCompoundTagEntries);
	}

	public static Path write(Tag<?> tag, File file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return write(new NamedTag(null, tag), file, compression, sortCompoundTagEntries);
	}

	public static Path write(Tag<?> tag, String file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return write(new NamedTag(null, tag), new File(file), compression, sortCompoundTagEntries);
	}

	public static Path write(Tag<?> tag, Path path, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return write(new NamedTag(null, tag), path.toFile(), compression, sortCompoundTagEntries);
	}

	public static NamedTag read(InputStream is, CompressionType compression) throws IOException {
		return new BinaryNbtDeserializer(compression).fromStream(is);
	}

	public static NamedTag read(File file, CompressionType compression) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return read(fis, compression);
		}
	}

	public static NamedTag read(String file, CompressionType compression) throws IOException {
		return read(new File(file), compression);
	}

	public static NamedTag read(Path path, CompressionType compression) throws IOException {
		return read(path.toFile(), compression);
	}

	public static NamedTag read(File file) throws IOException {
		CompressionType compression;
		try (FileInputStream fis = new FileInputStream(file)) {
			compression = CompressionType.detect(fis.readNBytes(2));
		}
		try (FileInputStream fis = new FileInputStream(file)) {
			return new BinaryNbtDeserializer(compression).fromStream(fis);
		}
	}

	public static NamedTag read(String file) throws IOException {
		return read(new File(file));
	}

	public static NamedTag read(Path path) throws IOException {
		return read(path.toFile());
	}

	/**
	 * Note that Paper's ItemStack#serializeAsBytes returns binary nbt data with {@link CompressionType#GZIP}.
	 */
	public static NamedTag deserializeBytes(byte[] bytes, CompressionType compression) throws IOException {
		return new BinaryNbtDeserializer(compression).fromStream(new ByteArrayInputStream(bytes));
	}

	/**
	 * Auto-detects {@link CompressionType} for the given bytes, then deserializes.
	 */
	public static NamedTag deserializeBytes(byte[] bytes) throws IOException {
		return new BinaryNbtDeserializer(CompressionType.detect(bytes))
				.fromStream(new ByteArrayInputStream(bytes));
	}

	/**
	 * Note that Paper's ItemStack#serializeAsBytes returns binary nbt data with {@link CompressionType#GZIP}.
	 */
	public static byte[] serializeAsBytes(Tag<?> tag, CompressionType compression) throws IOException {
		return serializeAsBytes(new NamedTag(null, tag), compression, false);
	}

	/**
	 * Note that Paper's ItemStack#serializeAsBytes returns binary nbt data with {@link CompressionType#GZIP}.
	 */
	public static byte[] serializeAsBytes(NamedTag tag, CompressionType compression) throws IOException {
		try (ByteArrayOutputStream fos = new ByteArrayOutputStream(1024)) {
			new BinaryNbtSerializer(compression, false).toStream(tag, fos);
			return fos.toByteArray();
		}
	}

	/**
	 * Note that Paper's ItemStack#serializeAsBytes returns binary nbt data with {@link CompressionType#GZIP}.
	 */
	public static byte[] serializeAsBytes(Tag<?> tag, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return serializeAsBytes(new NamedTag(null, tag), compression, sortCompoundTagEntries);
	}

	/**
	 * Note that Paper's ItemStack#serializeAsBytes returns binary nbt data with {@link CompressionType#GZIP}.
	 */
	public static byte[] serializeAsBytes(NamedTag tag, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		try (ByteArrayOutputStream fos = new ByteArrayOutputStream(1024)) {
			new BinaryNbtSerializer(compression, sortCompoundTagEntries).toStream(tag, fos);
			return fos.toByteArray();
		}
	}
	// </editor-fold>

	// <editor-fold desc="Little Endian read/write (MC Bedrock)">
	public static Path writeLittleEndian(NamedTag tag, File file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			new BinaryNbtSerializer(compression, true, sortCompoundTagEntries).toStream(tag, fos);
		}
		return file.toPath();
	}

	public static Path writeLittleEndian(NamedTag tag, File file, CompressionType compression) throws IOException {
		return writeLittleEndian(tag, file, compression, false);
	}

	public static Path writeLittleEndian(NamedTag tag, String file, CompressionType compression) throws IOException {
		return writeLittleEndian(tag, new File(file), compression);
	}

	public static Path writeLittleEndian(NamedTag tag, Path path, CompressionType compression) throws IOException {
		return writeLittleEndian(tag, path.toFile(), compression);
	}

	public static Path writeLittleEndian(Tag<?> tag, File file, CompressionType compression) throws IOException {
		return writeLittleEndian(new NamedTag(null, tag), file, compression);
	}

	public static Path writeLittleEndian(Tag<?> tag, String file, CompressionType compression) throws IOException {
		return writeLittleEndian(new NamedTag(null, tag), new File(file), compression);
	}

	public static Path writeLittleEndian(Tag<?> tag, Path path, CompressionType compression) throws IOException {
		return writeLittleEndian(new NamedTag(null, tag), path.toFile(), compression);
	}

	public static Path writeLittleEndian(NamedTag tag, String file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return writeLittleEndian(tag, new File(file), compression, sortCompoundTagEntries);
	}

	public static Path writeLittleEndian(NamedTag tag, Path path, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return writeLittleEndian(tag, path.toFile(), compression, sortCompoundTagEntries);
	}

	public static Path writeLittleEndian(Tag<?> tag, File file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return writeLittleEndian(new NamedTag(null, tag), file, compression, sortCompoundTagEntries);
	}

	public static Path writeLittleEndian(Tag<?> tag, String file, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return writeLittleEndian(new NamedTag(null, tag), new File(file), compression, sortCompoundTagEntries);
	}

	public static Path writeLittleEndian(Tag<?> tag, Path path, CompressionType compression, boolean sortCompoundTagEntries) throws IOException {
		return writeLittleEndian(new NamedTag(null, tag), path.toFile(), compression, sortCompoundTagEntries);
	}

	public static NamedTag readLittleEndian(File file, CompressionType compression) throws IOException {
		try (FileInputStream fis = new FileInputStream(file)) {
			return new BinaryNbtDeserializer(compression, true).fromStream(fis);
		}
	}

	public static NamedTag readLittleEndian(String file, CompressionType compression) throws IOException {
		return readLittleEndian(new File(file), compression);
	}

	public static NamedTag readLittleEndian(Path path, CompressionType compression) throws IOException {
		return readLittleEndian(path.toFile(), compression);
	}
	// </editor-fold>
}
