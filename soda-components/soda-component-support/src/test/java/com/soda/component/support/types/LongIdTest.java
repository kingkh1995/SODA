package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LongId} behavior tests.
 * <p>
 * Tests verify through public API only — {@code new LongId()}, {@code valueOf(Object)}, {@code identifier()},
 * Jackson round-trip, and contract guarantees (immutable, comparable, serializable).
 */
class LongIdTest {

    // ——— constructor ———

    @Test
    void constructor_positiveValue_createsLongId() {
        var id = new LongId(42);
        assertEquals(42, id.value());
    }

    @Test
    void constructor_maxLong_createsLongId() {
        var id = new LongId(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, id.value());
    }

    @Test
    void constructor_zero_throws() {
        assertThrows(IllegalArgumentException.class, () -> new LongId(0));
    }

    @Test
    void constructor_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> new LongId(-1));
    }

    // ——— valueOf(Object) ———

    @Test
    void valueOf_acceptsAllNumberTypes() {
        assertEquals(new LongId(1), LongId.valueOf(1L));       // Long
        assertEquals(new LongId(42), LongId.valueOf(42));       // Integer
        assertEquals(new LongId(3), LongId.valueOf(3.14));       // Double (truncated)
        assertEquals(new LongId(1), LongId.valueOf((byte) 1));  // Byte
    }

    @Test
    void valueOf_string_parsesDecimal() {
        assertEquals(new LongId(99), LongId.valueOf("99"));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> LongId.valueOf(null));
    }

    @Test
    void valueOf_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> LongId.valueOf("not-a-number"));
    }

    // ——— identifier() ———

    @Test
    void identifier_returnsBoxedLong() {
        var id = new LongId(77);
        assertEquals(Long.valueOf(77), id.identifier());
    }

    // ——— equals / hashCode (record contract) ———

    @Test
    void equal_whenSameValue() {
        assertEquals(new LongId(5), new LongId(5));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new LongId(5), new LongId(6));
    }

    @Test
    void hashCode_matchesValue() {
        assertEquals(Long.hashCode(10L), new LongId(10).hashCode());
    }

    // ——— compareTo (Identifier default) ———

    @Test
    void compareTo_lessThan() {
        assertTrue(new LongId(3).compareTo(new LongId(7)) < 0);
    }

    @Test
    void compareTo_greaterThan() {
        assertTrue(new LongId(9).compareTo(new LongId(2)) > 0);
    }

    @Test
    void compareTo_equal() {
        assertEquals(0, new LongId(4).compareTo(new LongId(4)));
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = new LongId(42).toString();
        assertTrue(s.contains("42"), "toString should expose value: " + s);
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var mapper = new ObjectMapper();
        var original = new LongId(123);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, LongId.class);
        assertEquals(original, restored);
    }

    @Test
    void jackson_serializesAsBareNumber() throws Exception {
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(new LongId(42));
        assertEquals("42", json);
    }
}
