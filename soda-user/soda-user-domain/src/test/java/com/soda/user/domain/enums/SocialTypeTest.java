package com.soda.user.domain.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SocialTypeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void values_containsSeven() {
        assertEquals(7, SocialType.values().length);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
        GE,    Gitee
        DT,    DingTalk
        WENT,  WechatWork
        WMP,   WechatMp
        WOPN,  WechatOpen
        WMIN,  WechatMini
        ALIP,  AlipayMini
    """)
    void constant(String name, String desc) {
        assertEquals(desc, SocialType.valueOf(name).desc());
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"GE", "DT", "WENT", "WMP", "WOPN", "WMIN", "ALIP"})
    void of(String name) {
        assertEquals(SocialType.valueOf(name), SocialType.of(name));
    }

    @Test
    void of_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> SocialType.of(null));
    }

    @ParameterizedTest(name = "jackson round-trip {0}")
    @CsvSource({"GE", "DT", "WENT", "WMP", "WOPN", "WMIN", "ALIP"})
    void jackson_serializeDeserialize(String name) throws Exception {
        var value = SocialType.valueOf(name);
        assertEquals("\"" + name + "\"", MAPPER.writeValueAsString(value));
        assertEquals(value, MAPPER.readValue("\"" + name + "\"", SocialType.class));
    }
}
