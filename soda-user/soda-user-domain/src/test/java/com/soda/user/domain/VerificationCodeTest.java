package com.soda.user.domain;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.soda.component.support.types.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * {@link VerificationCode} 单元测试。
 */
class VerificationCodeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);


    private static final String CODE = "123456";
    private static final Instant FUTURE = Instant.now().plus(1, ChronoUnit.HOURS);
    private static final Instant PAST = Instant.now().minus(1, ChronoUnit.HOURS);

    // ——— constructor ———

    @Test
    void constructor_validParams_creates() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertEquals(CODE, vc.code());
        assertEquals(FUTURE, vc.expireAt());
        assertFalse(vc.used());
    }

    @ParameterizedTest
    @NullSource
    void constructor_nullCode_throws(String invalidCode) {
        assertThrows(IllegalArgumentException.class, () -> new VerificationCode(invalidCode, FUTURE, false));
    }

    @Test
    void constructor_nullExpireAt_throws() {
        assertThrows(IllegalArgumentException.class, () -> new VerificationCode(CODE, null, false));
    }

    // ——— expired ———

    @Test
    void expired_futureDate_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertFalse(vc.expired());
    }

    @Test
    void expired_pastDate_returnsTrue() {
        var vc = new VerificationCode(CODE, PAST, false);
        assertTrue(vc.expired());
    }

    // ——— isUsed ———

    @Test
    void isUsed_fresh_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertFalse(vc.used());
    }

    @Test
    void isUsed_used_returnsTrue() {
        var vc = new VerificationCode(CODE, FUTURE, true);
        assertTrue(vc.used());
    }

    // ——— verify ———

    @Test
    void verify_matchingCodeNotExpiredNotUsed_returnsTrue() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertTrue(vc.verify(new RandomString(CODE)));
    }

    @Test
    void verify_wrongCode_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertFalse(vc.verify(new RandomString("wrong")));
    }

    @Test
    void verify_expired_returnsFalse() {
        var vc = new VerificationCode(CODE, PAST, false);
        assertFalse(vc.verify(new RandomString(CODE)));
    }

    @Test
    void verify_alreadyUsed_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, true);
        assertFalse(vc.verify(new RandomString(CODE)));
    }

    @Test
    void verify_nullInput_returnsFalse() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        assertFalse(vc.verify(null));
    }

    // ——— use() ———

    @Test
    void use_createsNewInstanceWithUsedTrue() {
        var vc = new VerificationCode(CODE, FUTURE, false);
        var used = vc.use();
        assertNotSame(vc, used);
        assertEquals(CODE, used.code());
        assertEquals(FUTURE, used.expireAt());
        assertTrue(used.used());
        assertFalse(vc.used());  // original unchanged
    }

    // ——— equals / hashCode ———

    @Test
    void equal_whenSameValues() {
        assertEquals(
                new VerificationCode(CODE, FUTURE, false),
                new VerificationCode(CODE, FUTURE, false));
    }

    @Test
    void notEqual_whenDifferentCode() {
        assertNotEquals(
                new VerificationCode("A", FUTURE, false),
                new VerificationCode("B", FUTURE, false));
    }

    @Test
    void notEqual_whenDifferentUsed() {
        assertNotEquals(
                new VerificationCode(CODE, FUTURE, false),
                new VerificationCode(CODE, FUTURE, true));
    }

    // ——— Jackson ———

    @Test
    void jackson_serializeDeserialize() {
        try {
            var original = new VerificationCode(CODE, FUTURE, false);
            var json = MAPPER.writeValueAsString(original);
            assertTrue(json.contains(CODE));
            var restored = MAPPER.readValue(json, VerificationCode.class);
            assertEquals(original, restored);
        } catch (Exception e) {
            fail(e);
        }
    }
}
