package io.github.ensgijs.nbt.tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTag extends Tag<String> implements Comparable<StringTag> {

	private static final Pattern ESCAPE_PATTERN = Pattern.compile("[\\\\\n\t\r\"]");
	private static final Pattern LENIENT_NON_QUOTE_PATTERN = Pattern.compile("(?!true|false)[a-z_][a-z0-9_\\-]*", Pattern.CASE_INSENSITIVE);

	private static final Map<String, String> ESCAPE_CHARACTERS;
	static {
        ESCAPE_CHARACTERS = Map.of(
				"\\", "\\\\\\\\",
				"\n", "\\\\n",
				"\t", "\\\\t",
				"\r", "\\\\r",
				"\"", "\\\\\"");
	}

	public static final byte ID = 8;
	public static final String ZERO_VALUE = "";

	public StringTag() {
		super(ZERO_VALUE);
	}

	public StringTag(String value) {
		super(value);
	}

	@Override
	public byte getID() {
		return ID;
	}

	@Override
	public String getValue() {
		return super.getValue();
	}

	@Override
	public void setValue(String value) {
		super.setValue(value);
	}

	@Override
	public String valueToString(int maxDepth) {
		return escapeString(getValue(), false);
	}

	/**
	 * Escapes a string to fit into a JSON-like string representation for Minecraft
	 * or to create the JSON string representation of a Tag returned from {@link Tag#toString()}
	 * @param s The string to be escaped.
	 * @param lenient {@code false} if it should force double quotes ({@code "}) at the start and
	 *                the end of the string.
	 * @return The escaped string.
	 * */
	public static String escapeString(String s, boolean lenient) {
		StringBuffer sb = new StringBuffer();
		Matcher m = ESCAPE_PATTERN.matcher(s);
		while (m.find()) {
			m.appendReplacement(sb, ESCAPE_CHARACTERS.get(m.group()));
		}
		m.appendTail(sb);
		m = LENIENT_NON_QUOTE_PATTERN.matcher(s);
		if (!lenient || !m.matches()) {
			sb.insert(0, "\"").append("\"");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		return super.equals(other) && getValue().equals(((StringTag) other).getValue());
	}

	@Override
	public int compareTo(StringTag o) {
		return getValue().compareTo(o.getValue());
	}

	@Override
	public StringTag clone() {
		return new StringTag(getValue());
	}
}
