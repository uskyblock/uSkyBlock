package us.talabrek.ultimateskyblock.ittest.result;

import java.util.LinkedHashMap;
import java.util.Map;

final class JsonLine {
    private JsonLine() {
    }

    static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    static Map<String, String> parseObject(String input) {
        Parser parser = new Parser(input);
        return parser.parse();
    }

    private static final class Parser {
        private final String input;
        private int offset;

        private Parser(String input) {
            this.input = input;
        }

        Map<String, String> parse() {
            LinkedHashMap<String, String> values = new LinkedHashMap<>();
            whitespace();
            expect('{');
            whitespace();
            if (peek('}')) {
                offset++;
                return values;
            }
            while (true) {
                String key = string();
                whitespace();
                expect(':');
                whitespace();
                String value = peek('"') ? string() : literal();
                if (values.putIfAbsent(key, value) != null) {
                    throw error("duplicate key " + key);
                }
                whitespace();
                if (peek('}')) {
                    offset++;
                    break;
                }
                expect(',');
                whitespace();
            }
            whitespace();
            if (offset != input.length()) {
                throw error("trailing data");
            }
            return values;
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (offset < input.length()) {
                char c = input.charAt(offset++);
                if (c == '"') {
                    return result.toString();
                }
                if (c != '\\') {
                    if (c < 0x20) throw error("control character in string");
                    result.append(c);
                    continue;
                }
                if (offset >= input.length()) throw error("incomplete escape");
                char escaped = input.charAt(offset++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (offset + 4 > input.length()) throw error("incomplete unicode escape");
                        try {
                            result.append((char) Integer.parseInt(input.substring(offset, offset + 4), 16));
                        } catch (NumberFormatException e) {
                            throw error("invalid unicode escape");
                        }
                        offset += 4;
                    }
                    default -> throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private String literal() {
            int start = offset;
            while (offset < input.length() && ",}".indexOf(input.charAt(offset)) < 0) offset++;
            String value = input.substring(start, offset).trim();
            if (value.isEmpty()) throw error("empty value");
            return value;
        }

        private void whitespace() {
            while (offset < input.length() && Character.isWhitespace(input.charAt(offset))) offset++;
        }

        private boolean peek(char c) {
            return offset < input.length() && input.charAt(offset) == c;
        }

        private void expect(char c) {
            if (!peek(c)) throw error("expected " + c);
            offset++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at offset " + offset);
        }
    }
}
