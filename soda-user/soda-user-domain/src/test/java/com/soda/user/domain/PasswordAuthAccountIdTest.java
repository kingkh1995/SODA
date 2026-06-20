package com.soda.user.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.types.LongId;
import org.junit.jupiter.api.Test;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PasswordAuthAccountIdTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Test
    void from_createsWithPrefix() {
        var userId = new UserId(42L);
        var id = PasswordAuthAccountId.from(userId);
        assertEquals("P:42", id.value());
        assertEquals(userId, id.userId());
    }

    @Test
    void from_equivalentToValueOf() {
        var userId = new UserId(42L);
        assertEquals(PasswordAuthAccountId.from(userId), PasswordAuthAccountId.valueOf("P:42"));
    }

    @Test
    void valueOf_string_creates() {
        assertEquals("P:42", PasswordAuthAccountId.valueOf("P:42").value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "P:", "Q:42", "42"})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> PasswordAuthAccountId.valueOf(invalid));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> PasswordAuthAccountId.valueOf(null));
    }

    @Test
    void identifier_returnsString() {
        var id = PasswordAuthAccountId.from(new UserId(42L));
        assertEquals("P:42", id.identifier());
    }

    @Test
    void authAccountType_returnsP() {
        assertEquals(AuthAccountType.P, PasswordAuthAccountId.ACCOUNT_TYPE);
    }

    @Test
    void equal_whenSameValue() {
        var a = PasswordAuthAccountId.from(new UserId(1L));
        var b = PasswordAuthAccountId.from(new UserId(1L));
        assertEquals(a, b);
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(
                PasswordAuthAccountId.from(new UserId(1L)),
                PasswordAuthAccountId.from(new UserId(2L)));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        var a = PasswordAuthAccountId.from(new UserId(1L));
        var b = PasswordAuthAccountId.from(new UserId(2L));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }

    @Test
    void jackson_serializeDeserialize() {
        try {
            var original = PasswordAuthAccountId.from(new UserId(42L));
            var json = MAPPER.writeValueAsString(original);
            assertEquals("\"P:42\"", json);
            var restored = MAPPER.readValue(json, PasswordAuthAccountId.class);
            assertEquals(original, restored);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void toLongId_convertsToLongId() {
        var id = PasswordAuthAccountId.from(new UserId(42L));
        assertEquals(LongId.valueOf(42L), id.toLongId());
    }

    @Test
    void toLongId_roundTripProducesSameValue() {
        var id = PasswordAuthAccountId.from(new UserId(42L));
        assertEquals(42L, id.toLongId().value());
    }
}
