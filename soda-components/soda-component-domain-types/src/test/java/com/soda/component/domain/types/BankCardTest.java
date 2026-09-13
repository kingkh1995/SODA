package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("银行卡号值对象")
class BankCardTest extends DomainPrimitiveContractTest<BankCard> {

    private static final String VALID_CARD = "6225880123456789";

    @Override
    protected Contract<BankCard> contract() {
        return new Contract<>(BankCard.class, () -> BankCard.of(VALID_CARD), "\"" + VALID_CARD + "\"",
                "BankCard[masked=622588******6789]", "\"not-a-card\"",
                () -> BankCard.of("6225880123456788"));
    }

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
}
