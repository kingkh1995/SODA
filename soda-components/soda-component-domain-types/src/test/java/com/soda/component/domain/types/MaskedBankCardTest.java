package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("已脱敏银行卡号值对象")
class MaskedBankCardTest extends DomainPrimitiveContractTest<MaskedBankCard> {

    private static final String VALID_MASKED_16 = "622588******6789";

    @Override
    protected Contract<MaskedBankCard> contract() {
        return new Contract<>(MaskedBankCard.class, () -> new MaskedBankCard(VALID_MASKED_16),
                "\"" + VALID_MASKED_16 + "\"", "MaskedBankCard[value=" + VALID_MASKED_16 + "]",
                "\"6225881234566789\"", () -> new MaskedBankCard("622577******6789"));
    }

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
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始银行卡号等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawBankCard() {
            assertThat(MaskedBankCard.from(BankCard.of("6225881234566789")))
                    .isEqualTo(new MaskedBankCard("622588******6789"));
        }
    }
}
