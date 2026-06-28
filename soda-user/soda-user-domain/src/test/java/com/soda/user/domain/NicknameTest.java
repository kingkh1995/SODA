package com.soda.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Nickname 值对象")
class NicknameTest {

    private static final String VALID_NICKNAME = "张三";

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
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("null/空拒绝")
        void should_throw_when_nullOrEmpty(String invalid) {
            assertThatThrownBy(() -> new Nickname(invalid))
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

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同昵称相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Nickname(VALID_NICKNAME)).isEqualTo(new Nickname(VALID_NICKNAME));
        }

        @Test
        @DisplayName("不同昵称不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Nickname("A")).isNotEqualTo(new Nickname("B"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new Nickname("nick")).hasSameHashCodeAs(new Nickname("nick"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_formatToString() {
            assertThat(new Nickname("nick"))
                    .hasToString("Nickname[value=nick]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化与反序列化")
        void should_roundTrip() throws Exception {
            var original = new Nickname(VALID_NICKNAME);
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, Nickname.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", Nickname.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 委托字符串比较")
        void should_compareByStringOrder() {
            assertThat(new Nickname("aaaa").compareTo(new Nickname("bbbb")) < 0).isTrue();
            assertThat(new Nickname("bbbb").compareTo(new Nickname("aaaa")) > 0).isTrue();
            assertThat(new Nickname("aaaa").compareTo(new Nickname("aaaa")) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new Nickname("nick");
            var b = new Nickname("nick");
            assertThat(a.compareTo(b) == 0).isTrue();
            assertThat(a).isEqualTo(b);
        }
    }
}
