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

@DisplayName("已脱敏身份证号值对象")
class MaskedIdCardTest extends DomainPrimitiveContractTest<MaskedIdCard> {

    private static final String VALID_MASKED = "110101********1234";
    private static final String VALID_MASKED_X = "110101********123X";

    @Override
    protected Contract<MaskedIdCard> contract() {
        return new Contract<>(MaskedIdCard.class, () -> new MaskedIdCard(VALID_MASKED),
                "\"" + VALID_MASKED + "\"", "MaskedIdCard[value=" + VALID_MASKED + "]",
                "\"110101199001011234\"", () -> new MaskedIdCard("310101********5678"));
    }

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
            var mic = new MaskedIdCard("110101********123x");
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
    @DisplayName("跨类型转换")
    class Conversion {
        @Test
        @DisplayName("from 原始身份证号等价于 of 掩码算法输出")
        void should_fromEqualOf_when_rawIdCard() {
            assertThat(MaskedIdCard.from(IdCard.of("110101199001011234")))
                    .isEqualTo(new MaskedIdCard("110101********1234"));
        }
    }
}
