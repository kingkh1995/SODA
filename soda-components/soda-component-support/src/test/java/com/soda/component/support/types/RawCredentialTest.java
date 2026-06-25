package com.soda.component.support.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawCredentialTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void of_valid_creates() {
        var cred = new RawCredential("mySecret123");
        assertEquals("mySecret123", cred.internalValue());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void of_nullOrEmpty_throws(String invalid) {
        assertThrows(IllegalArgumentException.class, () -> new RawCredential(invalid));
    }

    @Test
    void toString_masksValue() {
        var cred = new RawCredential("secret");
        assertEquals("RawCredential[***]", cred.toString());
    }

    @Test
    void jackson_serialization_rejected() {
        var cred = new RawCredential("secret123");
        assertThrows(Exception.class, () -> MAPPER.writeValueAsString(cred));
    }

    @Test
    void jackson_deserialization_rejected() {
        assertThrows(Exception.class, () ->
                MAPPER.readValue("{\"value\":\"test\"}", RawCredential.class));
    }
}
