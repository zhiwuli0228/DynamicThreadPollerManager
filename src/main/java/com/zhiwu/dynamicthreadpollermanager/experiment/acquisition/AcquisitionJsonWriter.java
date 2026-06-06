package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Package-private minimal JSON serializer for acquisition
 * report artifacts. Supports primitives, strings, lists, maps
 * and {@code null}. No external dependency is introduced.
 */
final class AcquisitionJsonWriter {

    private AcquisitionJsonWriter() {
    }

    static String render(Object value) {
        StringBuilder sb = new StringBuilder();
        renderValue(sb, value, 0);
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
}
