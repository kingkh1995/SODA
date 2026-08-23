package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("真实姓名值对象")
class ChineseNameTest {

    private static final String VALID_NAME = "张三";
    private static final String VALID_COMPOUND = "欧阳修";
    private static final String VALID_LONG = "张三丰道长";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("单姓创建实例")
        void should_create_when_singleSurname() {
            ChineseName name = new ChineseName(VALID_NAME);
            assertThat(name.value()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("复姓创建实例")
        void should_create_when_compoundSurname() {
            ChineseName name = new ChineseName(VALID_COMPOUND);
            assertThat(name.value()).isEqualTo(VALID_COMPOUND);
        }

        @Test
        @DisplayName("长姓名创建实例")
        void should_create_when_longName() {
            ChineseName name = new ChineseName(VALID_LONG);
            assertThat(name.value()).isEqualTo(VALID_LONG);
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
            assertThatThrownBy(() -> new ChineseName(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("脱敏行为")
    class Masking {

        @Test
        @DisplayName("单姓脱敏：保留姓氏，名替换为 *")
        void should_mask_singleSurname() {
            ChineseName name = new ChineseName(VALID_NAME);
            assertThat(name.maskedValue()).isEqualTo("张*");
        }

        @Test
        @DisplayName("复姓脱敏：同样只保留首字（均匀规则，无特例）")
        void should_mask_compoundSurnameUniformly() {
            ChineseName name = new ChineseName(VALID_COMPOUND);
            assertThat(name.maskedValue()).isEqualTo("欧**");
        }

        @ParameterizedTest
        @ValueSource(strings = {"司马迁", "诸葛亮", "慕容复", "夏侯惇", "公孙胜", "令狐冲", "司徒雷登"})
        @DisplayName("全部常见复姓按均匀规则脱敏：仅保留首字")
        void should_mask_allCompoundSurnames(String input) {
            ChineseName name = new ChineseName(input);
            assertThat(name.maskedValue()).isEqualTo(input.substring(0, 1) + "*".repeat(input.length() - 1));
        }

        @Test
        @DisplayName("长姓名脱敏：保留姓氏，其余替换为 *")
        void should_mask_longName() {
            ChineseName name = new ChineseName(VALID_LONG);
            assertThat(name.maskedValue()).isEqualTo("张****");
        }

        @Test
        @DisplayName("toString 输出 Maskable 标准格式")
        void should_toStringMasked() {
            ChineseName name = new ChineseName(VALID_NAME);
            assertThat(name.toString()).isEqualTo("ChineseName[masked=张*]");
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_equal_sameValue() {
            assertThat(new ChineseName(VALID_NAME)).isEqualTo(new ChineseName(VALID_NAME));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notEqual_diffValue() {
            assertThat(new ChineseName(VALID_NAME)).isNotEqualTo(new ChineseName("李四"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_hashCodeConsistent() {
            assertThat(new ChineseName(VALID_NAME).hashCode()).isEqualTo(new ChineseName(VALID_NAME).hashCode());
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("JSON 序列化输出原值")
        void should_serializeToValue() throws JacksonException {
            String json = MAPPER.writeValueAsString(new ChineseName(VALID_NAME));
            assertThat(json).isEqualTo("\"" + VALID_NAME + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromValue() throws JacksonException {
            ChineseName name = MAPPER.readValue("\"" + VALID_NAME + "\"", ChineseName.class);
            assertThat(name.value()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(new ChineseName(VALID_NAME), ChineseName.class);
        }
    }
}