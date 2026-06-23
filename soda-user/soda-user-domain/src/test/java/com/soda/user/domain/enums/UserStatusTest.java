package com.soda.user.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserStatusTest {

    @Test
    void values_containsTwo() {
        assertEquals(2, UserStatus.values().length);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
        E,     Enabled
        D,     Disabled
    """)
    void constant(String name, String desc) {
        assertEquals(desc, UserStatus.valueOf(name).desc());
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"E", "D"})
    void of(String name) {
        assertEquals(UserStatus.valueOf(name), UserStatus.of(name));
    }

    @Test
    void of_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> UserStatus.of(null));
    }

    @Test
    void jackson_serializeDeserialize() throws Exception {
        assertEquals("\"E\"", MAPPER.writeValueAsString(UserStatus.E));
        assertEquals(UserStatus.E, MAPPER.readValue("\"E\"", UserStatus.class));
        assertEquals(UserStatus.D, MAPPER.readValue("\"D\"", UserStatus.class));
    }
}
