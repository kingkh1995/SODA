package com.soda.component.support.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ParseUtils} behavior tests.
 */
class ParseUtilsTest {

    // ——— parseInt ———

    @Test
    void parseInt_integer_identity() {
        assertEquals(42, ParseUtils.parseInt(42));
    }

    @Test
    void parseInt_byte_widening() {
        assertEquals(7, ParseUtils.parseInt((byte) 7));
    }

    @Test
    void parseInt_short_widening() {
        assertEquals(300, ParseUtils.parseInt((short) 300));
    }

    @Test
    void parseInt_validString_parsed() {
        assertEquals(123, ParseUtils.parseInt("123"));
    }

    @Test
    void parseInt_stringWithSpaces_trimmed() {
        assertEquals(456, ParseUtils.parseInt("  456  "));
    }

    @Test
    void parseInt_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseInt(null));
    }

    @Test
    void parseInt_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseInt("abc"));
    }

    @Test
    void parseInt_unsupportedType_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseInt(new Object()));
    }

    // ——— parseLong ———

    @Test
    void parseLong_long_identity() {
        assertEquals(42L, ParseUtils.parseLong(42L));
    }

    @Test
    void parseLong_integer_widening() {
        assertEquals(99L, ParseUtils.parseLong(99));
    }

    @Test
    void parseLong_byte_widening() {
        assertEquals(5L, ParseUtils.parseLong((byte) 5));
    }

    @Test
    void parseLong_short_widening() {
        assertEquals(200L, ParseUtils.parseLong((short) 200));
    }

    @Test
    void parseLong_validString_parsed() {
        assertEquals(Long.MAX_VALUE, ParseUtils.parseLong(String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void parseLong_stringWithSpaces_trimmed() {
        assertEquals(789L, ParseUtils.parseLong("  789  "));
    }

    @Test
    void parseLong_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseLong(null));
    }

    @Test
    void parseLong_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseLong("xyz"));
    }

    @Test
    void parseLong_unsupportedType_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseLong(new Object()));
    }

    // ——— parseString ———

    @Test
    void parseString_valid_returnsToString() {
        assertEquals("hello", ParseUtils.parseString("hello"));
    }

    @Test
    void parseString_integer_returnsToString() {
        assertEquals("42", ParseUtils.parseString(42));
    }

    @Test
    void parseString_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseString(null));
    }
}
