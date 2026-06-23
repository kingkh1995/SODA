package com.soda.user.domain;

import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.SocialType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialAuthAccountIdTest {


    @Test
    void from_createsWithPrefix() {
        var id = SocialAuthAccountId.from(SocialType.GE, "open123");
        assertEquals("O:GE:open123", id.value());
        assertEquals(SocialType.GE, id.socialType());
        assertEquals("open123", id.openId());
    }

    @Test
    void from_equivalentToValueOf() {
        assertEquals(
                SocialAuthAccountId.from(SocialType.GE, "open123"),
                SocialAuthAccountId.of("O:GE:open123"));
    }

    @Test
    void valueOf_string_creates() {
        var id = SocialAuthAccountId.of("O:GE:1");
        assertEquals("O:GE:1", id.value());
        assertEquals(SocialType.GE, id.socialType());
        assertEquals("1", id.openId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "O:", "O:GE", "O:GE:", "social:GE:1", "O:UNKNOWN:1"})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> SocialAuthAccountId.of(invalid));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> SocialAuthAccountId.of(null));
    }

    @Test
    void identifier_returnsString() {
        var id = SocialAuthAccountId.from(SocialType.GE, "open123");
        assertEquals("O:GE:open123", id.identifier());
    }

    @Test
    void type_returnsO() {
        assertEquals(AuthAccountType.O, SocialAuthAccountId.ACCOUNT_TYPE);
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(
                SocialAuthAccountId.from(SocialType.GE, "1"),
                SocialAuthAccountId.from(SocialType.GE, "1"));
    }

    @Test
    void notEqual_whenDifferentSocialType() {
        assertNotEquals(
                SocialAuthAccountId.from(SocialType.GE, "1"),
                SocialAuthAccountId.from(SocialType.DT, "1"));
    }

    @Test
    void notEqual_whenDifferentOpenId() {
        assertNotEquals(
                SocialAuthAccountId.from(SocialType.GE, "1"),
                SocialAuthAccountId.from(SocialType.GE, "2"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        var a = SocialAuthAccountId.from(SocialType.GE, "1");
        var b = SocialAuthAccountId.from(SocialType.GE, "2");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = SocialAuthAccountId.from(SocialType.GE, "open123");
        var json = MAPPER.writeValueAsString(original);
        assertEquals("\"O:GE:open123\"", json);
        var restored = MAPPER.readValue(json, SocialAuthAccountId.class);
        assertEquals(original, restored);
    }

    @Test
    void openId_allSocialTypes() {
        for (var type : SocialType.values()) {
            var id = SocialAuthAccountId.from(type, "testOpenId");
            assertEquals(type, id.socialType());
            assertEquals("testOpenId", id.openId());
        }
    }
}
