package com.soda.component.support.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ValidateUtils} behavior tests.
 */
class ValidateUtilsTest {

    // ——— nonBlank ———

    @Test
    void nonBlank_valid_passes() {
        // should not throw
        ValidateUtils.nonBlank("valid");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void nonBlank_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.nonBlank(value));
    }

    // ——— minValue ———

    @Test
    void minValue_inclusive_equal_passes() {
        ValidateUtils.minValue(5, true, 5);
    }

    @Test
    void minValue_inclusive_above_passes() {
        ValidateUtils.minValue(5, true, 10);
    }

    @Test
    void minValue_inclusive_below_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.minValue(5, true, 3));
    }

    @Test
    void minValue_exclusive_above_passes() {
        ValidateUtils.minValue(5, false, 6);
    }

    @Test
    void minValue_exclusive_equal_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.minValue(5, false, 5));
    }

    @Test
    void minValue_exclusive_below_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.minValue(5, false, 4));
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
        ValidateUtils.matches(digit, "123");
    }

    @Test
    void matches_invalid_throws() {
        var digit = Pattern.compile("\\d+");
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.matches(digit, "abc"));
    }

    // ——— maxLength ———

    @Test
    void maxLength_withinLimit_passes() {
        ValidateUtils.maxLength(10, "hello");
    }

    @Test
    void maxLength_exact_passes() {
        ValidateUtils.maxLength(5, "hello");
    }

    @Test
    void maxLength_exceeds_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxLength(3, "hello"));
    }

    @Test
    void maxLength_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxLength(10, null));
    }

    // ——— maxValue ———

    @Test
    void maxValue_inclusive_equal_passes() {
        ValidateUtils.maxValue(10, true, 10);
    }

    @Test
    void maxValue_inclusive_below_passes() {
        ValidateUtils.maxValue(10, true, 5);
    }

    @Test
    void maxValue_inclusive_above_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxValue(10, true, 15));
    }

    @Test
    void maxValue_exclusive_below_passes() {
        ValidateUtils.maxValue(10, false, 9);
    }

    @Test
    void maxValue_exclusive_equal_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxValue(10, false, 10));
    }

    @Test
    void maxValue_exclusive_above_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxValue(10, false, 11));
    }

    // ——— range ———

    @Test
    void range_within_passes() {
        ValidateUtils.range(1, 10, 5);
    }

    @Test
    void range_minEqual_passes() {
        ValidateUtils.range(1, 10, 1);
    }

    @Test
    void range_maxEqual_passes() {
        ValidateUtils.range(1, 10, 10);
    }

    @Test
    void range_belowMin_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.range(1, 10, 0));
    }

    @Test
    void range_aboveMax_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.range(1, 10, 11));
    }

    // ——— validUri ———

    @Test
    void validUri_http_passes() {
        ValidateUtils.validUri("http://example.com");
    }

    @Test
    void validUri_https_passes() {
        ValidateUtils.validUri("https://example.com/path?q=1");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-uri"})
    void validUri_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.validUri(value));
    }

    // ——— hasPrefix ———

    @Test
    void hasPrefix_matching_passes() {
        ValidateUtils.hasPrefix("P:", "P:42");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Q:42"})
    void hasPrefix_invalid_throws(String value) {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.hasPrefix("P:", value));
    }

    // ——— maxScale ———

    @Test
    void maxScale_withinLimit_passes() {
        ValidateUtils.maxScale(2, new BigDecimal("10.50"));
    }

    @Test
    void maxScale_exact_passes() {
        ValidateUtils.maxScale(2, new BigDecimal("10.55"));
    }

    @Test
    void maxScale_exceeds_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.maxScale(2, new BigDecimal("10.555")));
    }
}
