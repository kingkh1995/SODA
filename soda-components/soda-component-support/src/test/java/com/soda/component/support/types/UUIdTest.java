package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link UUId} behavior tests.
 * <p>
 * Tests verify through public API only — {@code new UUId()}, {@code valueOf(Object)}, {@code random()},
 * Jackson round-trip, and contract guarantees (immutable, comparable, serializable).
 */
class UUIdTest {

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    // ——— constructor ———

    @Test
    void constructor_validUuid_createsId() {
        var id = new UUId(VALID_UUID);
        assertEquals(VALID_UUID, id.value());
    }

    @Test
    void constructor_uppercase_normalizesToLowercase() {
        var id = new UUId("550E8400-E29B-41D4-A716-446655440000");
        assertEquals(VALID_UUID, id.value());
    }

    @Test
    void constructor_mixedCase_normalizesToLowercase() {
        var id = new UUId("550e8400-e29b-41D4-A716-446655440000");
        assertEquals(VALID_UUID, id.value());
    }

    @Test
    void constructor_trimmed() {
        var id = new UUId("  " + VALID_UUID + "  ");
        assertEquals(VALID_UUID, id.value());
    }

    @Test
    void constructor_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> new UUId(null));
    }

    @Test
    void constructor_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> new UUId("  "));
    }

    @Test
    void constructor_noHyphens_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new UUId("550e8400e29b41d4a716446655440000"));
    }

    @Test
    void constructor_wrongSectionLength_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new UUId("550e8400-e29b-41d4-a716-44665544000"));  // last section 11 chars
    }

    @Test
    void constructor_invalidHex_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new UUId("550e8400-e29b-41d4-a716-44665544000g"));  // 'g' not hex
    }

    // ——— valueOf(Object) ———

    @Test
    void valueOf_string_createsId() {
        assertEquals(new UUId(VALID_UUID), UUId.valueOf(VALID_UUID));
    }

    @Test
    void valueOf_javaUtilUuid_parsesByToString() {
        var juid = java.util.UUID.randomUUID();
        assertEquals(juid.toString(), UUId.valueOf(juid).value());
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> UUId.valueOf(null));
    }

    @Test
    void valueOf_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> UUId.valueOf("not-a-uuid"));
    }

    // ——— random() ———

    @Test
    void random_generatesValidInstance() {
        var id = UUId.random();
        assertNotNull(id);
        // re-constructing from its value should succeed (format is valid)
        assertDoesNotThrow(() -> new UUId(id.value()));
    }

    @Test
    void random_generatesDistinctValues() {
        assertNotEquals(UUId.random(), UUId.random());
    }

    // ——— identifier() ———

    @Test
    void identifier_returnsString() {
        var id = new UUId(VALID_UUID);
        assertEquals(VALID_UUID, id.identifier());
    }

    // ——— equals / hashCode (record contract) ———

    @Test
    void equal_whenSameValue() {
        assertEquals(new UUId(VALID_UUID), new UUId(VALID_UUID));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new UUId(VALID_UUID), UUId.random());
    }

    // ——— compareTo (Identifier default) ———

    @Test
    void compareTo_delegatesToStringCompare() {
        var a = new UUId("00000000-0000-0000-0000-000000000001");
        var b = new UUId("00000000-0000-0000-0000-000000000002");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = new UUId(VALID_UUID).toString();
        assertTrue(s.contains(VALID_UUID), "toString should expose value: " + s);
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var mapper = new ObjectMapper();
        var original = new UUId(VALID_UUID);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, UUId.class);
        assertEquals(original, restored);
    }

    @Test
    void jackson_serializesAsBareString() throws Exception {
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(new UUId(VALID_UUID));
        assertEquals('"' + VALID_UUID + '"', json);
    }
}
