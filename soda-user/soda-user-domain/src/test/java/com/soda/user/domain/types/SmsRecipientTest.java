package com.soda.user.domain.types;

import com.soda.component.domain.types.Mobile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SmsRecipient 短信投递端点")
class SmsRecipientTest {

    private static final Mobile MOBILE = Mobile.of("13800138000");

    @Nested
    @DisplayName("构造")
    class Constructor {

        @Test
        @DisplayName("合法 Mobile 创建实例")
        void should_create_when_validMobile() {
            var recipient = new SmsRecipient(MOBILE);
            assertThat(recipient.target()).isEqualTo(MOBILE);
        }

        @Test
        @DisplayName("null Mobile 拒绝")
        void should_throw_when_mobileIsNull() {
            assertThatThrownBy(() -> new SmsRecipient(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("通道")
    class Channel {

        @Test
        @DisplayName("channel 恒为 S")
        void should_returnS_when_channel() {
            assertThat(new SmsRecipient(MOBILE).channel()).isEqualTo(VerificationChannel.S);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同 Mobile 相等")
        void should_beEqual_when_sameMobile() {
            assertThat(new SmsRecipient(MOBILE)).isEqualTo(new SmsRecipient(MOBILE));
        }

        @Test
        @DisplayName("不同 Mobile 不等")
        void should_notBeEqual_when_differentMobile() {
            assertThat(new SmsRecipient(MOBILE))
                    .isNotEqualTo(new SmsRecipient(Mobile.of("13900139000")));
        }

        @Test
        @DisplayName("hashCode 与 equals 一致")
        void should_haveConsistentHashCode() {
            var a = new SmsRecipient(MOBILE);
            var b = new SmsRecipient(MOBILE);
            assertThat(a).hasSameHashCodeAs(b);
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip 一致")
        void should_roundTrip() throws Exception {
            var original = new SmsRecipient(MOBILE);
            var json = MAPPER.writeValueAsString(original);
            assertThat(MAPPER.readValue(json, VerificationRecipient.class)).isEqualTo(original);
        }

        @Test
        @DisplayName("反序列化按 channel 路由到 SmsRecipient")
        void should_routeToSmsRecipient_when_channelS() throws Exception {
            var json = "{\"channel\":\"S\",\"target\":\"" + MOBILE.value() + "\"}";
            var recipient = MAPPER.readValue(json, VerificationRecipient.class);
            assertThat(recipient).isInstanceOf(SmsRecipient.class);
            assertThat(((SmsRecipient) recipient).target()).isEqualTo(MOBILE);
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
            assertThat(new SmsRecipient(MOBILE)).hasToString("SmsRecipient[target=" + MOBILE + "]");
        }
    }
}
