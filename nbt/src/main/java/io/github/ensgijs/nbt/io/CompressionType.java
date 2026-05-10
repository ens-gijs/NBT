package io.github.ensgijs.nbt.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public enum CompressionType {
	NONE(0, t -> t, t -> t),
	/** Most used compression type for binary nbt data files. */
	GZIP(1, GZIPOutputStream::new, GZIPInputStream::new),
	/** Default compression type used by the vanilla jar to store chunks in mca files. */
	ZLIB(2, DeflaterOutputStream::new, InflaterInputStream::new);

	@FunctionalInterface
	private interface IOExceptionFunction<T, R> {
		R accept(T t) throws IOException;
	}

	private final byte id;
	private final IOExceptionFunction<OutputStream, ? extends OutputStream> compressor;
	private final IOExceptionFunction<InputStream, ? extends InputStream> decompressor;

	CompressionType(int id,
					IOExceptionFunction<OutputStream, ? extends OutputStream> compressor,
					IOExceptionFunction<InputStream, ? extends InputStream> decompressor) {
		this.id = (byte) id;
		this.compressor = compressor;
		this.decompressor = decompressor;
	}

	public byte getID() {
		return id;
	}

	public OutputStream compress(OutputStream out) throws IOException {
		return compressor.accept(out);
	}

	public InputStream decompress(InputStream in) throws IOException {
		return decompressor.accept(in);
	}

	/**
	 * Finishes writing compressed data to the output stream without closing it.
	 * @exception IOException if an I/O error has occurred
	 */
	public void finish(OutputStream out) throws IOException {
		if (out instanceof DeflaterOutputStream) {
			((DeflaterOutputStream) out).finish();
		}
	}

	public static CompressionType getFromID(byte id) {
		for (CompressionType c : CompressionType.values()) {
			if (c.id == id) {
				return c;
			}
		}
		return null;
	}

	/**
	 * Makes an excellent guess (for minecraft nbt data) at the {@link CompressionType} used by looking for each
	 * compression types respective magic-header values.
	 *
	 * <p>
	 *     There is virtually NO RISK of this detection strategy producing a wrong result if the input
	 * bytes actually represent binary nbt data. This is because the first (uncompressed) byte is always a tag ID
	 * and the max tag id is well under 0x1f currently. Additionally, all bin nbt is stored as a {@link NamedTag}
	 * which is encoded in in-fix order [tag-id, named-tag name, tag-data] where the tag's id is put before the
	 * named-tag's name and this name is stored using UTF which always uses at least 2 bytes for the string length.
	 * This means that the tags name string would need to be extremely long (at least 35,592 bytes) before it could
	 * possibly match either gzip or zlib low magic header bytes, and we would need a valid tag-id of 0x1f to exist.
	 * <p>
	 *     WARNING: if this enum, and method, is used for non-nbt data it becomes possible, but probably unlikely,
	 * for this method to make a mistake and choose the wrong {@link CompressionType}!
	 */
	public static CompressionType detect(byte[] bytes) {
		if (bytes != null && bytes.length >= 2) {
			if (bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b) {
				return GZIP;
			}
			if (bytes[0] == (byte) 0x78 && bytes[1] == (byte) 0x9c) {
				return ZLIB;
			}
		}
		return NONE;
	}
}
