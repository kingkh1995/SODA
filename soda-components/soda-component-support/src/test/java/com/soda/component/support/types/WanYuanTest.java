package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WanYuan} behavior tests.
 * <p>
 * Tests verify through public API only — constructor, {@code fromYuan()},
 * {@code toYuan()}, {@code compareTo()}, and Jackson round-trip.
 */
class WanYuanTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ——— constructor (primary entry) ———

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

    // ——— parse(String) ———

    @Test
    void parse_validString_createsWanYuan() {
        var amount = WanYuan.parse("1.5");
        assertEquals(0, new BigDecimal("1.5").compareTo(amount.value()));
    }

    @Test
    void parse_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.parse(null));
    }

    @Test
    void parse_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.parse("not-a-number"));
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
        var original = new WanYuan(new BigDecimal("1.5"));
        JacksonTestUtil.assertRoundTrip(original, WanYuan.class);
    }

    @Test
    void jackson_serializesAsBareNumber() throws Exception {
        var json = MAPPER.writeValueAsString(new WanYuan(new BigDecimal("1.5")));
        assertEquals("1.5", json);
    }
}
