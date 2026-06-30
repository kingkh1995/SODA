package com.soda.component.support.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"abc"})
    void parseInt_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseInt(value));
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"xyz"})
    void parseLong_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseLong(value));
    }

    @Test
    void parseLong_unsupportedType_throws() {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseLong(new Object()));
    }


    // ——— parseUri ———

    @Test
    void parseUri_http_passes() {
        ParseUtils.parseUri("http://example.com");
    }

    @Test
    void parseUri_https_passes() {
        ParseUtils.parseUri("https://example.com/path?q=1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void parseUri_blank_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseUri(value));
    }

    @Test
    void parseUri_relative_passes() {
        assertEquals(URI.create("not-a-uri"), ParseUtils.parseUri("not-a-uri"));
    }

    // ——— parseEnum ———

    @Test
    void parseEnum_validString_parsed() {
        assertEquals(Thread.State.RUNNABLE, ParseUtils.parseEnum(Thread.State.class, "RUNNABLE"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"NO_SUCH_STATE"})
    void parseEnum_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ParseUtils.parseEnum(Thread.State.class, value));
    }
}
