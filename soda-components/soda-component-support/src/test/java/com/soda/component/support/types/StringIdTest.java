package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link StringId} behavior tests.
 * <p>
 * Tests verify through public API only — {@code new StringId()}, {@code valueOf(Object)}, {@code identifier()},
 * Jackson round-trip, and contract guarantees.
 */
class StringIdTest {

    // ——— constructor ———

    @Test
    void constructor_validString_createsStringId() {
        var id = new StringId("abc");
        assertEquals("abc", id.value());
    }

    @Test
    void constructor_trimmed_removesWhitespace() {
        var id = new StringId("  hello  ");
        assertEquals("hello", id.value());
    }

    @Test
    void constructor_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> new StringId(null));
    }

    @Test
    void constructor_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> new StringId("  "));
    }

    @Test
    void constructor_emptyString_throws() {
        assertThrows(IllegalArgumentException.class, () -> new StringId(""));
    }

    // ——— valueOf(Object) ———

    @Test
    void valueOf_string_createsStringId() {
        assertEquals(new StringId("xyz"), StringId.valueOf("xyz"));
    }

    @Test
    void valueOf_nonString_convertsViaToString() {
        assertEquals(new StringId("42"), StringId.valueOf(42));
        assertEquals(new StringId("true"), StringId.valueOf(true));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> StringId.valueOf(null));
    }

    // ——— identifier() ———

    @Test
    void identifier_returnsString() {
        var id = new StringId("test-id");
        assertEquals("test-id", id.identifier());
    }

    // ——— equals / hashCode (record contract) ———

    @Test
    void equal_whenSameValue() {
        assertEquals(new StringId("x"), new StringId("x"));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new StringId("a"), new StringId("b"));
    }

    @Test
    void hashCode_matchesValue() {
        assertEquals("key".hashCode(), new StringId("key").hashCode());
    }

    // ——— compareTo ———

    @Test
    void compareTo_lessThan() {
        assertTrue(new StringId("a").compareTo(new StringId("b")) < 0);
    }

    @Test
    void compareTo_greaterThan() {
        assertTrue(new StringId("z").compareTo(new StringId("m")) > 0);
    }

    @Test
    void compareTo_equal() {
        assertEquals(0, new StringId("mid").compareTo(new StringId("mid")));
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = new StringId("val").toString();
        assertTrue(s.contains("val"), "toString should expose value: " + s);
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var mapper = new ObjectMapper();
        var original = new StringId("jackson-test");
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, StringId.class);
        assertEquals(original, restored);
    }

    @Test
    void jackson_serializesAsBareString() throws Exception {
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(new StringId("bare"));
        assertEquals("\"bare\"", json);
    }
}
