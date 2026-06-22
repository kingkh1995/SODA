package com.soda.user.domain.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class AuthAccountTypeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void values_containsFour() {
        assertEquals(4, AuthAccountType.values().length);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
        P,     Password
        S,     Sms
        E,     Email
        O,     OAuth
    """)
    void constant(String name, String desc) {
        assertEquals(desc, AuthAccountType.valueOf(name).desc());
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"P", "S", "E", "O"})
    void of(String name) {
        assertEquals(AuthAccountType.valueOf(name), AuthAccountType.of(name));
    }

    @Test
    void of_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> AuthAccountType.of(null));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        assertEquals("\"P\"", MAPPER.writeValueAsString(AuthAccountType.P));
        assertEquals(AuthAccountType.P, MAPPER.readValue("\"P\"", AuthAccountType.class));
        assertEquals(AuthAccountType.S, MAPPER.readValue("\"S\"", AuthAccountType.class));
        assertEquals(AuthAccountType.E, MAPPER.readValue("\"E\"", AuthAccountType.class));
        assertEquals(AuthAccountType.O, MAPPER.readValue("\"O\"", AuthAccountType.class));
    }
}
