package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Sex 枚举")
class SexTest {

    @Nested
    @DisplayName("查找")
    class Lookup {

        @Test
        @DisplayName("枚举常量数量")
        void should_haveCorrectCount() {
            assertThat(Sex.values()).hasSize(2);
        }

        @ParameterizedTest(name = "of({0}) → {0}")
        @CsvSource({"M", "F"})
        @DisplayName("合法名查找返回对应枚举")
        void should_findByName_when_validName(String name) {
            assertThat(Sex.of(name)).isEqualTo(Sex.valueOf(name));
        }

        @Test
        @DisplayName("null 名拒绝")
        void should_throw_when_nameIsNull() {
            assertThatThrownBy(() -> Sex.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空名拒绝")
        void should_throw_when_nameIsBlank() {
            assertThatThrownBy(() -> Sex.of(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("未知名拒绝")
        void should_throw_when_unknownName() {
            assertThatThrownBy(() -> Sex.of("X"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("显示")
    class Display {

        @ParameterizedTest(name = "{0} → desc={1}")
        @CsvSource(textBlock = """
                    M,     male
                    F,     female
                """)
        @DisplayName("各枚举常量 desc() 返回描述")
        void should_haveCorrectDesc(String name, String desc) {
            assertThat(Sex.valueOf(name).desc()).isEqualTo(desc);
        }

        @Test
        @DisplayName("toString 返回枚举名")
        void should_returnName() {
            assertThat(Sex.M).hasToString("M");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = Sex.M;
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).isEqualTo("\"M\"");
            assertThat(MAPPER.readValue(json, Sex.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法枚举名称拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"INVALID\"", Sex.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
