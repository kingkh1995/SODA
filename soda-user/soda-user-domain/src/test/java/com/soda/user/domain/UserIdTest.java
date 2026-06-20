package com.soda.user.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.types.LongId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UserIdTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();


    private static final long VALID_ID = 1L;

    @Test
    void constructor_validId_createsId() {
        var id = new UserId(VALID_ID);
        assertEquals(VALID_ID, id.value());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void constructor_nonPositive_throws(long invalid) {
        assertThrows(IllegalArgumentException.class, () -> new UserId(invalid));
    }

    @Test
    void valueOf_long_createsId() {
        assertEquals(new UserId(VALID_ID), UserId.valueOf(VALID_ID));
    }

    @Test
    void valueOf_string_parses() {
        assertEquals(new UserId(VALID_ID), UserId.valueOf("1"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-a-number", ""})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> UserId.valueOf(invalid));
    }


    @Test
    void identifier_returnsLong() {
        var id = new UserId(42L);
        assertEquals(42L, id.identifier());
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new UserId(VALID_ID), new UserId(VALID_ID));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new UserId(1L), new UserId(2L));
    }

    @Test
    void compareTo_delegatesToLongCompare() {
        assertTrue(new UserId(1L).compareTo(new UserId(2L)) < 0);
        assertTrue(new UserId(2L).compareTo(new UserId(1L)) > 0);
        assertEquals(0, new UserId(1L).compareTo(new UserId(1L)));
    }

    @Test
    void jackson_serializeDeserialize() {
        try {
            var original = new UserId(42L);
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, UserId.class);
            assertEquals(original, restored);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void jackson_serializesAsNumber() {
        try {
            assertEquals("42", MAPPER.writeValueAsString(new UserId(42L)));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void toLongId_convertsUserIdToLongId() {
        var id = new UserId(42L);
        assertEquals(LongId.valueOf(42L), id.toLongId());
    }

    @Test
    void toLongId_roundTripProducesSameValue() {
        var id = new UserId(42L);
        assertEquals(id.value(), id.toLongId().value());
    }
}
