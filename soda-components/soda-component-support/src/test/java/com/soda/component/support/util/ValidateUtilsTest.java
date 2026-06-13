package com.soda.component.support.util;

import org.junit.jupiter.api.Test;

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

    @Test
    void nonBlank_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.nonBlank(null));
    }

    @Test
    void nonBlank_empty_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.nonBlank(""));
    }

    @Test
    void nonBlank_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.nonBlank("  "));
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
        var digit = java.util.regex.Pattern.compile("\\d+");
        ValidateUtils.matches(digit, "123");
    }

    @Test
    void matches_invalid_throws() {
        var digit = java.util.regex.Pattern.compile("\\d+");
        assertThrows(IllegalArgumentException.class, () -> ValidateUtils.matches(digit, "abc"));
    }
}

