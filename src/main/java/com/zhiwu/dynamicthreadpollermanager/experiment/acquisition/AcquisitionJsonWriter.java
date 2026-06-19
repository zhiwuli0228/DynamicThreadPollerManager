package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON serializer and parser for acquisition
 * report artifacts. Supports primitives, strings, lists, maps
 * and {@code null}. No external dependency is introduced.
 */
public final class AcquisitionJsonWriter {

    private AcquisitionJsonWriter() {
    }

    static String render(Object value) {
        StringBuilder sb = new StringBuilder();
        renderValue(sb, value, 0);
        return sb.toString();
    }

    static String renderCompact(Object value) {
        StringBuilder sb = new StringBuilder();
        renderCompactValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void renderValue(StringBuilder sb, Object value, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof CharSequence cs) {
            renderString(sb, cs.toString());
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            renderObject(sb, (Map<String, Object>) map, indent);
        } else if (value instanceof Iterable<?> it) {
            renderArray(sb, it, indent);
        } else if (value.getClass().isEnum()) {
            renderString(sb, ((Enum<?>) value).name());
        } else {
            renderString(sb, value.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static void renderCompactValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof CharSequence cs) {
            renderString(sb, cs.toString());
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            renderCompactObject(sb, (Map<String, Object>) map);
        } else if (value instanceof Iterable<?> it) {
            renderCompactArray(sb, it);
        } else if (value.getClass().isEnum()) {
            renderString(sb, ((Enum<?>) value).name());
        } else {
            renderString(sb, value.toString());
        }
    }

    private static void renderObject(StringBuilder sb, Map<String, Object> map, int indent) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{\n");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            appendIndent(sb, indent + 1);
            renderString(sb, entry.getKey());
            sb.append(": ");
            renderValue(sb, entry.getValue(), indent + 1);
            if (++i < map.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        appendIndent(sb, indent);
        sb.append("}");
    }

    private static void renderCompactObject(StringBuilder sb, Map<String, Object> map) {
        if (map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            renderString(sb, entry.getKey());
            sb.append(":");
            renderCompactValue(sb, entry.getValue());
            if (++i < map.size()) {
                sb.append(",");
            }
        }
        sb.append("}");
    }

    private static void renderArray(StringBuilder sb, Iterable<?> it, int indent) {
        List<Object> list = new ArrayList<>();
        for (Object element : it) {
            list.add(element);
        }
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            appendIndent(sb, indent + 1);
            renderValue(sb, list.get(i), indent + 1);
            if (i < list.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        appendIndent(sb, indent);
        sb.append("]");
    }

    private static void renderCompactArray(StringBuilder sb, Iterable<?> it) {
        List<Object> list = new ArrayList<>();
        for (Object element : it) {
            list.add(element);
        }
        if (list.isEmpty()) {
            sb.append("[]");
            return;
        }
        sb.append("[");
        int i = 0;
        for (Object element : list) {
            renderCompactValue(sb, element);
            if (++i < list.size()) {
                sb.append(",");
            }
        }
        sb.append("]");
    }

    private static void renderString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static void appendIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }

    static Map<String, Object> map() {
        return new LinkedHashMap<>();
    }

    public static Object parse(String json) {
        return new JsonParser(json).parseValue();
    }

    private static final class JsonParser {
        private final String input;
        private int pos;

        JsonParser(String input) {
            this.input = input;
            this.pos = 0;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new IllegalArgumentException("unexpected end of JSON input");
            }
            char c = input.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        yield parseNumber();
                    }
                    throw new IllegalArgumentException(
                            "unexpected character '%c' at position %d".formatted(c, pos));
                }
            };
        }

        Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> map = new LinkedHashMap<>();
            if (input.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (input.charAt(pos) == '}') {
                    pos++;
                    return map;
                }
                expect(',');
            }
        }

        List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> list = new ArrayList<>();
            if (input.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (input.charAt(pos) == ']') {
                    pos++;
                    return list;
                }
                expect(',');
            }
        }

        String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '"') {
                    pos++;
                    return sb.toString();
                }
                if (c == '\\') {
                    pos++;
                    if (pos >= input.length()) {
                        throw new IllegalArgumentException("unexpected end of string escape");
                    }
                    char escaped = input.charAt(pos);
                    switch (escaped) {
                        case '"', '\\', '/' -> sb.append(escaped);
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            if (pos + 4 >= input.length()) {
                                throw new IllegalArgumentException("unexpected end of unicode escape");
                            }
                            String hex = input.substring(pos + 1, pos + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> throw new IllegalArgumentException(
                                "invalid escape character '%c'".formatted(escaped));
                    }
                } else {
                    sb.append(c);
                }
                pos++;
            }
            throw new IllegalArgumentException("unterminated string");
        }

        Number parseNumber() {
            int start = pos;
            if (input.charAt(pos) == '-') {
                pos++;
            }
            while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                pos++;
            }
            boolean isFloating = false;
            if (pos < input.length() && input.charAt(pos) == '.') {
                isFloating = true;
                pos++;
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                    pos++;
                }
            }
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                isFloating = true;
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                    pos++;
                }
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                    pos++;
                }
            }
            String numStr = input.substring(start, pos);
            if (isFloating) {
                return Double.parseDouble(numStr);
            }
            long longVal = Long.parseLong(numStr);
            if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
                return longVal;
            }
            return longVal;
        }

        Boolean parseBoolean() {
            if (input.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (input.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("invalid boolean literal at position " + pos);
        }

        Object parseNull() {
            if (input.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("invalid null literal at position " + pos);
        }

        void expect(char c) {
            skipWhitespace();
            if (pos >= input.length() || input.charAt(pos) != c) {
                throw new IllegalArgumentException(
                        "expected '%c' at position %d but got '%c'"
                                .formatted(c, pos, pos < input.length() ? input.charAt(pos) : ' '));
            }
            pos++;
        }

        void skipWhitespace() {
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }
    }
}
