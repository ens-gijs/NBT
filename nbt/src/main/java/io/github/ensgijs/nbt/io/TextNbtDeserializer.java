package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.Tag;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TextNbtDeserializer implements Deserializer<NamedTag> {

	public NamedTag fromReader(Reader reader, int maxDepth) throws IOException {
		BufferedReader bufferedReader;
		if (reader instanceof BufferedReader) {
			bufferedReader = (BufferedReader) reader;
		} else {
			bufferedReader = new BufferedReader(reader);
		}
		return new TextNbtParser(bufferedReader.lines().collect(Collectors.joining())).readTag(maxDepth);
	}

	public NamedTag fromReader(Reader reader) throws IOException {
		return fromReader(reader, Tag.DEFAULT_MAX_DEPTH);
	}

	public NamedTag fromString(String s) throws IOException {
		return fromReader(new StringReader(s));
	}

	@Override
	public NamedTag fromStream(InputStream stream) throws IOException {
		try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			return fromReader(reader);
		}
	}

	@Override
	public NamedTag fromFile(File file) throws IOException {
		try (Reader reader = new FileReader(file)) {
			return fromReader(reader);
		}
	}

	@Override
	public NamedTag fromBytes(byte[] data) throws IOException {
		return fromReader(new StringReader(new String(data)));
	}
}
