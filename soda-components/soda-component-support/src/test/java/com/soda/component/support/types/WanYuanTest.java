package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link WanYuan} behavior tests.
 * <p>
 * Tests verify through public API only — constructor, {@code valueOf(Object)},
 * {@code fromYuan()}, {@code toYuan()}, {@code compareTo()}, and Jackson round-trip.
 */
class WanYuanTest {

    // ——— constructor ———

    @Test
    void constructor_validValue_createsWanYuan() {
        var amount = new WanYuan(new BigDecimal("1.50"));
        assertEquals(0, new BigDecimal("1.5").compareTo(amount.value()));
    }

    @Test
    void constructor_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> new WanYuan(null));
    }

    @Test
    void constructor_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> new WanYuan(new BigDecimal("-1")));
    }

    @Test
    void constructor_scaleExceedsMax_throws() {
        assertThrows(IllegalArgumentException.class, () -> new WanYuan(new BigDecimal("1.234")));
    }

    @Test
    void constructor_zero_isValid() {
        var amount = new WanYuan(BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(amount.value()));
    }

    // ——— valueOf(Object) ———

    @Test
    void valueOf_string_parsesWanYuan() {
        assertEquals(
                new WanYuan(new BigDecimal("1.5")),
                WanYuan.valueOf("1.5")
        );
    }

    @Test
    void valueOf_bigDecimal_passesThrough() {
        assertEquals(
                new WanYuan(new BigDecimal("2")),
                WanYuan.valueOf(new BigDecimal("2"))
        );
    }

    @Test
    void valueOf_integer_converts() {
        assertEquals(
                new WanYuan(new BigDecimal("3")),
                WanYuan.valueOf(3)
        );
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.valueOf(null));
    }

    @Test
    void valueOf_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.valueOf("not-a-number"));
    }

    // ——— fromYuan / toYuan ———

    @Test
    void fromYuan_15000yuan_is1dot5WanYuan() {
        var amount = WanYuan.fromYuan(new BigDecimal("15000"));
        assertEquals(0, new BigDecimal("1.5").compareTo(amount.value()));
    }

    @Test
    void fromYuan_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.fromYuan(null));
    }

    @Test
    void fromYuan_negative_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.fromYuan(new BigDecimal("-1")));
    }

    @Test
    void toYuan_1dot5WanYuan_is15000yuan() {
        var amount = new WanYuan(new BigDecimal("1.5"));
        assertEquals(0, new BigDecimal("15000").compareTo(amount.toYuan()));
    }

    @Test
    void toYuan_zero() {
        var amount = new WanYuan(BigDecimal.ZERO);
        assertEquals(0, BigDecimal.ZERO.compareTo(amount.toYuan()));
    }

    @Test
    void fromYuan_toYuan_roundtrip() {
        var original = new BigDecimal("12300");
        assertEquals(
                0,
                WanYuan.fromYuan(original).toYuan().compareTo(original)
        );
    }

    // ——— equals / hashCode (record contract) ———

    @Test
    void equal_whenSameValue() {
        assertEquals(
                new WanYuan(new BigDecimal("1.5")),
                new WanYuan(new BigDecimal("1.50"))
        );
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(
                new WanYuan(new BigDecimal("1")),
                new WanYuan(new BigDecimal("2"))
        );
    }

    // ——— compareTo ———

    @Test
    void compareTo_byValue() {
        assertTrue(new WanYuan(new BigDecimal("1")).compareTo(new WanYuan(new BigDecimal("2"))) < 0);
        assertTrue(new WanYuan(new BigDecimal("5")).compareTo(new WanYuan(new BigDecimal("3"))) > 0);
        assertEquals(0, new WanYuan(new BigDecimal("4")).compareTo(new WanYuan(new BigDecimal("4"))));
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = new WanYuan(new BigDecimal("1.5")).toString();
        assertTrue(s.contains("1.5"), "toString should expose value: " + s);
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var mapper = new ObjectMapper();
        var original = new WanYuan(new BigDecimal("1.5"));
        var json = mapper.writeValueAsString(original);
        var restored = mapper.readValue(json, WanYuan.class);
        assertEquals(original, restored);
    }

    @Test
    void jackson_serializesAsBareNumber() throws Exception {
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(new WanYuan(new BigDecimal("1.5")));
        assertEquals("1.5", json);
    }
}
