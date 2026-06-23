package com.soda.component.support.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiveTest {

    @Test
    void parse_true_returnsTrue() {
        assertSame(Active.TRUE, Active.parse("true"));
    }

    @Test
    void parse_false_returnsFalse() {
        assertSame(Active.FALSE, Active.parse("false"));
    }

    @Test
    void parse_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Active.parse(null));
    }

    @Test
    void parse_invalidString_throws() {
        assertThrows(IllegalArgumentException.class, () -> Active.parse("not-a-boolean"));
    }
}
