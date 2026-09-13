package com.soda.user.domain.types;

import com.soda.component.domain.testutil.DomainPrimitiveContractTest;
import com.soda.component.domain.types.Mobile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SmsRecipient 短信投递端点")
class SmsRecipientTest extends DomainPrimitiveContractTest<VerificationRecipient> {

    private static final Mobile MOBILE = Mobile.of("13800138000");

    @Override
    protected Contract<VerificationRecipient> contract() {
        return new Contract<>(VerificationRecipient.class, () -> new SmsRecipient(MOBILE),
                "{\"target\":\"13800138000\",\"channel\":\"S\"}", "SmsRecipient[target=" + MOBILE + "]",
                "{}", () -> new SmsRecipient(Mobile.of("13900139000")));
    }

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 Mobile 创建实例")
        void should_create_when_validMobile() {
            var recipient = new SmsRecipient(MOBILE);
            assertThat(recipient.target()).isEqualTo(MOBILE);
        }
    }

    @Nested
    @DisplayName("校验")
    class Validation {

        @Test
        @DisplayName("null Mobile 拒绝")
        void should_throw_when_mobileIsNull() {
            assertThatThrownBy(() -> new SmsRecipient(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("路由")
    class Routing {

        @Test
        @DisplayName("端点工厂按 channel=S 路由到 SmsRecipient")
        void should_routeToSmsRecipient_when_channelS() {
            assertThat(VerificationRecipient.of("S", MOBILE.value())).isEqualTo(new SmsRecipient(MOBILE));
        }

        @Test
        @DisplayName("JSON 以渠道分派路由到 SmsRecipient")
        void should_routeToSmsRecipient_when_channelJson() throws Exception {
            var json = "{\"channel\":\"S\",\"target\":\"" + MOBILE.value() + "\"}";
            var recipient = MAPPER.readValue(json, VerificationRecipient.class);
            assertThat(recipient).isInstanceOf(SmsRecipient.class);
            assertThat(((SmsRecipient) recipient).target()).isEqualTo(MOBILE);
        }

        @Test
        @DisplayName("未知 channel 拒绝")
        void should_throw_when_channelUnknown() {
            assertThatThrownBy(() -> VerificationRecipient.of("X", MOBILE.value()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("业务方法")
    class RichMethods {

        @Test
        @DisplayName("channel() 判别为 S")
        void should_returnChannelS_when_smsRecipient() {
            assertThat(new SmsRecipient(MOBILE).channel()).isEqualTo(VerificationChannel.S);
        }
    }
}
