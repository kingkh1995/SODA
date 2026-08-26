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

@DisplayName("身份证号值对象")
class IdCardTest {

    private static final String VALID_ID_CARD = "110101199003071234";
    private static final String VALID_ID_CARD_X = "11010119900307123X";
    private static final String VALID_ID_CARD_x = "11010119900307123x";

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
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(IdCard.of(VALID_ID_CARD)).isEqualTo(IdCard.of(VALID_ID_CARD));
        }

        @Test
        @DisplayName("X 大小写归一化后相等")
        void should_beEqual_when_trailingXCaseDiffers() {
            assertThat(IdCard.of(VALID_ID_CARD_X)).isEqualTo(IdCard.of(VALID_ID_CARD_x));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(IdCard.of(VALID_ID_CARD)).isNotEqualTo(IdCard.of("110101199003071235"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(IdCard.of(VALID_ID_CARD).hashCode()).isEqualTo(IdCard.of(VALID_ID_CARD).hashCode());
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

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = IdCard.of(VALID_ID_CARD);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, IdCard.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("JSON 序列化输出原值（大写）")
        void should_serializeToValue() throws Exception {
            var json = MAPPER.writeValueAsString(IdCard.of(VALID_ID_CARD_x));
            assertThat(json).isEqualTo("\"" + VALID_ID_CARD_X + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromValue() throws Exception {
            var card = MAPPER.readValue("\"" + VALID_ID_CARD + "\"", IdCard.class);
            assertThat(card.value()).isEqualTo(VALID_ID_CARD);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"invalid-id-card\"", IdCard.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确（脱敏）")
        void should_haveCorrectToString() {
            var card = IdCard.of(VALID_ID_CARD);
            assertThat(card).hasToString("IdCard[masked=110101********1234]");
        }
    }
}