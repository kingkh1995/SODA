package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialHashTest {

    private static final String VALID_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String VALID_HASH_2 = "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8";

    @Test
    void constructor_valid_creates() {
        var ch = new CredentialHash(VALID_HASH);
        assertEquals(VALID_HASH, ch.value());
    }

    @Test
    void constructor_anyNonBlank_accepts() {
        var ch = new CredentialHash("any-hash-value");
        assertEquals("any-hash-value", ch.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void constructor_nullOrEmpty_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new CredentialHash(invalid));
    }

    @Test
    void equal_whenSameValue() {
        assertEquals(new CredentialHash(VALID_HASH), new CredentialHash(VALID_HASH));
    }


    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new CredentialHash(VALID_HASH);
        JacksonTestUtil.assertRoundTrip(original, CredentialHash.class);
    }
}
