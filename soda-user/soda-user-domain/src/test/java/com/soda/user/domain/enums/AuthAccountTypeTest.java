package com.soda.user.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class AuthAccountTypeTest {

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
        var type = AuthAccountType.valueOf(name);
        assertEquals(desc, type.desc());
    }
}
