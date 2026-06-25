package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RandomStringTest {

    @Test
    void constructor_valid_creates() {
        var rs = new RandomString("AbCd123");
        assertEquals("AbCd123", rs.value());
    }

    @Test
    void constructor_anyNonBlank_accepts() {
        var rs = new RandomString("任何非空白字符串_123!");
        assertEquals("任何非空白字符串_123!", rs.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_nullOrEmpty_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new RandomString(invalid));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new RandomString("abc"), new RandomString("abc"));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new RandomString("Abc123");
        JacksonTestUtil.assertRoundTrip(original, RandomString.class);
    }
}
