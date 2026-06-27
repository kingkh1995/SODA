package com.soda.user.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.soda.user.domain.DomainTestUtil.MAPPER;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Sex 枚举")
class SexTest {

    @Test
    @DisplayName("枚举常量数量")
    void should_haveCorrectCount() {
        assertThat(Sex.values()).hasSize(2);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
        M,     Male
        F,     Female
    """)
    @DisplayName("各枚举常量 desc() 正确")
    void should_haveCorrectDesc(String name, String desc) {
        assertThat(Sex.valueOf(name).desc()).isEqualTo(desc);
    }

    @ParameterizedTest(name = "of({0}) → {0}")
    @CsvSource({"M", "F"})
    @DisplayName("of(String) 查找正确")
    void should_findByName(String name) {
        assertThat(Sex.of(name)).isEqualTo(Sex.valueOf(name));
    }

    @Test
    @DisplayName("of(null) 抛出异常")
    void should_throw_when_null() {
        assertThatThrownBy(() -> Sex.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Jackson round-trip")
    void should_serializeDeserialize() throws Exception {
        assertThat(MAPPER.writeValueAsString(Sex.M)).isEqualTo("\"M\"");
        assertThat(MAPPER.readValue("\"M\"", Sex.class)).isEqualTo(Sex.M);
    }

    @Test
    @DisplayName("非法枚举名称拒绝")
    void should_throw_when_invalidJson() {
        assertThatThrownBy(() -> MAPPER.readValue("\"INVALID\"", Sex.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    @DisplayName("toString 返回枚举名")
    void should_returnName() {
        assertThat(Sex.M).hasToString("M");
    }
}
