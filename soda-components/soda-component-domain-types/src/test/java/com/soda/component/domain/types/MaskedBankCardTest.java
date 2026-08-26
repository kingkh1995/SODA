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

@DisplayName("已脱敏银行卡号值对象")
class MaskedBankCardTest {

    private static final String VALID_MASKED_16 = "622588******6789";

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法 16 位脱敏格式创建实例")
        void should_create_when_valid16() {
            var mbc = new MaskedBankCard("622588******6789");
            assertThat(mbc.value()).isEqualTo("622588******6789");
        }

        @Test
        @DisplayName("合法 13 位脱敏格式创建实例")
        void should_create_when_valid13() {
            var mbc = new MaskedBankCard("123456***0123");
            assertThat(mbc.value()).isEqualTo("123456***0123");
        }

        @Test
        @DisplayName("合法 19 位脱敏格式创建实例")
        void should_create_when_valid19() {
            var mbc = new MaskedBankCard("622588*********7890");
            assertThat(mbc.value()).isEqualTo("622588*********7890");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "6225881234566789", "622588**6789", "622588**********6789", "622588***********6789", "622588************6789", "622588*************6789", "622588**************6789", "abc588******6789", "622588******67"})
        @DisplayName("非法脱敏格式抛出异常")
        void should_throw_when_invalidFormat(String input) {
            assertThatThrownBy(() -> new MaskedBankCard(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(new MaskedBankCard(VALID_MASKED_16)).isEqualTo(new MaskedBankCard(VALID_MASKED_16));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new MaskedBankCard(VALID_MASKED_16)).isNotEqualTo(new MaskedBankCard("622577******6789"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new MaskedBankCard(VALID_MASKED_16)).hasSameHashCodeAs(new MaskedBankCard(VALID_MASKED_16));
        }

        @Test
        @DisplayName("不同 Masked 类型不相等")
        void should_notBeEqual_when_otherMaskedType() {
            assertThat(new MaskedBankCard("123456***0123")).isNotEqualTo(new MaskedMobile("138****8000"));
        }
    }

    @Nested
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始银行卡号等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawBankCard() {
            assertThat(MaskedBankCard.from(BankCard.of("6225881234566789")))
                    .isEqualTo(new MaskedBankCard("622588******6789"));
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("JSON 序列化输出脱敏标量")
        void should_serializeToScalar() {
            var mbc = new MaskedBankCard(VALID_MASKED_16);
            var json = MAPPER.writeValueAsString(mbc);
            assertThat(json).isEqualTo("\"" + VALID_MASKED_16 + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromScalar() {
            var json = "\"" + VALID_MASKED_16 + "\"";
            var mbc = MAPPER.readValue(json, MaskedBankCard.class);
            assertThat(mbc.value()).isEqualTo(VALID_MASKED_16);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            var original = new MaskedBankCard(VALID_MASKED_16);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, MaskedBankCard.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"6225881234566789\"", MaskedBankCard.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 返回脱敏串")
        void should_haveCorrectToString() {
            assertThat(new MaskedBankCard(VALID_MASKED_16)).hasToString("MaskedBankCard[value=" + VALID_MASKED_16 + "]");
        }
    }
}