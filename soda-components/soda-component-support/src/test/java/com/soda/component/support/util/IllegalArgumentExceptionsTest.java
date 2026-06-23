package com.soda.component.support.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link IllegalArgumentExceptions} behavior tests.
 */
class IllegalArgumentExceptionsTest {

    @Test
    void forIsNull_message() {
        var ex = IllegalArgumentExceptions.forIsNull();
        assertEquals("must not be null", ex.getMessage());
    }

    @Test
    void forIsBlank_message() {
        var ex = IllegalArgumentExceptions.forIsBlank();
        assertEquals("must not be blank", ex.getMessage());
    }

    @Test
    void forWrongType_message() {
        var ex = IllegalArgumentExceptions.forWrongType("Number", String.class);
        assertEquals("expected Number but got: java.lang.String", ex.getMessage());
    }

    @Test
    void forWrongFormat_message() {
        var ex = IllegalArgumentExceptions.forWrongFormat("abc");
        assertEquals("invalid number format: 'abc'", ex.getMessage());
    }

    @Test
    void forMinValue_exclusive_message() {
        var ex = IllegalArgumentExceptions.forMinValue(1, 5, false);
        assertEquals("must be greater than 5, got: 1", ex.getMessage());
    }

    @Test
    void forMinValue_inclusive_message() {
        var ex = IllegalArgumentExceptions.forMinValue(4, 5, true);
        assertEquals("must be greater than or equal to 5, got: 4", ex.getMessage());
    }
}
