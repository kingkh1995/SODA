package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

class RawPasswordTest {

    @Test
    void constructor_valid_creates() {
        var pwd = new RawPassword("mySecret123");
        assertEquals("mySecret123", pwd.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_nullOrEmpty_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new RawPassword(invalid));
    }

    @Test
    void valueOf_string_creates() {
        assertEquals(new RawPassword("pwd"), RawPassword.valueOf("pwd"));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> RawPassword.valueOf(null));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new RawPassword("x"), new RawPassword("x"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        assertTrue(new RawPassword("a").compareTo(new RawPassword("b")) < 0);
        assertEquals(0, new RawPassword("x").compareTo(new RawPassword("x")));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new RawPassword("hunter2");
        JacksonTestUtil.assertRoundTrip(original, RawPassword.class);
    }
}
