package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LongId} behavior tests.
 * <p>
 * Tests verify through public API only — compact constructor, {@code parse()},
 * {@code identifier()}, {@code compareTo()}, and Jackson round-trip.
 */
class LongIdTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ——— constructor (primary entry) ———

    @Test
    void constructor_createsWithValue() {
        var id = new LongId(42L);
        assertEquals(42L, id.value());
    }

    @Test
    void constructor_acceptsOne() {
        var id = new LongId(1L);
        assertEquals(1L, id.value());
    }

    @Test
    void constructor_acceptsAllNumberTypes() {
        assertEquals(new LongId(1), new LongId(1L));
        assertEquals(new LongId(42), new LongId(42));
        assertEquals(new LongId(1), new LongId((byte) 1));
    }

    // ——— parse(String) ———

    @Test
    void parse_string_creates() {
        assertEquals(new LongId(99), LongId.parse("99"));
    }

    @Test
    void parse_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> LongId.parse(null));
    }

    @Test
    void parse_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> LongId.parse("not-a-number"));
    }

    // ——— identifier() ———

    @Test
    void identifier_returnsBoxedLong() {
        var id = new LongId(77);
        assertEquals(Long.valueOf(77), id.identifier());
    }

    @Test
    void identifier_matchesValue() {
        var id = new LongId(42L);
        assertEquals(id.value(), id.identifier().longValue());
    }

    // ——— compareTo ———

    @Test
    void compareTo_byNumericValue() {
        assertTrue(new LongId(1).compareTo(new LongId(2)) < 0);
        assertTrue(new LongId(5).compareTo(new LongId(3)) > 0);
        assertEquals(0, new LongId(4).compareTo(new LongId(4)));
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        JacksonTestUtil.assertRoundTrip(new LongId(42), LongId.class);
    }

    @Test
    void jackson_serializesAsBareNumber() throws Exception {
        assertEquals("42", MAPPER.writeValueAsString(new LongId(42)));
    }
}
