package com.soda.user.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import com.soda.component.domain.types.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EmailRecipient 邮箱投递端点")
class EmailRecipientTest extends DomainPrimitiveContractTest<VerificationRecipient> {

    private static final Email EMAIL = Email.of("user@example.com");

    @Override
    protected Contract<VerificationRecipient> contract() {
        return new Contract<>(VerificationRecipient.class, () -> new EmailRecipient(EMAIL),
                "{\"target\":\"user@example.com\",\"channel\":\"E\"}", "EmailRecipient[target=" + EMAIL + "]",
                "{}", () -> new EmailRecipient(Email.of("other@example.com")));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 Email 创建实例")
        void should_create_when_validEmail() {
            var recipient = new EmailRecipient(EMAIL);
            assertThat(recipient.target()).isEqualTo(EMAIL);
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("null Email 拒绝")
        void should_throw_when_emailIsNull() {
            assertThatThrownBy(() -> new EmailRecipient(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("路由")
    class Routing {

        @Test
        @DisplayName("端点工厂按 channel=E 路由到 EmailRecipient")
        void should_routeToEmailRecipient_when_channelE() {
            assertThat(VerificationRecipient.of("E", EMAIL.value())).isEqualTo(new EmailRecipient(EMAIL));
        }

        @Test
        @DisplayName("JSON 以渠道分派路由到 EmailRecipient")
        void should_routeToEmailRecipient_when_channelJson() throws Exception {
            var json = "{\"channel\":\"E\",\"target\":\"" + EMAIL.value() + "\"}";
            var recipient = MAPPER.readValue(json, VerificationRecipient.class);
            assertThat(recipient).isInstanceOf(EmailRecipient.class);
            assertThat(((EmailRecipient) recipient).target()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("未知 channel 拒绝")
        void should_throw_when_channelUnknown() {
            assertThatThrownBy(() -> VerificationRecipient.of("X", EMAIL.value()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("业务方法")
    class RichMethods {

        @Test
        @DisplayName("channel() 判别为 E")
        void should_returnChannelE_when_emailRecipient() {
            assertThat(new EmailRecipient(EMAIL).channel()).isEqualTo(VerificationChannel.E);
        }
    }
}
