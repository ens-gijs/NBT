package io.github.ensgijs.nbt.util;

/**
 * Very forgiving json parser and formatter.
 * Logic strongly based on implementation of https://jsonviewer.stack.hu/
 */
public final class JsonPrettyPrinter {
    /**
     * Default max length for a leaf object/array tag to be kept on a single line.
     * <ul>
     *   <li>Yes, your application can modify this default.
     *   <li>Controls the default behavior of {@link #prettyPrintJson(String)}.
     *   <li>Set to zero to disable one-lining feature by default. Empty tags are still one-lined.
     *   <li>This length is measured from the start of the line, the tags closing bracket and any following comma are counted.
     * </ul>
     */
    public static int DEFAULT_SINGLE_LINE_MAX_LENGTH = 120;

    private JsonPrettyPrinter() {}

    /**
     * Pretty prints a JSON string using the default single-line length limit {@link #DEFAULT_SINGLE_LINE_MAX_LENGTH}.
     * @param jsonText The raw JSON string.
     * @return A formatted JSON string.
     * @see #DEFAULT_SINGLE_LINE_MAX_LENGTH
     */
    public static String prettyPrintJson(final String jsonText) {
        return prettyPrintJson(jsonText, DEFAULT_SINGLE_LINE_MAX_LENGTH);
    }

