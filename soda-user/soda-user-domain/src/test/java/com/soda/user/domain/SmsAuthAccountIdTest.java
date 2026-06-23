package com.soda.user.domain;

import com.soda.component.support.types.Mobile;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsAuthAccountIdTest {

    private static final Mobile VALID_MOBILE = new Mobile("13800138000");

    @Test void from_createsWithPrefix() {
        var id = SmsAuthAccountId.from(VALID_MOBILE);
        assertEquals("S:13800138000", id.value());
        assertEquals(VALID_MOBILE, id.mobile());
    }
    @Test void from_equivalentToValueOf() {
        assertEquals(SmsAuthAccountId.from(VALID_MOBILE), SmsAuthAccountId.of("S:13800138000"));
    }
    @Test void from_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> SmsAuthAccountId.from(null));
    }
    @Test void valueOf_string_creates() {
        assertEquals("S:13800138000", SmsAuthAccountId.of("S:13800138000").value());
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "S:", "sms:13800138000", "not-a-mobile"})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> SmsAuthAccountId.of(invalid));
    }
    @Test void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> SmsAuthAccountId.of(null));
    }
    @Test void identifier_returnsString() {
        var id = SmsAuthAccountId.from(VALID_MOBILE);
        assertEquals("S:13800138000", id.identifier());
    }
    @Test void type_returnsS() {
        assertEquals(AuthAccountType.S, SmsAuthAccountId.ACCOUNT_TYPE);
    }
    @Test void equal_whenSameValue() {
        assertEquals(
                SmsAuthAccountId.from(new Mobile("13800138000")),
                SmsAuthAccountId.from(new Mobile("13800138000")));
    }
    @Test void notEqual_whenDifferentValue() {
        assertNotEquals(
                SmsAuthAccountId.from(new Mobile("13800138000")),
                SmsAuthAccountId.from(new Mobile("13900139000")));
    }
    @Test void compareTo_delegatesToStringCompare() {
        var a = SmsAuthAccountId.from(new Mobile("13800138000"));
        var b = SmsAuthAccountId.from(new Mobile("13900139000"));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
    @Test void jackson_serializeDeserialize() throws Exception {
        var original = SmsAuthAccountId.from(VALID_MOBILE);
        var json = MAPPER.writeValueAsString(original);
        assertEquals("\"S:13800138000\"", json);
        var restored = MAPPER.readValue(json, SmsAuthAccountId.class);
        assertEquals(original, restored);
    }
}
