package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;

import java.io.IOException;

/** If there is no content to parse (aka empty file) then null should be returned. */
public interface NbtInput {

	NamedTag readTag(int maxDepth) throws IOException;

	Tag<?> readRawTag(int maxDepth) throws IOException;
}
