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

@DisplayName("已脱敏真实姓名值对象")
class MaskedChineseNameTest {

    /**
     * maskOf(ChineseName.of("张三")) = "张*" — 2 字原始名脱敏输出（保留首字 + 1 个 `*`）
     */
    private static final String VALID_MASKED = "张*";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("单姓脱敏格式（首字 + 1 个 `*`）创建实例")
        void should_create_when_singleSurname() {
            var mcn = new MaskedChineseName(VALID_MASKED);
            assertThat(mcn.value()).isEqualTo(VALID_MASKED);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "张", "张**", "李*明", "a*", "1*", "**", "*张", "张*张", "张**"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> new MaskedChineseName(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new MaskedChineseName(VALID_MASKED)).isEqualTo(new MaskedChineseName(VALID_MASKED));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new MaskedChineseName(VALID_MASKED)).isNotEqualTo(new MaskedChineseName("李*"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new MaskedChineseName(VALID_MASKED)).hasSameHashCodeAs(new MaskedChineseName(VALID_MASKED));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notBeEqual_when_otherMaskedType() {
            assertThat(new MaskedChineseName(VALID_MASKED)).isNotEqualTo(new MaskedIdCard("110101********1234"));
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始中文姓名等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawChineseName() {
            assertThat(MaskedChineseName.from(ChineseName.of("张三")))
                    .isEqualTo(new MaskedChineseName(VALID_MASKED));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("JSON 序列化输出脱敏标量")
        void should_serializeToScalar() {
            var mrn = new MaskedChineseName(VALID_MASKED);
            var json = MAPPER.writeValueAsString(mrn);
            assertThat(json).isEqualTo("\"" + VALID_MASKED + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromScalar() {
            var json = "\"" + VALID_MASKED + "\"";
            var mrn = MAPPER.readValue(json, MaskedChineseName.class);
            assertThat(mrn.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            var original = new MaskedChineseName(VALID_MASKED);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, MaskedChineseName.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝（未脱敏原始名）")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"张三\"", MaskedChineseName.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 返回脱敏串")
        void should_haveCorrectToString() {
            assertThat(new MaskedChineseName(VALID_MASKED)).hasToString("MaskedChineseName[value=" + VALID_MASKED + "]");
        }
    }
}