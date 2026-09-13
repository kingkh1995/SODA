package com.soda.component.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("邮箱值对象")
class EmailTest extends DomainPrimitiveContractTest<Email> {

    @Override
    protected Contract<Email> contract() {
        return new Contract<>(Email.class, () -> Email.of("t@t.com"), "\"t@t.com\"",
                "Email[masked=t***@t.com]", "\"invalid-email\"", () -> Email.of("u@t.com"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法邮箱创建实例")
        void should_create_when_validEmail() {
            var email = Email.of("user@example.com");
            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("大写归一化为小写")
        void should_normalizeToLowercase() {
            var email = Email.of("USER@Example.COM");
            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("特殊字符邮箱接受")
        void should_accept_when_validSpecialChars() {
            var email = Email.of("user.name+tag@example.co.uk");
            assertThat(email.value()).isEqualTo("user.name+tag@example.co.uk");
        }
    }

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> Email.of(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "[{index}] \"{0}\"")
        @ValueSource(strings = {"  ", "  user@example.com  ", "userexample.com", "user@", "user@example"})
        @DisplayName("非法格式邮箱拒绝（空白 / 无 @ / 无域名 / 无顶级域名）")
        void should_throw_when_invalidFormat(String value) {
            assertThatThrownBy(() -> Email.of(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("派生字段")
    class DerivedFields {

        @Test
        @DisplayName("localPart() 返回 @ 前本地部分")
        void should_returnLocalPart_when_validEmail() {
            var email = Email.of("alice@example.com");
            assertThat(email.localPart()).isEqualTo("alice");
        }

        @Test
        @DisplayName("localPart() 包含点号")
        void should_returnLocalPart_when_withDots() {
            var email = Email.of("alice.smith@example.com");
            assertThat(email.localPart()).isEqualTo("alice.smith");
        }

        @Test
        @DisplayName("domain() 返回 @ 后域名")
        void should_returnDomain_when_validEmail() {
            var email = Email.of("alice@example.com");
            assertThat(email.domain()).isEqualTo("example.com");
        }

        @Test
        @DisplayName("domain() 包含子域名")
        void should_returnDomain_when_subdomain() {
            var email = Email.of("alice@mail.example.co.uk");
            assertThat(email.domain()).isEqualTo("mail.example.co.uk");
        }
    }
}
