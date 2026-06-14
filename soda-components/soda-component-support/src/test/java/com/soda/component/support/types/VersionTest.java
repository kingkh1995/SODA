package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Version} behavior tests.
 * <p>
 * Tests verify through public API only — {@code of()}, {@code valueOf(Object)},
 * cache reuse, {@code next()}, {@code compareTo()}, and Jackson round-trip.
 */
class VersionTest {

    // ——— of(int) ———

    @Test
    void of_5_createsVersionWithValue5() {
        var v = Version.of(5);
        assertEquals(5, v.value());
    }

    @Test
    void of_zero_createsPrimary() {
        assertSame(Version.PRIMARY, Version.of(0));
    }

    @Test
    void of_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> Version.of(-1));
    }

    // ——— valueOf(Object) ———

    @Test
    void valueOf_int_createsVersion() {
        var v = Version.valueOf(3);
        assertEquals(3, v.value());
    }

    @Test
    void valueOf_string_parsesInt() {
        assertEquals(Version.of(7), Version.valueOf("7"));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf(null));
    }

    @Test
    void valueOf_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> Version.valueOf("not-a-number"));
    }

    // ——— cache ———

    @Test
    void cache_sameInstance_withinRange() {
        assertSame(Version.of(5), Version.of(5));
    }

    @Test
    void cache_sameInstance_atUpperBound() {
        assertSame(Version.of(99), Version.of(99));
    }

    @Test
    void cache_differentInstance_beyondRange() {
        // 缓存范围由 SPI 控制，至少 [0, 99]。使用远大于最小范围的值保证超出缓存。
        var vLargeA = Version.of(10000);
        var vLargeB = Version.of(10000);
        assertNotSame(vLargeA, vLargeB);
    }

    // ——— next() ———

    @Test
    void next_incrementsValue() {
        assertEquals(Version.of(6), Version.of(5).next());
    }

    @Test
    void next_fromPrimary() {
        assertEquals(Version.of(1), Version.PRIMARY.next());
    }

    @Test
    void next_fromCachedBoundary() {
        assertEquals(Version.of(100), Version.of(99).next());
    }

    @Test
    void next_doesNotMutateOriginal() {
        var v = Version.of(5);
        v.next();
        assertEquals(5, v.value(), "original must be unchanged");
    }

    // ——— equals / hashCode ———

    @Test
    void equal_whenSameValue() {
        assertEquals(Version.of(3), Version.of(3));
    }

    @Test
    void equal_whenSameInstance() {
        var v = Version.of(5);
        assertEquals(v, v);
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(Version.of(1), Version.of(2));
    }

    @Test
    void notEqual_withDifferentType() {
        assertNotEquals(Version.of(1), "1");
    }

    // ——— compareTo ———

    @Test
    void compareTo_byValue() {
        assertTrue(Version.of(1).compareTo(Version.of(2)) < 0);
        assertTrue(Version.of(5).compareTo(Version.of(3)) > 0);
        assertEquals(0, Version.of(4).compareTo(Version.of(4)));
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = Version.of(42).toString();
        assertTrue(s.contains("42"), "toString should expose value: " + s);
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var mapper = new ObjectMapper();
        var original = Version.of(42);
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, Version.class);
        assertEquals(original, restored);
    }

    @Test
    void jackson_serializesAsBareNumber() throws Exception {
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(Version.of(7));
        assertEquals("7", json);
    }

    @Test
    void jackson_deserializeReturnsCachedInstance() throws Exception {
        var mapper = new ObjectMapper();
        var restored = mapper.readValue("5", Version.class);
        assertSame(Version.of(5), restored);
    }

    // ——— Serializable ———

    @Test
    void serializable() throws Exception {
        var original = Version.of(42);
        var baos = new java.io.ByteArrayOutputStream();
        try (var oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }
        try (var ois = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(baos.toByteArray()))) {
            var restored = (Version) ois.readObject();
            assertEquals(original, restored);
        }
    }
}
