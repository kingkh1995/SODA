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

@DisplayName("已脱敏身份证号值对象")
class MaskedIdCardTest {

    private static final String VALID_MASKED = "110101********1234";
    private static final String VALID_MASKED_X = "110101********123X";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法脱敏格式创建实例")
        void should_create_when_validFormat() {
            var mic = new MaskedIdCard("110101********1234");
            assertThat(mic.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("校验位 x 大写归一化")
        void should_normalizeCheckDigit_when_lowercaseX() {
            var mic = new MaskedIdCard("110101********123X");
            assertThat(mic.value()).isEqualTo(VALID_MASKED_X);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "110101199001011234", "110101*******1234", "110101*********234", "110101********123"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> new MaskedIdCard(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new MaskedIdCard(VALID_MASKED)).isEqualTo(new MaskedIdCard(VALID_MASKED));
        }

        @Test
        @DisplayName("x 大小写归一化后相等")
        void should_beEqual_when_caseInsensitiveX() {
            assertThat(new MaskedIdCard(VALID_MASKED_X)).isEqualTo(new MaskedIdCard("110101********123x"));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new MaskedIdCard(VALID_MASKED)).isNotEqualTo(new MaskedIdCard("310101********5678"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new MaskedIdCard(VALID_MASKED)).hasSameHashCodeAs(new MaskedIdCard(VALID_MASKED));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notBeEqual_when_otherMaskedType() {
            assertThat(new MaskedIdCard(VALID_MASKED)).isNotEqualTo(new MaskedBankCard("622588******6789"));
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始身份证号等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawIdCard() {
            assertThat(MaskedIdCard.from(IdCard.of("110101199001011234")))
                    .isEqualTo(new MaskedIdCard("110101********1234"));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("JSON 序列化输出归一化后的脱敏标量")
        void should_serializeToScalar() {
            var mic = new MaskedIdCard("110101********123x");
            var json = MAPPER.writeValueAsString(mic);
            assertThat(json).isEqualTo("\"" + VALID_MASKED_X + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例并归一化")
        void should_deserializeFromScalar() {
            var json = "\"" + VALID_MASKED + "\"";
            var mic = MAPPER.readValue(json, MaskedIdCard.class);
            assertThat(mic.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            var original = new MaskedIdCard(VALID_MASKED);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, MaskedIdCard.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"110101199001011234\"", MaskedIdCard.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 返回脱敏串")
        void should_haveCorrectToString() {
            assertThat(new MaskedIdCard(VALID_MASKED)).hasToString("MaskedIdCard[value=" + VALID_MASKED + "]");
        }
    }
}