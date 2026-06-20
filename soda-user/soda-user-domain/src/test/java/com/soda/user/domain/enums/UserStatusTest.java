package com.soda.user.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

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
        var status = UserStatus.valueOf(name);
        assertEquals(desc, status.desc());
    }
}
