package com.soda.user.domain;

import com.soda.component.support.types.LongId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIdTest {

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

    // ——— parse(String) ———

    @Test
    void parse_string_parses() {
        assertEquals(new UserId(VALID_ID), UserId.parse("1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", ""})
    void parse_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> UserId.parse(invalid));
    }

    @Test
    void parse_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> UserId.parse(null));
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
    void jackson_serializeDeserialize() throws Exception {
        var original = new UserId(42L);
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, UserId.class);
        assertEquals(original, restored);
    }

    @Test
    void jackson_serializesAsNumber() throws Exception {
        assertEquals("42", MAPPER.writeValueAsString(new UserId(42L)));
    }

    @Test
    void toLongId_convertsUserIdToLongId() {
        var id = new UserId(42L);
        assertEquals(new LongId(42L), id.toLongId());
    }

    @Test
    void toLongId_roundTripProducesSameValue() {
        var id = new UserId(42L);
        assertEquals(id.value(), id.toLongId().value());
    }
}
