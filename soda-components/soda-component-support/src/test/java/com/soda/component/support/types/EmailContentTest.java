package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import com.soda.component.support.testutil.JacksonTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class EmailContentTest {

    @Test
    void constructor_valid_creates() {
        var c = new EmailContent("Welcome", "Thank you for registering");
        assertEquals("Welcome", c.subject());
        assertEquals("Thank you for registering", c.body());
    }

    @Test
    void constructor_subjectMaxLength255_creates() {
        var subj = "a".repeat(255);
        var c = new EmailContent(subj, "body");
        assertEquals(255, c.subject().length());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_nullOrEmptySubject_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new EmailContent(invalid, "body"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_nullOrEmptyBody_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new EmailContent("subject", invalid));
    }

    @Test
    void constructor_subjectTooLong_throws() {
        assertThrows(IllegalArgumentException.class, () -> new EmailContent("a".repeat(256), "body"));
    }

    @Test
    void equal_whenSameValues() {
        assertEquals(
                new EmailContent("a", "b"),
                new EmailContent("a", "b"));
    }

    @Test
    void notEqual_whenDifferentSubject() {
        assertNotEquals(
                new EmailContent("a", "b"),
                new EmailContent("c", "b"));
    }

    @Test
    void compareTo_bySubjectThenBody() {
        var a = new EmailContent("a", "b");
        var b = new EmailContent("a", "c");
        var c = new EmailContent("b", "a");
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(c) < 0);
        assertEquals(0, a.compareTo(new EmailContent("a", "b")));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new EmailContent("Welcome", "Thank you");
        JacksonTestUtil.assertRoundTrip(original, EmailContent.class);
    }
}
