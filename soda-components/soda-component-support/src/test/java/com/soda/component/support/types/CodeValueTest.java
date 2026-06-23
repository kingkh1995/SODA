package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeValueTest {

    @Test
    void constructor_valid_creates() {
        var cv = new CodeValue("A1b2C3");
        assertEquals("A1b2C3", cv.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc def", "abc-def", "abc_def", "", "  "})
    void constructor_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new CodeValue(invalid));
    }


    @Test
    void equal_whenSameValue() {
        assertEquals(new CodeValue("abc"), new CodeValue("abc"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        assertTrue(new CodeValue("a").compareTo(new CodeValue("b")) < 0);
        assertEquals(0, new CodeValue("x").compareTo(new CodeValue("x")));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new CodeValue("Abc123");
        JacksonTestUtil.assertRoundTrip(original, CodeValue.class);
    }
}
