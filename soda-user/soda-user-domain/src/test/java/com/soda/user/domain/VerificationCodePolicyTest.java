package com.soda.user.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationCodePolicyTest {


    @Test
    void constructor_validParams_creates() {
        var policy = new VerificationCodePolicy(6, Duration.ofMinutes(5));
        assertEquals(6, policy.codeLength());
        assertEquals(Duration.ofMinutes(5), policy.expiry());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 21})
    void constructor_invalidCodeLength_throws(int invalid) {
        assertThrows(IllegalArgumentException.class,
                () -> new VerificationCodePolicy(invalid, Duration.ofMinutes(5)));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void constructor_nonPositiveExpiry_throws(long minutes) {
        assertThrows(IllegalArgumentException.class,
                () -> new VerificationCodePolicy(6, Duration.ofMinutes(minutes)));
    }

    @Test
    void constructor_nullExpiry_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new VerificationCodePolicy(6, null));
    }

    @Test
    void defaultSms_hasCorrectValues() {
        assertEquals(6, VerificationCodePolicy.DEFAULT_SMS.codeLength());
        assertEquals(Duration.ofMinutes(5), VerificationCodePolicy.DEFAULT_SMS.expiry());
    }

    @Test
    void defaultEmail_hasCorrectValues() {
        assertEquals(8, VerificationCodePolicy.DEFAULT_EMAIL.codeLength());
        assertEquals(Duration.ofMinutes(30), VerificationCodePolicy.DEFAULT_EMAIL.expiry());
    }

    @Test
    void equal_whenSameParams() {
        assertEquals(
                new VerificationCodePolicy(6, Duration.ofMinutes(5)),
                new VerificationCodePolicy(6, Duration.ofMinutes(5)));
    }

    @Test
    void notEqual_whenDifferentParams() {
        assertNotEquals(
                new VerificationCodePolicy(6, Duration.ofMinutes(5)),
                new VerificationCodePolicy(8, Duration.ofMinutes(30)));
    }

    @Test
    void compareTo_comparesCodeLengthFirstThenExpiry() {
        var a = new VerificationCodePolicy(6, Duration.ofMinutes(5));
        var b = new VerificationCodePolicy(8, Duration.ofMinutes(5));
        var c = new VerificationCodePolicy(6, Duration.ofMinutes(10));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(c) < 0);
        assertEquals(0, a.compareTo(a));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = new VerificationCodePolicy(6, Duration.ofMinutes(5));
        var json = MAPPER.writeValueAsString(original);
        assertTrue(json.contains("6"));
        var restored = MAPPER.readValue(json, VerificationCodePolicy.class);
        assertEquals(original, restored);
    }
}
