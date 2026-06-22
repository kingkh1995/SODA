package com.soda.user.domain.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SexTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void values_containsTwo() {
        assertEquals(2, Sex.values().length);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
        M,     Male
        F,     Female
    """)
    void constant(String name, String desc) {
        assertEquals(desc, Sex.valueOf(name).desc());
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"M", "F"})
    void of(String name) {
        assertEquals(Sex.valueOf(name), Sex.of(name));
    }

    @Test
    void of_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Sex.of(null));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        assertEquals("\"M\"", MAPPER.writeValueAsString(Sex.M));
        assertEquals(Sex.M, MAPPER.readValue("\"M\"", Sex.class));
        assertEquals(Sex.F, MAPPER.readValue("\"F\"", Sex.class));
    }
}
