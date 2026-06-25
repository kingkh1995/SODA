package com.soda.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarTest {


    private static final String VALID_URL = "https://example.com/avatar.png";

    @Test
    void constructor_validUrl_creates() {
        var a = new Avatar(VALID_URL);
        assertEquals(VALID_URL, a.value());
    }

    @Test
    void constructor_whitespaceAround_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Avatar("  " + VALID_URL + "  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "not a url"})
    void constructor_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new Avatar(invalid));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/relative/path", "relative", "files/avatar.png"})
    void constructor_relativePath_throws(String relative) {
        assertThrows(IllegalArgumentException.class, () -> new Avatar(relative));
    }

    @ParameterizedTest
    @ValueSource(strings = {"file:///local/1.png", "ftp://files/avatar.png", "data:image/png;base64,abc"})
    void constructor_nonHttpScheme_throws(String nonHttp) {
        assertThrows(IllegalArgumentException.class, () -> new Avatar(nonHttp));
    }

    @Test
    void constructor_httpScheme_accepted() {
        var a = new Avatar("http://example.com/avatar.png");
        assertEquals("http://example.com/avatar.png", a.value());
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new Avatar(VALID_URL), new Avatar(VALID_URL));
    }

    @Test
    void notEqual_whenDifferentValue() {
        assertNotEquals(new Avatar("https://a.com/1.png"), new Avatar("https://a.com/2.png"));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new Avatar(VALID_URL);
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, Avatar.class);
        assertEquals(original, restored);
    }
}
