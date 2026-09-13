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

@DisplayName("身份证号值对象")
class IdCardTest extends DomainPrimitiveContractTest<IdCard> {

    private static final String VALID_ID_CARD = "110101199003071234";
    private static final String VALID_ID_CARD_X = "11010119900307123X";
    private static final String VALID_ID_CARD_x = "11010119900307123x";

    @Override
    protected Contract<IdCard> contract() {
        return new Contract<>(IdCard.class, () -> IdCard.of(VALID_ID_CARD), "\"" + VALID_ID_CARD + "\"",
                "IdCard[masked=110101********1234]", "\"invalid-id-card\"",
                () -> IdCard.of("110101199003071235"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 18 位身份证创建实例")
        void should_create_when_valid() {
            var card = IdCard.of(VALID_ID_CARD);
            assertThat(card.value()).isEqualTo(VALID_ID_CARD);
        }

        @Test
        @DisplayName("末尾 X 大写归一化")
        void should_normalize_when_trailingLowercaseX() {
            var card = IdCard.of(VALID_ID_CARD_x);
            assertThat(card.value()).isEqualTo(VALID_ID_CARD_X);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "11010119900307123", "1101011990030712345", "11010119900307123Y", "abc"})
        @DisplayName("非法输入抛出异常")
        void should_throw_when_invalid(String input) {
            assertThatThrownBy(() -> IdCard.of(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("脱敏行为")
    class Masking {

        @Test
        @DisplayName("maskedValue 返回脱敏格式：前 6 位 + 8 个 * + 后 4 位")
        void should_returnMaskedValue_when_valid() {
            var card = IdCard.of(VALID_ID_CARD);
            assertThat(card.maskedValue()).isEqualTo("110101********1234");
        }

        @Test
        @DisplayName("末尾 X 脱敏")
        void should_returnMaskedValue_when_trailingX() {
            var card = IdCard.of(VALID_ID_CARD_X);
            assertThat(card.maskedValue()).isEqualTo("110101********123X");
        }
    }
}
