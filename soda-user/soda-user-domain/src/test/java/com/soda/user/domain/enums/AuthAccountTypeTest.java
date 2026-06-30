package com.soda.user.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthAccountType 枚举")
class AuthAccountTypeTest {

    @Test
    @DisplayName("枚举常量数量")
    void should_haveCorrectCount() {
        assertThat(AuthAccountType.values()).hasSize(4);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
                P,     Password
                S,     Sms
                E,     Email
                O,     OAuth
            """)
    @DisplayName("各枚举常量 desc() 正确")
    void should_haveCorrectDesc(String name, String desc) {
        assertThat(AuthAccountType.valueOf(name).desc()).isEqualTo(desc);
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"P", "S", "E", "O"})
    @DisplayName("of(String) 查找正确")
    void should_findByName(String name) {
        assertThat(AuthAccountType.of(name)).isEqualTo(AuthAccountType.valueOf(name));
    }

    @Test
    @DisplayName("of(null) 抛出异常")
    void should_throw_when_null() {
        assertThatThrownBy(() -> AuthAccountType.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Jackson round-trip")
    void should_serializeDeserialize() throws Exception {
        assertThat(MAPPER.writeValueAsString(AuthAccountType.P)).isEqualTo("\"P\"");
        assertThat(MAPPER.readValue("\"P\"", AuthAccountType.class)).isEqualTo(AuthAccountType.P);
    }

    @Test
    @DisplayName("非法枚举名称拒绝")
    void should_throw_when_invalidJson() {
        assertThatThrownBy(() -> MAPPER.readValue("\"INVALID\"", AuthAccountType.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    @DisplayName("toString 返回枚举名")
    void should_returnName() {
        assertThat(AuthAccountType.P).hasToString("P");
    }
}
