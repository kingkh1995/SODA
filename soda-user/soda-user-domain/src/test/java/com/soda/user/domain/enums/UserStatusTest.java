package com.soda.user.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("UserStatus 枚举")
class UserStatusTest {

    @Test
    @DisplayName("枚举常量数量")
    void should_haveCorrectCount() {
        assertThat(UserStatus.values()).hasSize(2);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
        E,     Enabled
        D,     Disabled
    """)
    @DisplayName("各枚举常量 desc() 正确")
    void should_haveCorrectDesc(String name, String desc) {
        assertThat(UserStatus.valueOf(name).desc()).isEqualTo(desc);
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"E", "D"})
    @DisplayName("of(String) 查找正确")
    void should_findByName(String name) {
        assertThat(UserStatus.of(name)).isEqualTo(UserStatus.valueOf(name));
    }

    @Test
    @DisplayName("of(null) 抛出异常")
    void should_throw_when_null() {
        assertThatThrownBy(() -> UserStatus.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Jackson round-trip")
    void should_serializeDeserialize() throws Exception {
        assertThat(MAPPER.writeValueAsString(UserStatus.E)).isEqualTo("\"E\"");
        assertThat(MAPPER.readValue("\"E\"", UserStatus.class)).isEqualTo(UserStatus.E);
    }

    @Test
    @DisplayName("非法枚举名称拒绝")
    void should_throw_when_invalidJson() {
        assertThatThrownBy(() -> MAPPER.readValue("\"INVALID\"", UserStatus.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("toString 返回枚举名")
    void should_returnName() {
        assertThat(UserStatus.E).hasToString("E");
    }
}
