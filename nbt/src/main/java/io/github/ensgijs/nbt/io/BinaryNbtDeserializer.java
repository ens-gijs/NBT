package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;
import java.io.IOException;
import java.io.InputStream;

public class BinaryNbtDeserializer implements Deserializer<NamedTag> {
	private final CompressionType compression;
	private final boolean littleEndian;

	public BinaryNbtDeserializer(CompressionType compression) {
		this(compression, false);
	}

	/**
	 * @param compression Compressions strategy to use.
	 * @param littleEndian Minecraft bedrock data is stored in little endian while MC Java is stored big endian.
	 */
	public BinaryNbtDeserializer(CompressionType compression, boolean littleEndian) {
		this.compression = compression;
		this.littleEndian = littleEndian;
	}

	@Override
	public NamedTag fromStream(InputStream stream) throws IOException {
		NbtInput nbtIn;
		InputStream input = compression.decompress(stream);
		if (!littleEndian) {
			nbtIn = new BigEndianNbtInputStream(input);
		} else {
			nbtIn = new LittleEndianNbtInputStream(input);
		}
		return nbtIn.readTag(Tag.DEFAULT_MAX_DEPTH);
	}
}
