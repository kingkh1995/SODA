package com.soda.user.domain;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.user.domain.types.Nickname;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Nickname 值对象")
class NicknameTest extends ComparableDomainPrimitiveContractTest<Nickname> {

    private static final String VALID_NICKNAME = "张三";

    @Override
    protected Contract<Nickname> contract() {
        return new Contract<>(Nickname.class, () -> new Nickname("nick"), "\"nick\"",
                "Nickname[value=nick]", "{}", () -> new Nickname("zack"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法昵称创建实例")
        void should_create_when_validValue() {
            var n = new Nickname(VALID_NICKNAME);
            assertThat(n.value()).isEqualTo(VALID_NICKNAME);
        }

        @Test
        @DisplayName("接受 Unicode 字符")
        void should_create_when_unicode() {
            var n = new Nickname("张三_test-123");
            assertThat(n.value()).isEqualTo("张三_test-123");
        }

        @Test
        @DisplayName("接受下划线和连字符")
        void should_create_when_underscoreAndHyphen() {
            var n = new Nickname("hello_world-test");
            assertThat(n.value()).isEqualTo("hello_world-test");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("null / 空 / 空白拒绝")
        void should_throw_when_nullOrEmpty() {
            assertThatThrownBy(() -> new Nickname(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Nickname(""))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Nickname("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"hello world", "sp ace", "tab\t", "new\nline", "line\rbreak", "lead ", " trail"})
        @DisplayName("含空白字符拒绝")
        void should_throw_when_whitespace(String whitespace) {
            assertThatThrownBy(() -> new Nickname(whitespace))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超长拒绝")
        void should_throw_when_tooLong() {
            assertThatThrownBy(() -> new Nickname("a".repeat(31)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
