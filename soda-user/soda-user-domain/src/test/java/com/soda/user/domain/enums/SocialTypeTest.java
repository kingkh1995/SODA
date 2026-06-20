package com.soda.user.domain.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SocialTypeTest {

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
        var type = SocialType.valueOf(name);
        assertEquals(desc, type.desc());
    }
}
