package com.soda.component.domain.types;

import com.soda.component.domain.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("字符集值对象")
class AlphabetTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法字符集创建实例")
        void should_create_when_validValue() {
            var alphabet = new Alphabet("AbCd123");
            assertThat(alphabet.value()).isEqualTo("AbCd123");
        }

        @Test
        @DisplayName("任意非空白字符集接受（开集）")
        void should_create_when_anyCustomCharset() {
            var alphabet = new Alphabet("ABC123!@#");
            assertThat(alphabet.value()).isEqualTo("ABC123!@#");
        }

        @Test
        @DisplayName("常量字符集正确")
        void should_haveCorrectConstants() {
            assertThat(Alphabet.DIGITS.value()).isEqualTo("0123456789");
            assertThat(Alphabet.LETTERS.value())
                    .isEqualTo("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
            assertThat(Alphabet.ALPHANUMERIC.value())
                    .isEqualTo(Alphabet.DIGITS.value() + Alphabet.LETTERS.value());
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null / 空白拒绝")
        void should_throw_when_blank(String invalid) {
            assertThatThrownBy(() -> new Alphabet(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("重复字符拒绝（隐式加权）")
        void should_throw_when_duplicateCharacters() {
            assertThatThrownBy(() -> new Alphabet("1123"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("单字符拒绝（熵为 0）")
        void should_throw_when_singleCharacter() {
            assertThatThrownBy(() -> new Alphabet("1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("索引与大小")
    class Indexing {

        @Test
        @DisplayName("size 返回字符集长度")
        void should_returnSize() {
            assertThat(Alphabet.DIGITS.size()).isEqualTo(10);
        }

        @Test
        @DisplayName("charAt 返回对应索引字符")
        void should_returnCharAt_when_validIndex() {
            assertThat(Alphabet.DIGITS.charAt(0)).isEqualTo('0');
            assertThat(Alphabet.DIGITS.charAt(9)).isEqualTo('9');
        }

        @Test
        @DisplayName("charAt 负索引拒绝")
        void should_throw_when_negativeIndex() {
            assertThatThrownBy(() -> Alphabet.DIGITS.charAt(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("charAt 越界索引拒绝")
        void should_throw_when_indexOutOfBounds() {
            assertThatThrownBy(() -> Alphabet.DIGITS.charAt(10))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同字符集相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Alphabet("abc")).isEqualTo(new Alphabet("abc"));
        }

        @Test
        @DisplayName("不同字符集不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Alphabet("abc")).isNotEqualTo(new Alphabet("abd"));
        }

        @Test
        @DisplayName("字符顺序不同不等（索引语义随顺序）")
        void should_notBeEqual_when_differentOrder() {
            assertThat(new Alphabet("abc")).isNotEqualTo(new Alphabet("cba"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            assertThat(new Alphabet("abc")).hasSameHashCodeAs(new Alphabet("abc"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new Alphabet("abc")).hasToString("Alphabet[value=abc]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致（裸字符串）")
        void should_roundTrip() throws Exception {
            JacksonTestUtil.assertRoundTrip(new Alphabet("Abc123"), Alphabet.class);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeToBareString() throws Exception {
            var json = MAPPER.writeValueAsString(new Alphabet("Abc123"));
            assertThat(json).isEqualTo("\"Abc123\"");
        }

        @Test
        @DisplayName("从裸字符串反序列化")
        void should_deserializeFromBareString() throws Exception {
            assertThat(MAPPER.readValue("\"Abc123\"", Alphabet.class))
                    .isEqualTo(new Alphabet("Abc123"));
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", Alphabet.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
