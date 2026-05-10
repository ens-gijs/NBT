package io.github.ensgijs.nbt.io;

import io.github.ensgijs.nbt.tag.ArrayTag;
import io.github.ensgijs.nbt.tag.ByteArrayTag;
import io.github.ensgijs.nbt.tag.ByteTag;
import io.github.ensgijs.nbt.tag.CompoundTag;
import io.github.ensgijs.nbt.tag.DoubleTag;
import io.github.ensgijs.nbt.tag.EndTag;
import io.github.ensgijs.nbt.tag.FloatTag;
import io.github.ensgijs.nbt.tag.IntArrayTag;
import io.github.ensgijs.nbt.tag.IntTag;
import io.github.ensgijs.nbt.tag.ListTag;
import io.github.ensgijs.nbt.tag.LongArrayTag;
import io.github.ensgijs.nbt.tag.LongTag;
import io.github.ensgijs.nbt.tag.ShortTag;
import io.github.ensgijs.nbt.tag.StringTag;
import io.github.ensgijs.nbt.tag.Tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class TextNbtParser implements MaxDepthIO, NbtInput {

	private static final Pattern
			FLOAT_LITERAL_PATTERN = Pattern.compile("^[-+]?(?:\\d+\\.?|\\d*\\.\\d+)(?:e[-+]?\\d+)?f$", Pattern.CASE_INSENSITIVE),
			DOUBLE_LITERAL_PATTERN = Pattern.compile("^[-+]?(?:\\d+\\.?|\\d*\\.\\d+)(?:e[-+]?\\d+)?d$", Pattern.CASE_INSENSITIVE),
			DOUBLE_LITERAL_NO_SUFFIX_PATTERN = Pattern.compile("^[-+]?(?:\\d+\\.|\\d*\\.\\d+)(?:e[-+]?\\d+)?$", Pattern.CASE_INSENSITIVE),
			BYTE_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+b$", Pattern.CASE_INSENSITIVE),
			SHORT_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+s$", Pattern.CASE_INSENSITIVE),
			INT_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+$", Pattern.CASE_INSENSITIVE),
			LONG_LITERAL_PATTERN = Pattern.compile("^[-+]?\\d+l$", Pattern.CASE_INSENSITIVE),
			NUMBER_PATTERN = Pattern.compile("^[-+]?\\d+$");

	private StringPointer ptr;

	public TextNbtParser(String string) {
		this.ptr = new StringPointer(string);
	}

	@Override
	public NamedTag readTag(int maxDepth) throws IOException {
		ptr.reset();
		ptr.skipWhitespace();
		if (!ptr.hasNext()) return null;
		String name = ptr.currentChar() == '"' ? ptr.parseQuotedString() : ptr.parseSimpleString();
		// note to future self: if you're ever compelled to set NamedTag's name to null if it's empty
		// consider changing TextNbtWriter#writeAnything(NamedTag, int)'s behavior to match
		ptr.skipWhitespace();
		if (ptr.hasNext() && ptr.next() ==':') {
			ptr.skipWhitespace();
			if (!ptr.hasNext()) {
				throw ptr.parseException("unexpected end of input - no value after name:");
			}
			return new NamedTag(name, parseAnything(maxDepth));
		}
		return new NamedTag(null, readRawTag(maxDepth));
	}

	@Override
	public Tag<?> readRawTag(int maxDepth) throws IOException {
		ptr.reset();
		ptr.skipWhitespace();
		if (!ptr.hasNext()) return null;
		return parseAnything(maxDepth);
	}

	/**
	 *
	 * @param maxDepth
	 * @param lenient allows trailing content to follow the text nbt data - this could be useful if multiple
	 *                   text nbt's are present without a ListTag being used.
	 * @return
	 * @throws ParseException
	 */
	public Tag<?> parse(int maxDepth, boolean lenient) throws ParseException {
		Tag<?> tag = parseAnything(maxDepth);
		if (!lenient) {
			ptr.skipWhitespace();
			if (ptr.hasNext()) {
				throw ptr.parseException("invalid characters after end of text nbt");
			}
		}
		return tag;
	}

	public Tag<?> parse(int maxDepth) throws ParseException {
		return parse(maxDepth, false);
	}

	public Tag<?> parse() throws ParseException {
		return parse(Tag.DEFAULT_MAX_DEPTH, false);
	}

	/**
	 * Useful for parsing a text nbt tag used in code - generally {@link #parse()}, or one of its overloads,
	 * should be used for all other situations.
	 * <p>Traps and rethrows any checked {@link ParseException}'s as a runtime
	 * {@link ParseException.SilentParseException}.</p>
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Tag<?>> T parseInline(String nbtText) throws ParseException.SilentParseException {
		try {
			return (T) new TextNbtParser(nbtText).parse();
		} catch (ParseException ex) {
			throw new ParseException.SilentParseException(ex);
		}
	}

	public int getReadChars() {
		return ptr.getIndex() + 1;
	}

	private Tag<?> parseAnything(int maxDepth) throws ParseException {
		ptr.skipWhitespace();
		switch (ptr.currentChar()) {
			case '{':
				return parseCompoundTag(maxDepth);
			case '[':
				if (ptr.hasCharsLeft(2) && ptr.lookAhead(1) != '"' && ptr.lookAhead(2) == ';') {
					return parseNumArray();
				}
				return parseListTag(maxDepth);
		}
		return parseStringOrLiteral();
	}

	private Tag<?> parseStringOrLiteral() throws ParseException {
		ptr.skipWhitespace();
		if (ptr.currentChar() == '"') {
			return new StringTag(ptr.parseQuotedString());
		}
		String s = ptr.parseSimpleString();
		if (s.isEmpty()) {
			throw ptr.parseException("expected non empty value");
		}
		if (FLOAT_LITERAL_PATTERN.matcher(s).matches()) {
			return new FloatTag(Float.parseFloat(s.substring(0, s.length() - 1)));
		} else if (BYTE_LITERAL_PATTERN.matcher(s).matches()) {
			try {
				return new ByteTag(Byte.parseByte(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException ex) {
				throw ptr.parseException("byte not in range: \"" + s.substring(0, s.length() - 1) + "\"");
			}
		} else if (SHORT_LITERAL_PATTERN.matcher(s).matches()) {
			try {
				return new ShortTag(Short.parseShort(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException ex) {
				throw ptr.parseException("short not in range: \"" + s.substring(0, s.length() - 1) + "\"");
			}
		} else if (LONG_LITERAL_PATTERN.matcher(s).matches()) {
			try {
				return new LongTag(Long.parseLong(s.substring(0, s.length() - 1)));
			} catch (NumberFormatException ex) {
				throw ptr.parseException("long not in range: \"" + s.substring(0, s.length() - 1) + "\"");
			}
		} else if (INT_LITERAL_PATTERN.matcher(s).matches()) {
			try {
				return new IntTag(Integer.parseInt(s));
			} catch (NumberFormatException ex) {
				throw ptr.parseException("int not in range: \"" + s.substring(0, s.length() - 1) + "\"");
			}
		} else if (DOUBLE_LITERAL_PATTERN.matcher(s).matches()) {
			return new DoubleTag(Double.parseDouble(s.substring(0, s.length() - 1)));
		} else if (DOUBLE_LITERAL_NO_SUFFIX_PATTERN.matcher(s).matches()) {
			return new DoubleTag(Double.parseDouble(s));
		} else if ("true".equalsIgnoreCase(s)) {
			return new ByteTag(true);
		} else if ("false".equalsIgnoreCase(s)) {
			return new ByteTag(false);
		}
		return new StringTag(s);
	}

	private CompoundTag parseCompoundTag(int maxDepth) throws ParseException {
		ptr.expectChar('{');

		CompoundTag compoundTag = new CompoundTag();

		ptr.skipWhitespace();
		while (ptr.hasNext() && ptr.currentChar() != '}') {
			ptr.skipWhitespace();
			String key = ptr.currentChar() == '"' ? ptr.parseQuotedString() : ptr.parseSimpleString();
			if (key.isEmpty()) {
				throw ptr.parseException("empty keys are not allowed");
			}
			ptr.expectChar(':');

			compoundTag.put(key, parseAnything(decrementMaxDepth(maxDepth)));

			if (!ptr.nextArrayElement()) {
				break;
			}
		}
		ptr.expectChar('}');
		return compoundTag;
	}

	private ListTag<?> parseListTag(int maxDepth) throws ParseException {
		ptr.expectChar('[');
		ptr.skipWhitespace();
		ListTag<?> list = ListTag.createUnchecked(EndTag.class);
		while (ptr.currentChar() != ']') {
			Tag<?> element = parseAnything(decrementMaxDepth(maxDepth));
			try {
				list.addUnchecked(element);
			} catch (IllegalArgumentException ex) {
				throw ptr.parseException(ex.getMessage());
			}
			if (!ptr.nextArrayElement()) {
				break;
			}
		}
		ptr.expectChar(']');
		return list;
	}

	private ArrayTag<?> parseNumArray() throws ParseException {
		ptr.expectChar('[');
		char arrayType = ptr.next();
		ptr.expectChar(';');
		ptr.skipWhitespace();
		switch (arrayType) {
			case 'B':
				return parseByteArrayTag();
			case 'I':
				return parseIntArrayTag();
			case 'L':
				return parseLongArrayTag();
		}
		throw ptr.parseException("invalid array type '" + arrayType + "'");
	}

	private ByteArrayTag parseByteArrayTag() throws ParseException {
		List<Byte> byteList = new ArrayList<>();
		while (ptr.currentChar() != ']') {
			String s = ptr.parseSimpleString();
			ptr.skipWhitespace();
			if (NUMBER_PATTERN.matcher(s).matches()) {
				try {
					byteList.add(Byte.parseByte(s));
				} catch (NumberFormatException ex) {
					throw ptr.parseException("byte not in range: \"" + s + "\"");
				}
			} else {
				throw ptr.parseException("invalid byte in ByteArrayTag: \"" + s + "\"");
			}
			if (!ptr.nextArrayElement()) {
				break;
			}
		}
		ptr.expectChar(']');
		byte[] bytes = new byte[byteList.size()];
		for (int i = 0; i < byteList.size(); i++) {
			bytes[i] = byteList.get(i);
		}
		return new ByteArrayTag(bytes);
	}

	private IntArrayTag parseIntArrayTag() throws ParseException {
		List<Integer> intList = new ArrayList<>();
		while (ptr.currentChar() != ']') {
			String s = ptr.parseSimpleString();
			ptr.skipWhitespace();
			if (NUMBER_PATTERN.matcher(s).matches()) {
				try {
					intList.add(Integer.parseInt(s));
				} catch (NumberFormatException ex) {
					throw ptr.parseException("int not in range: \"" + s + "\"");
				}
			} else {
				throw ptr.parseException("invalid int in IntArrayTag: \"" + s + "\"");
			}
			if (!ptr.nextArrayElement()) {
				break;
			}
		}
		ptr.expectChar(']');
		return new IntArrayTag(intList.stream().mapToInt(i -> i).toArray());
	}

	private LongArrayTag parseLongArrayTag() throws ParseException {
		List<Long> longList = new ArrayList<>();
		while (ptr.currentChar() != ']') {
			String s = ptr.parseSimpleString();
			ptr.skipWhitespace();
			if (NUMBER_PATTERN.matcher(s).matches()) {
				try {
					longList.add(Long.parseLong(s));
				} catch (NumberFormatException ex) {
					throw ptr.parseException("long not in range: \"" + s + "\"");
				}
			} else {
				throw ptr.parseException("invalid long in LongArrayTag: \"" + s + "\"");
			}
			if (!ptr.nextArrayElement()) {
				break;
			}
		}
		ptr.expectChar(']');
		return new LongArrayTag(longList.stream().mapToLong(l -> l).toArray());
	}
}
