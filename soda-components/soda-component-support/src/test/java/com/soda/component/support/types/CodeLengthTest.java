package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeLengthTest {

    @Test
    void constructor_min1_creates() {
        assertEquals(1, new CodeLength(1).value());
    }

    @Test
    void constructor_max100_creates() {
        assertEquals(100, new CodeLength(100).value());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void constructor_outOfRange_throws(int invalid) {
        assertThrows(IllegalArgumentException.class, () -> new CodeLength(invalid));
    }

    @Test
    void parse_string_creates() {
        assertEquals(new CodeLength(6), CodeLength.parse("6"));
    }

    @Test
    void parse_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> CodeLength.parse(null));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new CodeLength(6), new CodeLength(6));
    }

    @Test
    void compareTo_delegatesToIntCompare() {
        assertTrue(new CodeLength(4).compareTo(new CodeLength(6)) < 0);
        assertTrue(new CodeLength(8).compareTo(new CodeLength(6)) > 0);
        assertEquals(0, new CodeLength(6).compareTo(new CodeLength(6)));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new CodeLength(6);
        JacksonTestUtil.assertRoundTrip(original, CodeLength.class);
    }
}
