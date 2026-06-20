package com.soda.user.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.types.Mobile;
import org.junit.jupiter.api.Test;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SmsAuthAccountIdTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();


    private static final Mobile VALID_MOBILE = new Mobile("13800138000");

    @Test
    void from_createsWithPrefix() {
        var id = SmsAuthAccountId.from(VALID_MOBILE);
        assertEquals("S:13800138000", id.value());
        assertEquals(VALID_MOBILE, id.mobile());
    }

    @Test
    void from_equivalentToValueOf() {
        assertEquals(SmsAuthAccountId.from(VALID_MOBILE), SmsAuthAccountId.valueOf("S:13800138000"));
    }

    @Test
    void valueOf_string_creates() {
        assertEquals("S:13800138000", SmsAuthAccountId.valueOf("S:13800138000").value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "S:", "sms:13800138000", "not-a-mobile"})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> SmsAuthAccountId.valueOf(invalid));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> SmsAuthAccountId.valueOf(null));
    }

    @Test
    void identifier_returnsString() {
        var id = SmsAuthAccountId.from(VALID_MOBILE);
        assertEquals("S:13800138000", id.identifier());
    }

    @Test
    void authAccountType_returnsS() {
        assertEquals(AuthAccountType.S, SmsAuthAccountId.ACCOUNT_TYPE);
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(
                SmsAuthAccountId.from(new Mobile("13800138000")),
                SmsAuthAccountId.from(new Mobile("13800138000")));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(
                SmsAuthAccountId.from(new Mobile("13800138000")),
                SmsAuthAccountId.from(new Mobile("13900139000")));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        var a = SmsAuthAccountId.from(new Mobile("13800138000"));
        var b = SmsAuthAccountId.from(new Mobile("13900139000"));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }

    @Test
    void jackson_serializeDeserialize() {
        try {
            var original = SmsAuthAccountId.from(VALID_MOBILE);
            var json = MAPPER.writeValueAsString(original);
            assertEquals("\"S:13800138000\"", json);
            var restored = MAPPER.readValue(json, SmsAuthAccountId.class);
            assertEquals(original, restored);
        } catch (Exception e) {
            fail(e);
        }
    }
}
