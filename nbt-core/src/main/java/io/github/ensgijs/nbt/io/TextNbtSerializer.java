package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;

import java.io.*;

public class TextNbtSerializer implements Serializer<NamedTag> {
	private boolean sortCompoundTagEntries;

	public TextNbtSerializer(boolean sortCompoundTagEntries) {
		this.sortCompoundTagEntries = sortCompoundTagEntries;
	}

	public void toWriter(NamedTag tag, Writer writer) throws IOException {
		TextNbtWriter.write(tag, writer, sortCompoundTagEntries, Tag.DEFAULT_MAX_DEPTH);
	}

	public void toWriter(NamedTag tag, Writer writer, int maxDepth) throws IOException {
		TextNbtWriter.write(tag, writer, sortCompoundTagEntries, maxDepth);
	}

	public String toString(NamedTag object) {
		return toString(object, Tag.DEFAULT_MAX_DEPTH);
	}

	public String toString(NamedTag object, int maxDepth) {
		Writer writer = new StringWriter();
		try {
			toWriter(object, writer, maxDepth);
			writer.flush();
		} catch (IOException ex) {
			// this case should (probably) never happen so just wrap and toss if it ever does
			throw new RuntimeException(ex);
		}
		return writer.toString();
	}

	@Override
	public void toStream(NamedTag object, OutputStream stream) throws IOException {
		Writer writer = new OutputStreamWriter(stream);
		toWriter(object, writer);
		writer.flush();
	}

	@Override
	public void toFile(NamedTag object, File file) throws IOException {
		try (Writer writer = new FileWriter(file)) {
			toWriter(object, writer);
		}
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
