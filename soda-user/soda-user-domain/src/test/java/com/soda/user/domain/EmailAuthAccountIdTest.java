package com.soda.user.domain;

import com.soda.component.support.types.Email;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailAuthAccountIdTest {

    @Test void from_createsWithPrefix() {
        var email = new Email("test@example.com");
        var id = EmailAuthAccountId.from(email);
        assertEquals("E:test@example.com", id.value());
        assertEquals(email, id.email());
    }
    @Test void from_equivalentToValueOf() {
        var email = new Email("test@example.com");
        assertEquals(EmailAuthAccountId.from(email), EmailAuthAccountId.of("E:test@example.com"));
    }
    @Test void from_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> EmailAuthAccountId.from(null));
    }
    @Test void valueOf_string_creates() {
        assertEquals("E:a@b.com", EmailAuthAccountId.of("E:a@b.com").value());
    }
    @ParameterizedTest
    @ValueSource(strings = {"", "E:", "email:a@b.com", "not-an-email"})
    void valueOf_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> EmailAuthAccountId.of(invalid));
    }
    @Test void valueOf_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> EmailAuthAccountId.of(null));
    }
    @Test void identifier_returnsString() {
        var id = EmailAuthAccountId.from(new Email("test@example.com"));
        assertEquals("E:test@example.com", id.identifier());
    }
    @Test void type_returnsE() {
        assertEquals(AuthAccountType.E, EmailAuthAccountId.ACCOUNT_TYPE);
    }
    @Test void equal_whenSameValue() {
        assertEquals(
                EmailAuthAccountId.of("E:a@b.com"),
                EmailAuthAccountId.of("E:a@b.com"));
    }
    @Test void notEqual_whenDifferentValue() {
        assertNotEquals(
                EmailAuthAccountId.of("E:a@b.com"),
                EmailAuthAccountId.of("E:c@d.com"));
    }
    @Test void compareTo_delegatesToStringCompare() {
        var a = EmailAuthAccountId.of("E:a@b.com");
        var b = EmailAuthAccountId.of("E:c@d.com");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(a));
    }
    @Test void jackson_serializeDeserialize() throws Exception {
        var original = EmailAuthAccountId.from(new Email("test@example.com"));
        var json = MAPPER.writeValueAsString(original);
        assertEquals("\"E:test@example.com\"", json);
        var restored = MAPPER.readValue(json, EmailAuthAccountId.class);
        assertEquals(original, restored);
    }
}
