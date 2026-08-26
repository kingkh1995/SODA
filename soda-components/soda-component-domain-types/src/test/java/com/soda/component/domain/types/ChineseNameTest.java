package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("真实姓名值对象")
class ChineseNameTest {

    private static final String VALID_NAME = "张三";

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("单姓创建实例")
        void should_create_when_singleSurname() {
            var name = ChineseName.of(VALID_NAME);
            assertThat(name.value()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("复姓创建实例")
        void should_create_when_compoundSurname() {
            var name = ChineseName.of("欧阳修");
            assertThat(name.value()).isEqualTo("欧阳修");
        }

        @Test
        @DisplayName("长姓名创建实例")
        void should_create_when_longName() {
            var name = ChineseName.of("张三丰道长");
            assertThat(name.value()).isEqualTo("张三丰道长");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "张", "John", "张3", "张@", "a"})
        @DisplayName("非法输入抛出异常")
        void should_throw_when_invalid(String input) {
            assertThatThrownBy(() -> ChineseName.of(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(ChineseName.of(VALID_NAME)).isEqualTo(ChineseName.of(VALID_NAME));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(ChineseName.of(VALID_NAME)).isNotEqualTo(ChineseName.of("李四"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode_when_sameValue() {
            assertThat(ChineseName.of(VALID_NAME)).hasSameHashCodeAs(ChineseName.of(VALID_NAME));
        }
    }

    @Nested
    @DisplayName("脱敏行为")
    class Masking {

        @Test
        @DisplayName("2 字姓名脱敏：保留姓氏 + 1 个 *")
        void should_maskSingleSurname() {
            var name = ChineseName.of(VALID_NAME);
            assertThat(name.maskedValue()).isEqualTo("张*");
        }

        @Test
        @DisplayName("3 字姓名脱敏：保留首字 + 1 个 *（复姓也按此规则，不再额外保留）")
        void should_maskCompoundSurnameUniformly() {
            var name = ChineseName.of("欧阳修");
            assertThat(name.maskedValue()).isEqualTo("欧*");
        }

        @ParameterizedTest
        @ValueSource(strings = {"司马迁", "诸葛亮", "慕容复", "夏侯惇", "公孙胜", "令狐冲", "司徒雷登", "张三丰道长"})
        @DisplayName("全部姓名按单星规则脱敏：仅保留首字 + 1 个 *")
        void should_maskAllNamesBySingleStar(String input) {
            var name = ChineseName.of(input);
            assertThat(name.maskedValue()).isEqualTo(input.substring(0, 1) + "*");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("JSON 序列化输出原值")
        void should_serializeToValue() {
            var json = MAPPER.writeValueAsString(ChineseName.of(VALID_NAME));
            assertThat(json).isEqualTo("\"" + VALID_NAME + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromValue() {
            var name = MAPPER.readValue("\"" + VALID_NAME + "\"", ChineseName.class);
            assertThat(name.value()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() {
            var original = ChineseName.of(VALID_NAME);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, ChineseName.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"John\"", ChineseName.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 输出脱敏标准格式")
        void should_haveMaskedToString() {
            var name = ChineseName.of(VALID_NAME);
            assertThat(name.toString()).isEqualTo("ChineseName[masked=张*]");
        }
    }
}