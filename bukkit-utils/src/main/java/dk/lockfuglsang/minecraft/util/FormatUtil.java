package dk.lockfuglsang.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Format utility
 */
public enum FormatUtil {;
    private static final Pattern FORMAT_CODE = Pattern.compile("\u00a7[0-9a-fklmor]");
    public static String stripFormatting(String format) {
        if (format == null || format.trim().isEmpty()) {
            return "";
        }
        return format.replaceAll("(\u00a7|&)[0-9a-fklmor]", "");
    }

    public static String normalize(String format) {
        if (format == null || format.trim().isEmpty()) {
            return "";
        }
        return format.replaceAll("(\u00a7|&)([0-9a-fklmor])", "\u00a7$2");
    }

    public static List<String> wordWrap(String s, int lineSize) {
        return wordWrap(s, lineSize, lineSize);
    }

    /**
     * Wraps the string rather lazyly around the linesize (over).
     *
     * I.e.
     * <pre>
     *   this is a line of words
     * </pre>
     * Will break into the following with linesize of 11:
     * <pre>
     *   this is a line
     *   of words
     * </pre>
     * Note that the first line is longer than 11 (14).
     * <p>
     * Each wrapped line starts with the colour and formats still in effect where the line before ended.
     */
    public static List<String> wordWrap(String s, int firstSegment, int lineSize) {
        List<String> words = new ArrayList<>();
        int numChars = firstSegment;
        int ix = 0;
        int jx = 0;
        while (ix < s.length()) {
            ix = s.indexOf(' ', ix+1);
            if (ix != -1) {
                String subString = s.substring(jx, ix).trim();
                int chars = stripFormatting(subString).length() + 1; // remember the space
                if (chars >= numChars) {
                    if (!subString.isEmpty()) {
                        words.add(withFormat(trailingFormat(s.substring(0, jx)), subString));
                        numChars = lineSize;
                        jx = ix + 1;
                    }
                }
            } else {
                break;
            }
        }
        words.add(withFormat(trailingFormat(s.substring(0, jx)), s.substring(jx).trim()));
        return words;
    }

    public static List<String> wordWrapStrict(String s, int lineLength) {
        List<String> lines = new ArrayList<>();
        String format = "";
        String[] words = s.split(" ");
        String line = "";
        for (String word: words) {
            String test = stripFormatting(line + " " + word).trim();
            if (test.length() <= lineLength) {
                // add word
                line += (line.isEmpty() ? "" : " ") + word;
            } else if (line.isEmpty() || stripFormatting(word).length() > lineLength) {
                // add word truncated
                String wordFormat = trailingFormat(format + line + word);
                String strip = stripFormatting(word);
                do {
                    int len = Math.min(strip.length(), lineLength-line.length()-1);
                    lines.add(withFormat(format, line + (line.isEmpty() ? "" : " ") + strip.substring(0, len)));
                    strip = strip.substring(len);
                    format = wordFormat;
                } while (strip.length() > lineLength);
                line = strip;
            } else {
                // add line, then start a new
                lines.add(withFormat(format, line));
                format = trailingFormat(format + line);
                line = word;
            }
        }
        if (!line.isEmpty()) {
            lines.add(withFormat(format, line));
        }
        return lines;
    }

    /** A line that opens with its own colour or reset doesn't need the carried format. */
    private static String withFormat(String format, String line) {
        return line.length() > 1 && line.charAt(0) == '\u00a7' && isColourOrReset(line.charAt(1)) ? line : format + line;
    }

    /** The codes still in effect after {@code s}: a colour or {@code \u00a7r} replaces them, the others add to them. */
    private static String trailingFormat(String s) {
        StringBuilder format = new StringBuilder();
        Matcher m = FORMAT_CODE.matcher(s);
        while (m.find()) {
            if (isColourOrReset(m.group().charAt(1))) {
                format.setLength(0);
            }
            if (format.indexOf(m.group()) == -1) {
                format.append(m.group());
            }
        }
        return format.toString();
    }

    private static boolean isColourOrReset(char code) {
        return "0123456789abcdefr".indexOf(code) >= 0;
    }

    public static List<String> prefix(List<String> list, String prefix) {
        List<String> prefixed = new ArrayList<>(list.size());
        for (String s : list) {
            prefixed.add(prefix + s);
        }
        return prefixed;
    }

    public static String camelcase(String name) {
        return capitalize(name, "");
    }

    public static String capitalize(String name) {
        return capitalize(name, " ");
    }

    public static String capitalize(String name, String separator) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : name.split("[ _]")) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Escapes formatting by "denormalizing" back to using &amp; instead of §.
     * @param formatString A formatstring (formerly normalized).
     * @return A non-format string using &amp; instead of §.
     * @since 1.10
     */
    public static String escape(String formatString) {
        String escaped = normalize(formatString);
        escaped = escaped
                .replaceAll("\u00a7", "&");
        return escaped;
    }
}
