package com.soda.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsernameTest {

    private static final String VALID_USERNAME = "testuser";
    private static final String LONG_USERNAME = "a".repeat(31);

    @Test
    void constructor_validUsername_creates() {
        var u = new Username(VALID_USERNAME);
        assertEquals(VALID_USERNAME, u.value());
    }

    @Test
    void constructor_whitespaceAround_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Username("  " + VALID_USERNAME + "  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "abc", "user name!", "用户名"})
    void constructor_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Username(invalid));
    }

    @Test
    void constructor_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Username(LONG_USERNAME));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new Username(VALID_USERNAME), new Username(VALID_USERNAME));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new Username("userA"), new Username("userB"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        assertTrue(new Username("aaaa").compareTo(new Username("bbbb")) < 0);
        assertTrue(new Username("bbbb").compareTo(new Username("aaaa")) > 0);
        assertEquals(0, new Username("aaaa").compareTo(new Username("aaaa")));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new Username(VALID_USERNAME);
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, Username.class);
        assertEquals(original, restored);
    }
}
