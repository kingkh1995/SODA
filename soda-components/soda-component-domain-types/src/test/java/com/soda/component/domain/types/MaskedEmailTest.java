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

@DisplayName("已脱敏邮箱值对象")
class MaskedEmailTest {

    private static final String VALID_MASKED = "t***@example.com";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法脱敏格式创建实例")
        void should_create_when_validFormat() {
            var me = new MaskedEmail(VALID_MASKED);
            assertThat(me.value()).isEqualTo(VALID_MASKED);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "test@example.com", "te***@example.com", "***@example.com", "t***@example"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> new MaskedEmail(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new MaskedEmail(VALID_MASKED)).isEqualTo(new MaskedEmail(VALID_MASKED));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new MaskedEmail(VALID_MASKED)).isNotEqualTo(new MaskedEmail("a***@domain.org"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new MaskedEmail(VALID_MASKED)).hasSameHashCodeAs(new MaskedEmail(VALID_MASKED));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notBeEqual_when_otherMaskedType() {
            assertThat(new MaskedEmail(VALID_MASKED)).isNotEqualTo(new MaskedChineseName("张*"));
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始邮箱等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawEmail() {
            assertThat(MaskedEmail.from(Email.of("test@example.com")))
                    .isEqualTo(new MaskedEmail("t***@example.com"));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("JSON 序列化输出脱敏标量")
        void should_serializeToScalar() {
            var me = new MaskedEmail(VALID_MASKED);
            var json = MAPPER.writeValueAsString(me);
            assertThat(json).isEqualTo("\"" + VALID_MASKED + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromScalar() {
            var json = "\"" + VALID_MASKED + "\"";
            var me = MAPPER.readValue(json, MaskedEmail.class);
            assertThat(me.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            var original = new MaskedEmail(VALID_MASKED);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, MaskedEmail.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"test@example.com\"", MaskedEmail.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 返回脱敏串")
        void should_haveCorrectToString() {
            assertThat(new MaskedEmail(VALID_MASKED)).hasToString("MaskedEmail[value=" + VALID_MASKED + "]");
        }
    }
}