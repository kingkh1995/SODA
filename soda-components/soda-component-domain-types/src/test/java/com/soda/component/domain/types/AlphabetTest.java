package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("字符集值对象")
class AlphabetTest extends DomainPrimitiveContractTest<Alphabet> {

    @Override
    protected Contract<Alphabet> contract() {
        return new Contract<>(Alphabet.class, () -> new Alphabet("abc"), "\"abc\"",
                "Alphabet[value=abc]", "{}", () -> new Alphabet("abd"));
    }

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
            assertThat(Alphabet.UNAMBIGUOUS_ALPHANUMERIC.value())
                    .isEqualTo("23456789ABCDEFGHJKLMNPQRSTUVWXYZ");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("null / 空字符串拒绝")
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
    @DisplayName("业务方法")
    class RichMethods {

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
}
