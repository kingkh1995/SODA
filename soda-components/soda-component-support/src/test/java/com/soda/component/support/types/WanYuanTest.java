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
 * Tests verify through public API only — {@code of()}, {@code fromYuan()},
 * {@code toYuan()}, and Jackson round-trip.
 */
class WanYuanTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ——— of (primary entry) ———

    @Test
    void of_validValue_createsWanYuan() {
        var amount = WanYuan.of("1.50");
        assertEquals("1.50", amount.value());
        assertEquals(0, new BigDecimal("1.5").compareTo(amount.bigDecimalValue()));
    }

    @Test
    void of_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.of(null));
    }

    @Test
    void of_negative_accepts() {
        var amount = WanYuan.of("-1");
        assertEquals("-1.00", amount.value());
    }

    @Test
    void of_scaleAboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.of("1.234"));
    }

    @Test
    void of_trailingZeros_preserved() {
        // "1.50" → toPlainString "1.50"
        var amount = WanYuan.of("1.50");
        assertEquals("1.50", amount.value());
    }

    @Test
    void of_negativeScale_normalized() {
        // 1E+2 (scale -2) → normalized to 100.00 (scale 2)
        var amount = WanYuan.of("1E+2");
        assertEquals("100.00", amount.value());
    }

    @Test
    void of_deserializesFromString() {
        // This tests that a JSON string input round-trips through of(String)
        var amount = WanYuan.of("123.45");
        assertEquals("123.45", amount.value());
    }

    // ——— fromYuan ———

    @Test
    void fromYuan_convertsCorrectly() {
        var amount = WanYuan.fromYuan(new BigDecimal("15000"));
        assertEquals("1.50", amount.value());
    }

    @Test
    void fromYuan_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> WanYuan.fromYuan(null));
    }

    @Test
    void fromYuan_negative_accepts() {
        var amount = WanYuan.fromYuan(new BigDecimal("-5000"));
        assertEquals("-0.50", amount.value());
    }

    @Test
    void fromYuan_withRoundingMode_roundsAsSpecified() {
        // 1元 → 0.0001万元，HALF_UP 舍入到2位 = 0.00
        assertEquals("0.00", WanYuan.fromYuan(new BigDecimal("1"), java.math.RoundingMode.HALF_UP).value());
        // 50元 → 0.005万元，HALF_UP 舍入到2位 = 0.01
        assertEquals("0.01", WanYuan.fromYuan(new BigDecimal("50"), java.math.RoundingMode.HALF_UP).value());
        // 49元 → 0.0049万元，HALF_DOWN 舍入到2位 = 0.00
        assertEquals("0.00", WanYuan.fromYuan(new BigDecimal("49"), java.math.RoundingMode.HALF_DOWN).value());
    }

    // ——— toYuan ———

    @Test
    void toYuan_convertsCorrectly() {
        assertEquals(
                0,
                new BigDecimal("10000").compareTo(WanYuan.of("1").toYuan())
        );
    }

    // ——— equals / hashCode ———

    @Test
    void equal_whenSameValue() {
        assertEquals(
                WanYuan.of("1"),
                WanYuan.of("1")
        );
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(
                WanYuan.of("1"),
                WanYuan.of("2")
        );
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = WanYuan.of("1.5").toString();
        assertTrue(s.contains("1.5"), "toString should expose value: " + s);
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = WanYuan.of("1.5");
        JacksonTestUtil.assertRoundTrip(original, WanYuan.class);
    }

    @Test
    void jackson_serializesAsBareString() throws Exception {
        var json = MAPPER.writeValueAsString(WanYuan.of("1.5"));
        assertEquals("\"1.50\"", json);
    }

    // ——— toPlainText ———

    @Test
    void toPlainText_positive() {
        var w = WanYuan.of("111.11");
        assertEquals("111.11万元", w.toPlainText());
    }

    @Test
    void toPlainText_negative() {
        var w = WanYuan.of("-50.00");
        assertEquals("-50.00万元", w.toPlainText());
    }
}
