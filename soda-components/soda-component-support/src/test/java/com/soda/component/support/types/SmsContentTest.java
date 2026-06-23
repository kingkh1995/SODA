package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsContentTest {

    @Test
    void constructor_validContent_creates() {
        var content = new SmsContent("您的验证码是123456");
        assertEquals("您的验证码是123456", content.value());
    }

    @Test
    void constructor_maxLength70_creates() {
        var content = new SmsContent("a".repeat(70));
        assertEquals(70, content.value().length());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_nullOrEmpty_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new SmsContent(invalid));
    }

    @Test
    void constructor_tooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SmsContent("a".repeat(71)));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new SmsContent("hello"), new SmsContent("hello"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        assertTrue(new SmsContent("a").compareTo(new SmsContent("b")) < 0);
        assertEquals(0, new SmsContent("a").compareTo(new SmsContent("a")));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new SmsContent("hello");
        JacksonTestUtil.assertRoundTrip(original, SmsContent.class);
    }
}
