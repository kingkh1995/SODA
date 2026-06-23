package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobileTest {

    private static final String VALID_MOBILE = "13800138000";

    @Test
    void constructor_validMobile_creates() {
        var m = new Mobile(VALID_MOBILE);
        assertEquals(VALID_MOBILE, m.value());
    }

    @Test
    void constructor_trimmed() {
        var m = new Mobile("  " + VALID_MOBILE + "  ");
        assertEquals(VALID_MOBILE, m.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1380013800", "138001380000", "12300138000", "1380013800a", "abc", "  "})
    void constructor_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Mobile(invalid));
    }


    @Test
    void equal_whenSameValue() {
        assertEquals(new Mobile(VALID_MOBILE), new Mobile(VALID_MOBILE));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new Mobile("13800138000"), new Mobile("13900139000"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        assertTrue(new Mobile("13800000001").compareTo(new Mobile("13800000002")) < 0);
        assertTrue(new Mobile("13800000002").compareTo(new Mobile("13800000001")) > 0);
        assertEquals(0, new Mobile("13800000001").compareTo(new Mobile("13800000001")));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new Mobile(VALID_MOBILE);
        JacksonTestUtil.assertRoundTrip(original, Mobile.class);
    }
}
