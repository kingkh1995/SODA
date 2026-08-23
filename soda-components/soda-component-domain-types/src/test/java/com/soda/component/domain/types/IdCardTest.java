package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("身份证号值对象")
class IdCardTest {

    private static final String VALID_ID_CARD = "110101199003071234";
    private static final String VALID_ID_CARD_X = "11010119900307123X";
    private static final String VALID_ID_CARD_x = "11010119900307123x";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 18 位身份证创建实例")
        void should_create_when_valid() {
            IdCard card = new IdCard(VALID_ID_CARD);
            assertThat(card.value()).isEqualTo(VALID_ID_CARD);
        }

        @Test
        @DisplayName("末尾 X 大写归一化")
        void should_uppercaseX() {
            IdCard card = new IdCard(VALID_ID_CARD_x);
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
            assertThatThrownBy(() -> new IdCard(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("脱敏行为")
    class Masking {

        @Test
        @DisplayName("maskedValue 返回脱敏格式：前 6 位 + 8 个 * + 后 4 位")
        void should_maskCorrectly() {
            IdCard card = new IdCard(VALID_ID_CARD);
            assertThat(card.maskedValue()).isEqualTo("110101********1234");
        }

        @Test
        @DisplayName("末尾 X 脱敏")
        void should_maskWithX() {
            IdCard card = new IdCard(VALID_ID_CARD_X);
            assertThat(card.maskedValue()).isEqualTo("110101********123X");
        }

        @Test
        @DisplayName("toString 输出 Maskable 标准格式")
        void should_toStringMasked() {
            IdCard card = new IdCard(VALID_ID_CARD);
            assertThat(card.toString()).isEqualTo("IdCard[masked=110101********1234]");
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同值相等")
        void should_equal_sameValue() {
            assertThat(new IdCard(VALID_ID_CARD)).isEqualTo(new IdCard(VALID_ID_CARD));
        }

        @Test
        @DisplayName("X 大小写归一化后相等")
        void should_equal_caseInsensitiveX() {
            assertThat(new IdCard(VALID_ID_CARD_X)).isEqualTo(new IdCard(VALID_ID_CARD_x));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notEqual_diffValue() {
            assertThat(new IdCard(VALID_ID_CARD)).isNotEqualTo(new IdCard("110101199003071235"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_hashCodeConsistent() {
            assertThat(new IdCard(VALID_ID_CARD).hashCode()).isEqualTo(new IdCard(VALID_ID_CARD).hashCode());
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("JSON 序列化输出原值（大写）")
        void should_serializeToValue() throws JacksonException {
            String json = MAPPER.writeValueAsString(new IdCard(VALID_ID_CARD_x));
            assertThat(json).isEqualTo("\"" + VALID_ID_CARD_X + "\"");
        }

        @Test
        @DisplayName("JSON 反序列化还原实例")
        void should_deserializeFromValue() throws JacksonException {
            IdCard card = MAPPER.readValue("\"" + VALID_ID_CARD + "\"", IdCard.class);
            assertThat(card.value()).isEqualTo(VALID_ID_CARD);
        }

        @Test
        @DisplayName("Jackson 双向验证")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(new IdCard(VALID_ID_CARD), IdCard.class);
        }
    }
}