package com.soda.component.support.types;

import com.soda.component.support.testutil.JacksonTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("邮箱值对象")
class EmailTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("localPart() 返回 @ 前本地部分")
    void should_returnLocalPart_when_validEmail() {
        var email = new Email("alice@example.com");
        assertThat(email.localPart()).isEqualTo("alice");
    }

    @Test
    @DisplayName("localPart() 包含点号")
    void should_returnLocalPart_when_withDots() {
        var email = new Email("alice.smith@example.com");
        assertThat(email.localPart()).isEqualTo("alice.smith");
    }

    @Test
    @DisplayName("domain() 返回 @ 后域名")
    void should_returnDomain_when_validEmail() {
        var email = new Email("alice@example.com");
        assertThat(email.domain()).isEqualTo("example.com");
    }

    @Test
    @DisplayName("domain() 包含子域名")
    void should_returnDomain_when_subdomain() {
        var email = new Email("alice@mail.example.co.uk");
        assertThat(email.domain()).isEqualTo("mail.example.co.uk");
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法邮箱创建实例")
        void should_create_when_validEmail() {
            var email = new Email("user@example.com");
            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("大写归一化为小写")
        void should_normalizeToLowercase() {
            var email = new Email("USER@Example.COM");
            assertThat(email.value()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("特殊字符邮箱接受")
        void should_accept_when_validSpecialChars() {
            var email = new Email("user.name+tag@example.co.uk");
            assertThat(email.value()).isEqualTo("user.name+tag@example.co.uk");
        }
    }

    // ——— 业务方法 ———

    @Nested
    @DisplayName("校验与异常")
    class Validation {

        @Test
        @DisplayName("null 拒绝")
        void should_throw_when_valueIsNull() {
            assertThatThrownBy(() -> new Email(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("空白字符串拒绝")
        void should_throw_when_valueIsBlank() {
            assertThatThrownBy(() -> new Email("  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("首尾空白拒绝")
        void should_throw_when_whitespaceAround() {
            assertThatThrownBy(() -> new Email("  user@example.com  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("缺少 @ 符号拒绝")
        void should_throw_when_noAtSymbol() {
            assertThatThrownBy(() -> new Email("userexample.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("缺少域名拒绝")
        void should_throw_when_noDomain() {
            assertThatThrownBy(() -> new Email("user@"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("缺少顶级域名拒绝")
        void should_throw_when_noTld() {
            assertThatThrownBy(() -> new Email("user@example"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("相等性与 hashCode")
    class Equality {

        @Test
        @DisplayName("相同地址相等（忽略大小写）")
        void should_beEqual_when_sameAddress() {
            assertThat(new Email("a@b.com")).isEqualTo(new Email("A@B.com"));
        }

        @Test
        @DisplayName("不同地址不等")
        void should_notBeEqual_when_differentAddress() {
            assertThat(new Email("a@b.com")).isNotEqualTo(new Email("a@c.com"));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new Email("a@b.com");
            var b = new Email("a@b.com");
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new Email("t@t.com")).hasToString("Email[value=t@t.com]");
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson 序列化反序列化一致")
        void should_roundTrip() throws Exception {
            var original = new Email("jackson@test.org");
            JacksonTestUtil.assertRoundTrip(original, Email.class);
        }

        @Test
        @DisplayName("序列化为裸字符串")
        void should_serializeAsBareString() throws Exception {
            var json = MAPPER.writeValueAsString(new Email("bare@test.com"));
            assertThat(json).isEqualTo("\"bare@test.com\"");
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("\"invalid-email\"", Email.class))
                    .isInstanceOf(JacksonException.class);
        }
    }
}
