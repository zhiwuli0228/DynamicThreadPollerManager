package com.zhiwu.dynamicthreadpollermanager.experiment.acquisition;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcquisitionJsonWriterParseTest {

    @Test
    void shouldParseEmptyObject() {
        Object result = AcquisitionJsonWriter.parse("{}");
        assertTrue(result instanceof Map);
        assertTrue(((Map<?, ?>) result).isEmpty());
    }

    @Test
    void shouldParseEmptyArray() {
        Object result = AcquisitionJsonWriter.parse("[]");
        assertTrue(result instanceof List);
        assertTrue(((List<?>) result).isEmpty());
    }

    @Test
    void shouldParseString() {
        Object result = AcquisitionJsonWriter.parse("\"hello\"");
        assertEquals("hello", result);
    }

    @Test
    void shouldParseNull() {
        Object result = AcquisitionJsonWriter.parse("null");
        assertNull(result);
    }

    @Test
    void shouldParseTrue() {
        Object result = AcquisitionJsonWriter.parse("true");
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void shouldParseFalse() {
        Object result = AcquisitionJsonWriter.parse("false");
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void shouldParseIntegerAsLong() {
        Object result = AcquisitionJsonWriter.parse("42");
        assertEquals(42L, result);
    }

    @Test
    void shouldParseDecimalAsDouble() {
        Object result = AcquisitionJsonWriter.parse("3.14");
        assertEquals(3.14, (Double) result, 0.001);
    }

    @Test
    void shouldParseNegativeNumber() {
        Object result = AcquisitionJsonWriter.parse("-10");
        assertEquals(-10L, result);
    }

    @Test
    void shouldParseNestedObject() {
        Object result = AcquisitionJsonWriter.parse("{\"a\":{\"b\":1}}");
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) map.get("a");
        assertEquals(1L, inner.get("b"));
    }

    @Test
    void shouldParseArrayInObject() {
        Object result = AcquisitionJsonWriter.parse("{\"items\":[1,2,3]}");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) map.get("items");
        assertEquals(3, items.size());
        assertEquals(1L, items.get(0));
        assertEquals(2L, items.get(1));
        assertEquals(3L, items.get(2));
    }

    @Test
    void shouldParseMixedArray() {
        Object result = AcquisitionJsonWriter.parse("[1,\"two\",true,null]");
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) result;
        assertEquals(4, list.size());
        assertEquals(1L, list.get(0));
        assertEquals("two", list.get(1));
        assertEquals(Boolean.TRUE, list.get(2));
        assertNull(list.get(3));
    }

    @Test
    void shouldRoundTripRenderThenParse() {
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("name", "test");
        original.put("count", 42L);     // parse returns Long
        original.put("active", true);

        String json = AcquisitionJsonWriter.render(original);
        Object parsed = AcquisitionJsonWriter.parse(json);

        assertEquals(original, parsed);
    }

    @Test
    void shouldThrowOnInvalidJson() {
        assertThrows(IllegalArgumentException.class, () ->
                AcquisitionJsonWriter.parse("{invalid}"));
    }

    @Test
    void shouldThrowOnUnterminatedString() {
        assertThrows(IllegalArgumentException.class, () ->
                AcquisitionJsonWriter.parse("\"unterminated"));
    }

    @Test
    void shouldThrowOnTrailingComma() {
        assertThrows(IllegalArgumentException.class, () ->
                AcquisitionJsonWriter.parse("{\"a\":1,}"));
    }

    @Test
    void shouldThrowOnEmptyInput() {
        assertThrows(IllegalArgumentException.class, () ->
                AcquisitionJsonWriter.parse(""));
    }

    @Test
    void shouldParseStringWithEscapes() {
        Object result = AcquisitionJsonWriter.parse("\"hello\\nworld\\t!\"");
        assertEquals("hello\nworld\t!", result);
    }
}
