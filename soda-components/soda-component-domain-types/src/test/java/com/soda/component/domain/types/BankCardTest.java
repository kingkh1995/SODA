package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("银行卡号值对象")
class BankCardTest {

    private static final String VALID_CARD = "6225880123456789";
    private static final String VALID_CARD_13 = "1234567890123";
    private static final String VALID_CARD_19 = "1234567890123456789";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 16 位银行卡创建实例")
        void should_create_when_valid16() {
            BankCard card = new BankCard(VALID_CARD);
            assertThat(card.value()).isEqualTo(VALID_CARD);
        }

        @Test
        @DisplayName("合法 13 位银行卡创建实例")
        void should_create_when_valid13() {
            BankCard card = new BankCard(VALID_CARD_13);
            assertThat(card.value()).isEqualTo(VALID_CARD_13);
        }

        @Test
        @DisplayName("合法 19 位银行卡创建实例")
        void should_create_when_valid19() {
            BankCard card = new BankCard(VALID_CARD_19);
            assertThat(card.value()).isEqualTo(VALID_CARD_19);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @ValueSource(strings = {" ", "123", "abc", "123456789012", "12345678901234567890", "6225880123456789a"})
        @DisplayName("非法输入抛出异常")
        void should_throw_when_invalid(String input) {
            assertThatThrownBy(() -> new BankCard(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("脱敏行为")
    class Masking {

        @Test
        @DisplayName("maskedValue 返回脱敏格式：前 6 位 + * + 后 4 位")
        void should_maskCorrectly() {
            BankCard card = new BankCard(VALID_CARD);
            assertThat(card.maskedValue()).isEqualTo("622588******6789");
        }

        @Test
        @DisplayName("13 位卡号脱敏")
        void should_mask13() {
            BankCard card = new BankCard(VALID_CARD_13);
            assertThat(card.maskedValue()).isEqualTo("123456***0123");
        }

        @Test
        @DisplayName("19 位卡号脱敏")
        void should_mask19() {
            BankCard card = new BankCard(VALID_CARD_19);
            assertThat(card.maskedValue()).isEqualTo("123456*********6789");
        }

        @Test
        @DisplayName("toString 输出 Maskable 标准格式")
        void should_toStringMasked() {
            BankCard card = new BankCard(VALID_CARD);
            assertThat(card.toString()).isEqualTo("BankCard[masked=622588******6789]");
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_equal_sameValue() {
            assertThat(new BankCard(VALID_CARD)).isEqualTo(new BankCard(VALID_CARD));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notEqual_diffValue() {
            assertThat(new BankCard(VALID_CARD)).isNotEqualTo(new BankCard("6225880123456788"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_hashCodeConsistent() {
            assertThat(new BankCard(VALID_CARD).hashCode()).isEqualTo(new BankCard(VALID_CARD).hashCode());
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("JSON 序列化输出原值")
        void should_serializeToValue() throws JacksonException {
            String json = MAPPER.writeValueAsString(new BankCard(VALID_CARD));
            assertThat(json).isEqualTo("\"" + VALID_CARD + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromValue() throws JacksonException {
            BankCard card = MAPPER.readValue("\"" + VALID_CARD + "\"", BankCard.class);
            assertThat(card.value()).isEqualTo(VALID_CARD);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(new BankCard(VALID_CARD), BankCard.class);
        }
    }
}