    /**
     * Pretty prints a JSON string.
     * @param jsonText The raw JSON string.
     * @param singleLineMaxLength Attempt to place leaf tags on one line so long as the line does not exceed this limit.
     *                            Specify 0 to disable this feature (only empty lists/objects will be one-lined).
     * @return A formatted JSON string.
     */
    public static String prettyPrintJson(final String jsonText, final int singleLineMaxLength) {
        StringBuilder sb = new StringBuilder();
        int startOfLinePos = 0;
        int indentationLevel = 0;
        boolean inString = false;
        char strQuoteChar = '\0';
        scan: for (int i = 0, jsonLen = jsonText.length(); i < jsonLen; i++) {
            char c = jsonText.charAt(i);
            if (inString && c == strQuoteChar) {
                if (jsonText.charAt(i - 1) != '\\') {
                    inString = false;
                }
                sb.append(c);
            } else if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                strQuoteChar = c;
                sb.append(c);
            } else if (!inString && Character.isWhitespace(c)) {
                continue;
            } else if (!inString && c == ':') {
                sb.append(c).append(' ');
            } else if (!inString && c == ',') {
                // Identify hanging commas. If found, don't add a newline to avoid a double-space.
                for (int j = i + 1; j < jsonText.length(); j++) {
                    c = jsonText.charAt(j);
                    if (Character.isWhitespace(c))
                        continue;
                    if (c != ']' && c != '}') {
                        sb.append(",\n");
                        startOfLinePos = sb.length();
                        appendIndent(sb, indentationLevel);
                        continue scan;
                    }
                }
                sb.append(',');
            } else if (!inString && (c == '[' || c == '{')) {
                if (singleLineMaxLength > 0 && singleLineMaxLength >= indentationLevel * 2 + 4) {
                    int closingIndex = findMatchingBracket(jsonText, i, singleLineMaxLength - (sb.length() - startOfLinePos));
                    if (closingIndex != -1 && appendSingleLineFragment(sb, jsonText, i, closingIndex + 1, singleLineMaxLength - (sb.length() - startOfLinePos))) {
                        i = closingIndex;
                        continue;
                    }

                    indentationLevel++;
                    sb.append(c);
                    // keep int/long tag array hint on same line next to open bracket
                    if (i + 2 < jsonLen && jsonText.charAt(i + 2) == ';') {
                        sb.append(jsonText.charAt(i + 1)).append(';');
                        i += 2;
                    }
                    sb.append('\n');
                    startOfLinePos = sb.length();
                    appendIndent(sb, indentationLevel);
                } else {
                    indentationLevel++;
                    sb.append(c);
                    // keep int/long tag array hint on same line next to open bracket
                    if (i + 2 < jsonLen && jsonText.charAt(i + 2) == ';') {
                        sb.append(jsonText.charAt(i + 1)).append(';');
                        i += 2;
                    }
                    if (i + 1 < jsonLen && jsonText.charAt(i + 1) == ']') {
                        sb.append(']');
                        i ++;
                        indentationLevel--;
                    } else {
                        sb.append('\n');
                        startOfLinePos = sb.length();
                        appendIndent(sb, indentationLevel);
                    }
                }
            } else if (!inString && c == ']') {
                indentationLevel--;
                sb.append('\n');
                startOfLinePos = sb.length();
                appendIndent(sb, indentationLevel);
                sb.append(c);
            } else if (!inString && c == '}') {
                indentationLevel--;
                if (jsonText.charAt(i - 1) != '{') {
                    sb.append('\n');
                    startOfLinePos = sb.length();
                    appendIndent(sb, indentationLevel);
                } else {
                    sb.setLength(sb.lastIndexOf("{") + 1);
                }
                sb.append(c);
            } else if (inString && c == '\n') {
                sb.append("\\n");
            } else if (c != '\r') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int findMatchingBracket(final String jsonText, final int startIndex, final int maxSearchDistance) {
        final char openBracket = jsonText.charAt(startIndex);
        char closeBracket;
        if (openBracket == '{') closeBracket = '}';
        else if (openBracket == '[') closeBracket = ']';
        else return -1;

        boolean inString = false;
        char strQuoteChar = '\0';
        int distanceRemaining = maxSearchDistance;

        for (int i = startIndex + 1; i < jsonText.length() && distanceRemaining > 0; i++) {
            char c = jsonText.charAt(i);
            if (!Character.isWhitespace(c)) distanceRemaining--;
            if (inString) {
                if (c == strQuoteChar && jsonText.charAt(i - 1) != '\\') {
                    inString = false;
                }
            } else {
                if (c == '"' || c == '\'') {
                    inString = true;
                    strQuoteChar = c;
                } else if (c == '{' || c == '[') {
                    return -1; // cancel search when a nested structure is encountered
                } else if (c == closeBracket) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Appends a compact, single-line version of a JSON fragment to the output.
     * This method removes newlines and ensures proper spacing around delimiters.
     */
    private static boolean appendSingleLineFragment(final StringBuilder output, final String jsonText, final int start, final int end, final int distanceToEOL) {
        boolean inString = false;
        char strQuoteChar = '\0';
        final int p = output.length();
        int i;
        for (i = start; i < end; i++) {
            char c = jsonText.charAt(i);
            if (inString) {
                if (c == strQuoteChar && jsonText.charAt(i - 1) != '\\') {
                    inString = false;
                }
                output.append(c);
            } else if (c == '"' || c == '\'') {
                inString = true;
                strQuoteChar = c;
                output.append(c);
            } else if (!Character.isWhitespace(c)) {
                // For any character that isn't whitespace...
                // Add a space after comma or colon, but not if it's the last char
                if ((c == ',' || c == ':' || c == ';') && i + 1 < end) {
                    // Remove space if it's right before a closing bracket
                    if (!output.isEmpty() && output.charAt(output.length() - 1) == ' ') {
                        output.setLength(output.length() - 1);
                    }
                    output.append(c).append(' ');
                } else {
                    // Remove trailing space before a closing bracket
                    if ((c == ']' || c == '}') && !output.isEmpty() && output.charAt(output.length() - 1) == ' ') {
                        output.setLength(output.length() - 1);
                    }
                    output.append(c);
                }
            }
        }
        final int len = output.length() - p;
        if (len <= distanceToEOL) {
            if (len < distanceToEOL)
                return true;

            // We are right at the length limit.
            // Scan ahead to see if a comma comes next.
            // If one does, it'll make the line too long.
            for (; i < jsonText.length(); i++) {
                char c = jsonText.charAt(i);
                if (c == ',')
                    break;
                if (!Character.isWhitespace(c))
                    return true;
            }
        }
        output.setLength(p);
        return false;
    }

    private static void appendIndent(final StringBuilder output, final int indentationLevel) {
        output.append(" ".repeat(indentationLevel * 2));
    }
}
