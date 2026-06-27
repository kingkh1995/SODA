package com.soda.user.domain;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Username 值对象")
class UsernameTest {

    private static final String VALID_USERNAME = "testuser";
    private static final String LONG_USERNAME = "a".repeat(31);

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("合法用户名创建实例")
        void should_create_when_validValue() {
            var u = new Username(VALID_USERNAME);
            assertThat(u.value()).isEqualTo(VALID_USERNAME);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  ", "abc", "user name!", "用户名"})
        @DisplayName("null/空/非法字符串拒绝")
        void should_throw_when_invalid(String invalid) {
            assertThatThrownBy(() -> new Username(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("超长拒绝")
        void should_throw_when_tooLong() {
            assertThatThrownBy(() -> new Username(LONG_USERNAME))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new Username("  " + VALID_USERNAME + "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同用户名相等")
        void should_beEqual_when_sameValue() {
            assertThat(new Username(VALID_USERNAME)).isEqualTo(new Username(VALID_USERNAME));
        }

        @Test
        @DisplayName("不同用户名不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(new Username("userA")).isNotEqualTo(new Username("userB"));
        }

        @Test
        @DisplayName("hashCode 一致")
        void should_haveConsistentHashCode() {
            assertThat(new Username("user")).hasSameHashCodeAs(new Username("user"));
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 格式正确")
        void should_formatToString() {
            assertThat(new Username("testuser"))
                    .hasToString("Username[value=testuser]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化与反序列化")
        void should_roundTrip() throws Exception {
            var original = new Username(VALID_USERNAME);
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, Username.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", Username.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 委托字符串比较")
        void should_compareByStringOrder() {
            assertThat(new Username("aaaa").compareTo(new Username("bbbb")) < 0).isTrue();
            assertThat(new Username("bbbb").compareTo(new Username("aaaa")) > 0).isTrue();
            assertThat(new Username("aaaa").compareTo(new Username("aaaa")) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals() {
            var a = new Username("user");
            var b = new Username("user");
            assertThat(a.compareTo(b) == 0).isTrue();
            assertThat(a).isEqualTo(b);
        }
    }
}
