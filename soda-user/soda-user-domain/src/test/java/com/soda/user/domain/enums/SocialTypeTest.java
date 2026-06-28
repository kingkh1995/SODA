package com.soda.user.domain.enums;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SocialType 枚举")
class SocialTypeTest {

    @Test
    @DisplayName("枚举常量数量")
    void should_haveCorrectCount() {
        assertThat(SocialType.values()).hasSize(7);
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
    @DisplayName("各枚举常量 desc() 正确")
    void should_haveCorrectDesc(String name, String desc) {
        assertThat(SocialType.valueOf(name).desc()).isEqualTo(desc);
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"GE", "DT", "WENT", "WMP", "WOPN", "WMIN", "ALIP"})
    @DisplayName("of(String) 查找正确")
    void should_findByName(String name) {
        assertThat(SocialType.of(name)).isEqualTo(SocialType.valueOf(name));
    }

    @Test
    @DisplayName("of(null) 抛出异常")
    void should_throw_when_null() {
        assertThatThrownBy(() -> SocialType.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Jackson round-trip {0}")
    @CsvSource({"GE", "DT", "WENT", "WMP", "WOPN", "WMIN", "ALIP"})
    @DisplayName("Jackson round-trip")
    void should_serializeDeserialize(String name) throws Exception {
        var value = SocialType.valueOf(name);
        assertThat(MAPPER.writeValueAsString(value)).isEqualTo("\"" + name + "\"");
        assertThat(MAPPER.readValue("\"" + name + "\"", SocialType.class)).isEqualTo(value);
    }

    @Test
    @DisplayName("非法枚举名称拒绝")
    void should_throw_when_invalidJson() {
        assertThatThrownBy(() -> MAPPER.readValue("\"INVALID\"", SocialType.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("toString 返回枚举名")
    void should_returnName() {
        assertThat(SocialType.GE).hasToString("GE");
    }
}
