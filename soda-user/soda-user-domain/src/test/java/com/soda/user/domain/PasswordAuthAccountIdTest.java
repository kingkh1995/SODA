package com.soda.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordAuthAccountId 值对象")
class PasswordAuthAccountIdTest {

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from(UserId) 创建实例带 P: 前缀")
        void should_createWithPrefix_when_fromUserId() {
            var id = PasswordAuthAccountId.from(new UserId(42L));
            assertThat(id.value()).isEqualTo("P:42");
            assertThat(id.userId()).isEqualTo(new UserId(42L));
        }

        @Test
        @DisplayName("from 等价于 of")
        void should_beEquivalent_when_fromAndOf() {
            assertThat(PasswordAuthAccountId.from(new UserId(42L)))
                    .isEqualTo(PasswordAuthAccountId.of("P:42"));
        }

        @Test
        @DisplayName("of 正确解析字符串")
        void should_create_when_validString() {
            assertThat(PasswordAuthAccountId.of("P:42").value()).isEqualTo("P:42");
        }

        @Test
        @DisplayName("authAccountType 返回 P")
        void should_returnP_when_authAccountType() {
            assertThat(PasswordAuthAccountId.ACCOUNT_TYPE).isEqualTo(AuthAccountType.P);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("from(null) 抛出异常")
        void should_throw_when_fromNull() {
            assertThatThrownBy(() -> PasswordAuthAccountId.from(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(null) 抛出异常")
        void should_throw_when_ofNull() {
            assertThatThrownBy(() -> PasswordAuthAccountId.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @DisplayName("of 非法字符串抛出异常")
        @ValueSource(strings = {"", "P:", "Q:42", "42"})
        void should_throw_when_invalidString(String invalid) {
            assertThatThrownBy(() -> PasswordAuthAccountId.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(PasswordAuthAccountId.from(new UserId(1L)))
                    .isEqualTo(PasswordAuthAccountId.from(new UserId(1L)));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(PasswordAuthAccountId.from(new UserId(1L)))
                    .isNotEqualTo(PasswordAuthAccountId.from(new UserId(2L)));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode_when_equal() {
            var a = PasswordAuthAccountId.of("P:42");
            var b = PasswordAuthAccountId.of("P:42");
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 包含 value")
        void should_containValue_when_toString() {
            assertThat(PasswordAuthAccountId.of("P:42"))
                    .hasToString("PasswordAuthAccountId[value=P:42]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化反序列化")
        void should_roundTrip_when_validJson() throws Exception {
            var original = PasswordAuthAccountId.from(new UserId(42L));
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).isEqualTo("\"P:42\"");
            var restored = MAPPER.readValue(json, PasswordAuthAccountId.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 抛出异常")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"invalid\"", PasswordAuthAccountId.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 委托给字符串比较")
        void should_delegateToStringCompare_when_compareTo() {
            var a = PasswordAuthAccountId.from(new UserId(1L));
            var b = PasswordAuthAccountId.from(new UserId(2L));
            assertThat(a.compareTo(b) < 0).isTrue();
            assertThat(b.compareTo(a) > 0).isTrue();
            assertThat(a.compareTo(a) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals_when_compareTo() {
            var a = PasswordAuthAccountId.of("P:42");
            var b = PasswordAuthAccountId.of("P:42");
            assertThat(a.compareTo(b) == 0).isTrue();
        }
    }
}
