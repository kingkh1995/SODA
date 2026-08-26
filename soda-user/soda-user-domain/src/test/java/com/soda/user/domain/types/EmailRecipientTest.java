package com.soda.user.domain.types;

import com.soda.component.domain.types.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailRecipient 邮箱投递端点")
class EmailRecipientTest {

    private static final Email EMAIL = Email.of("user@example.com");

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 Email 创建实例")
        void should_create_when_validEmail() {
            var recipient = new EmailRecipient(EMAIL);
            assertThat(recipient.target()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("null Email 拒绝")
        void should_throw_when_emailIsNull() {
            assertThatThrownBy(() -> new EmailRecipient(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("通道")
    class Channel {

        @Test
        @DisplayName("channel 恒为 E")
        void should_returnE_when_channel() {
            assertThat(new EmailRecipient(EMAIL).channel()).isEqualTo(VerificationChannel.E);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同 Email 相等")
        void should_beEqual_when_sameEmail() {
            assertThat(new EmailRecipient(EMAIL)).isEqualTo(new EmailRecipient(EMAIL));
        }

        @Test
        @DisplayName("不同 Email 不等")
        void should_notBeEqual_when_differentEmail() {
            assertThat(new EmailRecipient(EMAIL))
                    .isNotEqualTo(new EmailRecipient(Email.of("other@example.com")));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new EmailRecipient(EMAIL);
            var b = new EmailRecipient(EMAIL);
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = new EmailRecipient(EMAIL);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, VerificationRecipient.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("反序列化按 channel 路由到 EmailRecipient")
        void should_routeToEmailRecipient_when_channelE() throws Exception {
            var json = "{\"channel\":\"E\",\"target\":\"" + EMAIL.value() + "\"}";
            var recipient = MAPPER.readValue(json, VerificationRecipient.class);
            assertThat(recipient).isInstanceOf(EmailRecipient.class);
            assertThat(((EmailRecipient) recipient).target()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("非法 JSON 拒绝")
        void should_throw_when_invalidJson() {
            assertThatThrownBy(() -> MAPPER.readValue("{}", VerificationRecipient.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 格式正确")
        void should_haveCorrectToString() {
            assertThat(new EmailRecipient(EMAIL)).hasToString("EmailRecipient[target=" + EMAIL + "]");
        }
    }
}
