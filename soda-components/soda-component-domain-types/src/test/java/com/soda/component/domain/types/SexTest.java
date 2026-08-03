package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.mapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Sex 枚举")
class SexTest {

    @Test
    @DisplayName("枚举常量数量")
    void should_haveCorrectCount() {
        assertThat(Sex.values()).hasSize(2);
    }

    @ParameterizedTest(name = "{0} → desc={1}")
    @CsvSource(textBlock = """
                M,     male
                F,     female
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
        assertThat(mapper().writeValueAsString(Sex.M)).isEqualTo("\"M\"");
        assertThat(mapper().readValue("\"M\"", Sex.class)).isEqualTo(Sex.M);
    }

    @Test
    @DisplayName("非法枚举名称拒绝")
    void should_throw_when_invalidJson() {
        assertThatThrownBy(() -> mapper().readValue("\"INVALID\"", Sex.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    @DisplayName("toString 返回枚举名")
    void should_returnName() {
        assertThat(Sex.M).hasToString("M");
    }
}
