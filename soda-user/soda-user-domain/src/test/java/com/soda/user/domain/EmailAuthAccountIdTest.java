package com.soda.user.domain;

import com.soda.component.domain.testutil.ComparableDomainPrimitiveContractTest;
import com.soda.component.domain.types.Email;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.EmailAuthAccountId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailAuthAccountId 值对象")
class EmailAuthAccountIdTest extends ComparableDomainPrimitiveContractTest<EmailAuthAccountId> {

    @Override
    protected Contract<EmailAuthAccountId> contract() {
        return new Contract<>(EmailAuthAccountId.class, () -> EmailAuthAccountId.of("E:user@test.com"),
                "\"E:user@test.com\"", "EmailAuthAccountId[value=E:user@test.com]", "\"invalid\"",
                () -> EmailAuthAccountId.of("E:zser@test.com"));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {
        @Test
        @DisplayName("from(Email) 创建实例带 E: 前缀")
        void should_createWithPrefix_when_fromEmail() {
            var email = Email.of("test@example.com");
            var id = EmailAuthAccountId.from(email);
            assertThat(id.value()).isEqualTo("E:test@example.com");
            assertThat(id.email()).isEqualTo(email);
        }

        @Test
        @DisplayName("from 等价于 of")
        void should_beEquivalent_when_fromAndOf() {
            var email = Email.of("test@example.com");
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
            assertThat(EmailAuthAccountId.of("E:a@b.com").accountType()).isEqualTo(AuthAccountType.E);
        }
    }

    @Nested
    @DisplayName("路由")
    class Routing {
        @Test
        @DisplayName("JSON 反序列化以本子类为声明类型")
        void should_routeToEmailSubtype_when_json() throws Exception {
            assertThat(MAPPER.readValue("\"E:a@b.com\"", EmailAuthAccountId.class))
                    .isEqualTo(EmailAuthAccountId.of("E:a@b.com"));
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
    @DisplayName("标识符")
    class Identity {
        @Test
        @DisplayName("identifier 返回规范化编码值")
        void should_returnCanonicalValue_when_identifier() {
            assertThat(EmailAuthAccountId.of("E:user@test.com").identifier()).isEqualTo("E:user@test.com");
        }
    }
}
