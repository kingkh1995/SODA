package com.soda.user.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SexTest {

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
        var sex = Sex.valueOf(name);
        assertEquals(desc, sex.desc());
    }
}
