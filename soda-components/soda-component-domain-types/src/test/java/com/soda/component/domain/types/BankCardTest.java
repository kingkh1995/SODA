package com.soda.component.domain.types;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.core.JacksonException;

import static com.soda.component.domain.testutil.JacksonTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("银行卡号值对象")
class BankCardTest {

    private static final String VALID_CARD = "6225880123456789";

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 16 位银行卡创建实例")
        void should_create_when_valid16() {
            var card = BankCard.of(VALID_CARD);
            assertThat(card.value()).isEqualTo(VALID_CARD);
        }

        @Test
        @DisplayName("合法 13 位银行卡创建实例")
        void should_create_when_valid13() {
            var card = BankCard.of("1234567890123");
            assertThat(card.value()).isEqualTo("1234567890123");
        }

        @Test
        @DisplayName("合法 19 位银行卡创建实例")
        void should_create_when_valid19() {
            var card = BankCard.of("1234567890123456789");
            assertThat(card.value()).isEqualTo("1234567890123456789");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {" ", "123", "abc", "123456789012", "12345678901234567890", "6225880123456789a"})
        @DisplayName("null / 非法输入抛出异常")
        void should_throw_when_invalid(String input) {
            assertThatThrownBy(() -> BankCard.of(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(BankCard.of(VALID_CARD)).isEqualTo(BankCard.of(VALID_CARD));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(BankCard.of(VALID_CARD)).isNotEqualTo(BankCard.of("6225880123456788"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode_when_sameValue() {
            assertThat(BankCard.of(VALID_CARD)).hasSameHashCodeAs(BankCard.of(VALID_CARD));
        }
    }

    @Nested
    @DisplayName("脱敏行为")
    class Masking {

        @Test
        @DisplayName("maskedValue 返回脱敏格式：前 6 位 + * + 后 4 位")
        void should_mask_when_16Digits() {
            var card = BankCard.of(VALID_CARD);
            assertThat(card.maskedValue()).isEqualTo("622588******6789");
        }

        @Test
        @DisplayName("13 位卡号脱敏")
        void should_mask_when_13Digits() {
            var card = BankCard.of("1234567890123");
            assertThat(card.maskedValue()).isEqualTo("123456***0123");
        }

        @Test
        @DisplayName("19 位卡号脱敏")
        void should_mask_when_19Digits() {
            var card = BankCard.of("1234567890123456789");
            assertThat(card.maskedValue()).isEqualTo("123456*********6789");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("JSON 序列化输出原值")
        void should_serializeToValue() {
            var json = MAPPER.writeValueAsString(BankCard.of(VALID_CARD));
            assertThat(json).isEqualTo("\"" + VALID_CARD + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromValue() {
            var card = MAPPER.readValue("\"" + VALID_CARD + "\"", BankCard.class);
            assertThat(card.value()).isEqualTo(VALID_CARD);
        }

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() {
            var original = BankCard.of(VALID_CARD);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, BankCard.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"not-a-card\"", BankCard.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 输出脱敏标准格式")
        void should_haveMaskedToString() {
            var card = BankCard.of(VALID_CARD);
            assertThat(card.toString()).isEqualTo("BankCard[masked=622588******6789]");
        }
    }
}