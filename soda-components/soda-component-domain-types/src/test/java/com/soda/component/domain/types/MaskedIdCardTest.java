package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.component.domain.testutil.JacksonTestUtil.mapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("已脱敏身份证号值对象")
class MaskedIdCardTest {

    private static final String VALID_MASKED = "110101********1234";
    private static final String VALID_MASKED_X = "110101********123X";
    private static final String VALID_MASKED_2 = "310101********5678";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法脱敏格式创建实例")
        void should_create_when_validFormat() {
            var mic = MaskedIdCard.of(VALID_MASKED);
            assertThat(mic.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("校验位 x 大写归一化")
        void should_uppercaseX() {
            var mic = MaskedIdCard.of("110101********123x");
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
            assertThatThrownBy(() -> MaskedIdCard.of(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始身份证号等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawIdCard() {
            assertThat(MaskedIdCard.from(new IdCard("110101199001011234")))
                    .isEqualTo(MaskedIdCard.of("110101********1234"));
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_equal_sameValue() {
            assertThat(MaskedIdCard.of(VALID_MASKED)).isEqualTo(MaskedIdCard.of(VALID_MASKED));
        }

        @Test
        @DisplayName("x 大小写归一化后相等")
        void should_equal_caseInsensitiveX() {
            assertThat(MaskedIdCard.of(VALID_MASKED_X)).isEqualTo(MaskedIdCard.of("110101********123x"));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notEqual_diffValue() {
            assertThat(MaskedIdCard.of(VALID_MASKED)).isNotEqualTo(MaskedIdCard.of(VALID_MASKED_2));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_hashCodeConsistent() {
            assertThat(MaskedIdCard.of(VALID_MASKED)).hasSameHashCodeAs(MaskedIdCard.of(VALID_MASKED));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notEqual_otherMaskedType() {
            assertThat(MaskedIdCard.of(VALID_MASKED)).isNotEqualTo(MaskedBankCard.of("622588******6789"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("record 标准格式")
        void should_toStringRecordFormat() {
            assertThat(MaskedIdCard.of(VALID_MASKED)).hasToString("MaskedIdCard[value=110101********1234]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("JSON 序列化输出归一化后的脱敏标量")
        void should_serializeToScalar() {
            String json = mapper().writeValueAsString(MaskedIdCard.of("110101********123x"));
            assertThat(json).isEqualTo("\"" + VALID_MASKED_X + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例并归一化")
        void should_deserializeFromScalar() {
            var mic = mapper().readValue("\"" + VALID_MASKED + "\"", MaskedIdCard.class);
            assertThat(mic.value()).isEqualTo(VALID_MASKED);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(MaskedIdCard.of(VALID_MASKED), MaskedIdCard.class);
        }
    }
}
