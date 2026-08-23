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

@DisplayName("已脱敏银行卡号值对象")
class MaskedBankCardTest {

    private static final String VALID_MASKED_16 = "622588******6789";
    private static final String VALID_MASKED_13 = "123456***0123";
    private static final String VALID_MASKED_19 = "622588*********7890";
    private static final String VALID_MASKED_2 = "622577******6789";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法 16 位脱敏格式创建实例")
        void should_create_when_valid16() {
            var mbc = MaskedBankCard.of(VALID_MASKED_16);
            assertThat(mbc.value()).isEqualTo(VALID_MASKED_16);
        }

        @Test
        @DisplayName("合法 13 位脱敏格式创建实例")
        void should_create_when_valid13() {
            var mbc = MaskedBankCard.of(VALID_MASKED_13);
            assertThat(mbc.value()).isEqualTo(VALID_MASKED_13);
        }

        @Test
        @DisplayName("合法 19 位脱敏格式创建实例")
        void should_create_when_valid19() {
            var mbc = MaskedBankCard.of(VALID_MASKED_19);
            assertThat(mbc.value()).isEqualTo(VALID_MASKED_19);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "6225881234566789", "622588**6789", "622588**************6789", "abc588******6789", "622588******67"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> MaskedBankCard.of(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始银行卡号等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawBankCard() {
            assertThat(MaskedBankCard.from(new BankCard("6225881234566789")))
                    .isEqualTo(MaskedBankCard.of("622588******6789"));
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_equal_sameValue() {
            assertThat(MaskedBankCard.of(VALID_MASKED_16)).isEqualTo(MaskedBankCard.of(VALID_MASKED_16));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notEqual_diffValue() {
            assertThat(MaskedBankCard.of(VALID_MASKED_16)).isNotEqualTo(MaskedBankCard.of(VALID_MASKED_2));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_hashCodeConsistent() {
            assertThat(MaskedBankCard.of(VALID_MASKED_16)).hasSameHashCodeAs(MaskedBankCard.of(VALID_MASKED_16));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notEqual_otherMaskedType() {
            assertThat(MaskedBankCard.of(VALID_MASKED_13)).isNotEqualTo(MaskedMobile.of("138****8000"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("record 标准格式")
        void should_toStringRecordFormat() {
            assertThat(MaskedBankCard.of(VALID_MASKED_16)).hasToString("MaskedBankCard[value=622588******6789]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("JSON 序列化输出脱敏标量")
        void should_serializeToScalar() {
            String json = mapper().writeValueAsString(MaskedBankCard.of(VALID_MASKED_16));
            assertThat(json).isEqualTo("\"" + VALID_MASKED_16 + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromScalar() {
            var mbc = mapper().readValue("\"" + VALID_MASKED_16 + "\"", MaskedBankCard.class);
            assertThat(mbc.value()).isEqualTo(VALID_MASKED_16);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(MaskedBankCard.of(VALID_MASKED_16), MaskedBankCard.class);
        }
    }
}
