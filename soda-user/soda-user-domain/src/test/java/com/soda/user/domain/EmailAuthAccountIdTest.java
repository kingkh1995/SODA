package com.soda.user.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.types.Email;
import org.junit.jupiter.api.Test;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailAuthAccountIdTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Test
    void from_createsWithPrefix() {
        var email = new Email("test@example.com");
        var id = EmailAuthAccountId.from(email);
        assertEquals("E:test@example.com", id.value());
        assertEquals(email, id.email());
    }

    @Test
    void from_equivalentToValueOf() {
        var email = new Email("test@example.com");
        assertEquals(EmailAuthAccountId.from(email), EmailAuthAccountId.valueOf("E:test@example.com"));
    }

    @Test
    void valueOf_string_creates() {
        assertEquals("E:a@b.com", EmailAuthAccountId.valueOf("E:a@b.com").value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "E:", "email:a@b.com", "not-an-email"})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> EmailAuthAccountId.valueOf(invalid));
    }

    @Test
    void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> EmailAuthAccountId.valueOf(null));
    }

    @Test
    void identifier_returnsString() {
        var id = EmailAuthAccountId.from(new Email("test@example.com"));
        assertEquals("E:test@example.com", id.identifier());
    }

    @Test
    void authAccountType_returnsE() {
        assertEquals(AuthAccountType.E, EmailAuthAccountId.ACCOUNT_TYPE);
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(
                EmailAuthAccountId.valueOf("E:a@b.com"),
                EmailAuthAccountId.valueOf("E:a@b.com"));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(
                EmailAuthAccountId.valueOf("E:a@b.com"),
                EmailAuthAccountId.valueOf("E:c@d.com"));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        var a = EmailAuthAccountId.valueOf("E:a@b.com");
        var b = EmailAuthAccountId.valueOf("E:c@d.com");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }

    @Test
    void jackson_serializeDeserialize() {
        try {
            var original = EmailAuthAccountId.from(new Email("test@example.com"));
            var json = MAPPER.writeValueAsString(original);
            assertEquals("\"E:test@example.com\"", json);
            var restored = MAPPER.readValue(json, EmailAuthAccountId.class);
            assertEquals(original, restored);
        } catch (Exception e) {
            fail(e);
        }
    }
}
