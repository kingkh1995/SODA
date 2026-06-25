package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositiveIntTest {

    @Test
    void of_min1_creates() {
        assertEquals(1, PositiveInt.of(1).value());
    }

    @Test
    void of_large_creates() {
        assertEquals(1000, PositiveInt.of(1000).value());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void of_nonPositive_throws(int invalid) {
        assertThrows(IllegalArgumentException.class, () -> PositiveInt.of(invalid));
    }

    @Test
    void parse_string_creates() {
        assertEquals(PositiveInt.of(6), PositiveInt.parse("6"));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(PositiveInt.of(6), PositiveInt.of(6));
    }

    @Test
    void compareTo_delegatesToIntCompare() {
        assertTrue(PositiveInt.of(4).compareTo(PositiveInt.of(6)) < 0);
        assertEquals(0, PositiveInt.of(6).compareTo(PositiveInt.of(6)));
        assertTrue(PositiveInt.of(8).compareTo(PositiveInt.of(6)) > 0);
    }

    // ——— cache ———

    @Test
    void cache_sameInstance_withinRange() {
        assertSame(PositiveInt.of(5), PositiveInt.of(5));
    }

    @Test
    void cache_sameInstance_atUpperBound() {
        assertSame(PositiveInt.of(100), PositiveInt.of(100));
    }

    @Test
    void cache_differentInstance_beyondRange() {
        assertNotSame(PositiveInt.of(1000), PositiveInt.of(1000));
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = PositiveInt.of(42).toString();
        assertEquals("PositiveInt[value=42]", s);
    }

    // ——— Jackson ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = PositiveInt.of(6);
        JacksonTestUtil.assertRoundTrip(original, PositiveInt.class);
    }
}
