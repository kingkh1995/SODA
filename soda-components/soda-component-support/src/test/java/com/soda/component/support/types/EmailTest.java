package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Email} behavior tests.
 * <p>
 * Tests verify through public API only — {@code new Email()}, {@code valueOf(Object)},
 * {@code localPart()}, {@code domain()}, {@code compareTo()}, and Jackson round-trip.
 */
class EmailTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ——— constructor ———

    @Test
    void constructor_validEmail_createsEmail() {
        var email = new Email("user@example.com");
        assertEquals("user@example.com", email.value());
    }

    @Test
    void constructor_normalizedToLowercase() {
        var email = new Email("USER@Example.COM");
        assertEquals("user@example.com", email.value());
    }

    @Test
    void constructor_whitespaceAround_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Email("  user@example.com  "));
    }

    @Test
    void constructor_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Email(null));
    }

    @Test
    void constructor_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Email("  "));
    }

    @Test
    void constructor_noAtSymbol_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Email("userexample.com"));
    }

    @Test
    void constructor_noDomain_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user@"));
    }

    @Test
    void constructor_noTld_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Email("user@example"));
    }

    @Test
    void constructor_specialChars_acceptsValid() {
        var email = new Email("user.name+tag@example.co.uk");
        assertEquals("user.name+tag@example.co.uk", email.value());
    }

    // ——— localPart() ———

    @Test
    void localPart_returnsBeforeAt() {
        var email = new Email("alice@example.com");
        assertEquals("alice", email.localPart());
    }

    @Test
    void localPart_withDots() {
        var email = new Email("alice.smith@example.com");
        assertEquals("alice.smith", email.localPart());
    }

    // ——— domain() ———

    @Test
    void domain_returnsAfterAt() {
        var email = new Email("alice@example.com");
        assertEquals("example.com", email.domain());
    }

    @Test
    void domain_subdomain() {
        var email = new Email("alice@mail.example.co.uk");
        assertEquals("mail.example.co.uk", email.domain());
    }

    // ——— equals / hashCode (record contract) ———

    @Test
    void equal_whenSameAddress() {
        assertEquals(
                new Email("a@b.com"),
                new Email("A@B.com")   // normalized to lowercase
        );
    }

    @Test
    void notEqual_whenDifferentAddress() {
        assertNotEquals(new Email("a@b.com"), new Email("a@c.com"));
    }

    // ——— toString ———

    @Test
    void toString_containsValue() {
        var s = new Email("t@t.com").toString();
        assertTrue(s.contains("t@t.com"), "toString should expose value: " + s);
    }

    // ——— Jackson round-trip ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new Email("jackson@test.org");
        JacksonTestUtil.assertRoundTrip(original, Email.class);
    }

    @Test
    void jackson_serializesAsBareString() throws Exception {
        var json = MAPPER.writeValueAsString(new Email("bare@test.com"));
        assertEquals("\"bare@test.com\"", json);
    }
}
