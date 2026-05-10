package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;

import java.io.IOException;

public interface NbtOutput {

	void writeTag(NamedTag tag, int maxDepth) throws IOException;

	void writeTag(Tag<?> tag, int maxDepth) throws IOException;

	void flush() throws IOException;
}
