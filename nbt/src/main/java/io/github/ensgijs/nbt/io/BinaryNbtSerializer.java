package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;
import java.io.IOException;
import java.io.OutputStream;

public class BinaryNbtSerializer implements Serializer<NamedTag> {
	private CompressionType compression;
	private boolean littleEndian;
	private boolean sortCompoundTagEntries;

	public BinaryNbtSerializer(CompressionType compression) {
		this(compression, false);
	}

	public BinaryNbtSerializer(CompressionType compression, boolean littleEndian) {
		this(compression, littleEndian, false);
	}

	public BinaryNbtSerializer(CompressionType compression, boolean littleEndian, boolean sortCompoundTagEntries) {
		this.compression = compression;
		this.littleEndian = littleEndian;
		this.sortCompoundTagEntries = sortCompoundTagEntries;
	}

	@Override
	public void toStream(NamedTag object, OutputStream out) throws IOException {
		NbtOutput nbtOut;
		OutputStream output = compression.compress(out);
		if (!littleEndian) {
			nbtOut = new BigEndianNbtOutputStream(output, sortCompoundTagEntries);
		} else {
			nbtOut = new LittleEndianNbtOutputStream(output, sortCompoundTagEntries);
		}
		nbtOut.writeTag(object, Tag.DEFAULT_MAX_DEPTH);
		// TODO: this execution order looks like a bug... fix or document why this is the correct order
		compression.finish(output);
		nbtOut.flush();
	}

	@Override
	public boolean getSortCompoundTagEntries() {
		return sortCompoundTagEntries;
	}

	@Override
	public void setSortCompoundTagEntries(boolean sorted) {
		sortCompoundTagEntries = sorted;
	}
}
