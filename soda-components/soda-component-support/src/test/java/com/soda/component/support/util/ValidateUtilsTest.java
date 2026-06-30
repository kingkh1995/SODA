package com.soda.component.support.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ValidateUtils} behavior tests.
 */
class ValidateUtilsTest {

    // ——— hasText ———

    @Test
    void hasText_valid_passes() {
        // should not throw
        ValidateUtils.hasText("valid");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void hasText_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.hasText(value));
    }

    // ——— minValue ———

    @Test
    void minValue_inclusive_equal_passes() {
        ValidateUtils.minValue(5, 5, true);
    }

    @Test
    void minValue_inclusive_above_passes() {
        ValidateUtils.minValue(10, 5, true);
    }

    @Test
    void minValue_inclusive_below_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.minValue(3, 5, true));
    }

    @Test
    void minValue_exclusive_above_passes() {
        ValidateUtils.minValue(6, 5, false);
    }

    @Test
    void minValue_exclusive_equal_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.minValue(5, 5, false));
    }

    @Test
    void minValue_exclusive_below_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.minValue(4, 5, false));
    }

    // ——— notNull ———

    @Test
    void notNull_nonNull_passes() {
        ValidateUtils.notNull("value");
    }

    @Test
    void notNull_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.notNull(null));
    }

    // ——— matches ———

    @Test
    void matches_valid_passes() {
        var digit = Pattern.compile("\\d+");
        ValidateUtils.matches("123", digit);
    }

    @Test
    void matches_invalid_throws() {
        var digit = Pattern.compile("\\d+");
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.matches("abc", digit));
    }

    // ——— maxLength ———

    @Test
    void maxLength_withinLimit_passes() {
        ValidateUtils.maxLength("hello", 10);
    }

    @Test
    void maxLength_exact_passes() {
        ValidateUtils.maxLength("hello", 5);
    }

    @Test
    void maxLength_exceeds_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxLength("hello", 3));
    }

    @Test
    void maxLength_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxLength(null, 10));
    }

    // ——— range ———

    @Test
    void range_within_passes() {
        ValidateUtils.range(5, 1, 10);
    }

    @Test
    void range_minEqual_passes() {
        ValidateUtils.range(1, 1, 10);
    }

    @Test
    void range_maxEqual_passes() {
        ValidateUtils.range(10, 1, 10);
    }

    @Test
    void range_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.range(0, 1, 10));
    }

    @Test
    void range_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.range(11, 1, 10));
    }

    // ——— hasPrefix ———

    @Test
    void hasPrefix_matching_passes() {
        ValidateUtils.hasPrefix("P:42", "P:");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Q:42"})
    void hasPrefix_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.hasPrefix(value, "P:"));
    }

    // ——— maxScale ———

    @Test
    void maxScale_withinLimit_passes() {
        ValidateUtils.maxScale(new BigDecimal("10.50"), 2);
    }

    @Test
    void maxScale_exact_passes() {
        ValidateUtils.maxScale(new BigDecimal("10.55"), 2);
    }

    @Test
    void maxScale_exceeds_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxScale(new BigDecimal("10.555"), 2));
    }
}
