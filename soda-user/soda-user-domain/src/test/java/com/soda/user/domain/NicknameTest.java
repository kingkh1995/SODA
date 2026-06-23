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

class NicknameTest {


    private static final String VALID_NICKNAME = "张三";

    @Test
    void constructor_validNickname_creates() {
        var n = new Nickname(VALID_NICKNAME);
        assertEquals(VALID_NICKNAME, n.value());
    }

    @Test
    void constructor_trimmed() {
        var n = new Nickname("  " + VALID_NICKNAME + "  ");
        assertEquals(VALID_NICKNAME, n.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void constructor_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Nickname(invalid));
    }

    @Test
    void valueOf_string_creates() {
        assertEquals(new Nickname(VALID_NICKNAME), new Nickname(VALID_NICKNAME));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Nickname(null));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new Nickname(VALID_NICKNAME), new Nickname(VALID_NICKNAME));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new Nickname("A"), new Nickname("B"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        assertTrue(new Nickname("A").compareTo(new Nickname("B")) < 0);
        assertTrue(new Nickname("B").compareTo(new Nickname("A")) > 0);
        assertEquals(0, new Nickname("A").compareTo(new Nickname("A")));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new Nickname(VALID_NICKNAME);
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, Nickname.class);
        assertEquals(original, restored);
    }
}
