package com.soda.user.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.soda.component.support.types.Email;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailAuthAccountId 值对象")
class EmailAuthAccountIdTest {

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from(Email) 创建实例带 E: 前缀")
        void should_createWithPrefix_when_fromEmail() {
            var email = new Email("test@example.com");
            var id = EmailAuthAccountId.from(email);
            assertThat(id.value()).isEqualTo("E:test@example.com");
            assertThat(id.email()).isEqualTo(email);
        }

        @Test
        @DisplayName("from 等价于 of")
        void should_beEquivalent_when_fromAndOf() {
            var email = new Email("test@example.com");
            assertThat(EmailAuthAccountId.from(email))
                    .isEqualTo(EmailAuthAccountId.of("E:test@example.com"));
        }

        @Test
        @DisplayName("of 正确解析字符串")
        void should_create_when_validString() {
            assertThat(EmailAuthAccountId.of("E:a@b.com").value()).isEqualTo("E:a@b.com");
        }

        @Test
        @DisplayName("authAccountType 返回 E")
        void should_returnE_when_authAccountType() {
            assertThat(EmailAuthAccountId.ACCOUNT_TYPE).isEqualTo(AuthAccountType.E);
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {
        @Test
        @DisplayName("from(null) 抛出异常")
        void should_throw_when_fromNull() {
            assertThatThrownBy(() -> EmailAuthAccountId.from(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of(null) 抛出异常")
        void should_throw_when_ofNull() {
            assertThatThrownBy(() -> EmailAuthAccountId.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @DisplayName("of 非法字符串抛出异常")
        @ValueSource(strings = {"", "E:", "email:a@b.com", "not-an-email"})
        void should_throw_when_invalidString(String invalid) {
            assertThatThrownBy(() -> EmailAuthAccountId.of(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {
        @Test
        @DisplayName("相同值相等")
        void should_beEqual_when_sameValue() {
            assertThat(EmailAuthAccountId.of("E:a@b.com"))
                    .isEqualTo(EmailAuthAccountId.of("E:a@b.com"));
        }

        @Test
        @DisplayName("不同值不等")
        void should_notBeEqual_when_differentValue() {
            assertThat(EmailAuthAccountId.of("E:a@b.com"))
                    .isNotEqualTo(EmailAuthAccountId.of("E:c@d.com"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode_when_equal() {
            var a = EmailAuthAccountId.of("E:user@test.com");
            var b = EmailAuthAccountId.of("E:user@test.com");
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {
        @Test
        @DisplayName("toString 包含 value")
        void should_containValue_when_toString() {
            assertThat(EmailAuthAccountId.of("E:user@test.com"))
                    .hasToString("EmailAuthAccountId[value=E:user@test.com]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {
        @Test
        @DisplayName("Jackson 序列化反序列化")
        void should_roundTrip_when_validJson() throws Exception {
            var original = EmailAuthAccountId.from(new Email("test@example.com"));
            var json = MAPPER.writeValueAsString(original);
            assertThat(json).isEqualTo("\"E:test@example.com\"");
            var restored = MAPPER.readValue(json, EmailAuthAccountId.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("非法 JSON 抛出异常")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"invalid\"", EmailAuthAccountId.class))
                    .isInstanceOf(JsonProcessingException.class);
        }
    }

    @Nested
    @DisplayName("比较")
    class ComparableTest {
        @Test
        @DisplayName("compareTo 委托给字符串比较")
        void should_delegateToStringCompare_when_compareTo() {
            var a = EmailAuthAccountId.of("E:a@b.com");
            var b = EmailAuthAccountId.of("E:c@d.com");
            assertThat(a.compareTo(b) < 0).isTrue();
            assertThat(b.compareTo(a) > 0).isTrue();
            assertThat(a.compareTo(a) == 0).isTrue();
        }

        @Test
        @DisplayName("compareTo 与 equals 一致")
        void should_beConsistentWithEquals_when_compareTo() {
            var a = EmailAuthAccountId.of("E:user@test.com");
            var b = EmailAuthAccountId.of("E:user@test.com");
            assertThat(a.compareTo(b) == 0).isTrue();
        }
    }
}
