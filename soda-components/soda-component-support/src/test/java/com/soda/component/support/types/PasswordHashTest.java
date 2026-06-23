package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {

    private static final String VALID_BCRYPT = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String VALID_BCRYPT_2 = "$2b$08$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    @Test
    void constructor_valid_creates() {
        var ph = new PasswordHash(VALID_BCRYPT);
        assertEquals(VALID_BCRYPT, ph.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-bcrypt-hash", "$2a$10$tooShort", "", "  "})
    void constructor_invalid_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash(invalid));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new PasswordHash(VALID_BCRYPT), new PasswordHash(VALID_BCRYPT));
    }

    @Test
    void compareTo_delegatesToStringCompare() {
        assertTrue(new PasswordHash(VALID_BCRYPT).compareTo(new PasswordHash(VALID_BCRYPT_2)) != 0);
        assertEquals(0, new PasswordHash(VALID_BCRYPT).compareTo(new PasswordHash(VALID_BCRYPT)));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new PasswordHash(VALID_BCRYPT);
        JacksonTestUtil.assertRoundTrip(original, PasswordHash.class);
    }
}
