package com.soda.component.support.types;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.soda.component.support.testutil.JacksonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

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
    void valueOf_int_creates() {
        assertEquals(new CodeLength(6), CodeLength.valueOf(6));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> CodeLength.valueOf(null));
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
