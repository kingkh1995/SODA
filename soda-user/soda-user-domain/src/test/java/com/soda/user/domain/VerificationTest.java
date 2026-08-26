package com.soda.user.domain;

import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.Uuid;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.VerificationChannel;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationRecipient;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证实体测试（source/recipient 双概念形态，见 ADR-0026）。
 * <p>
 * 测试场景：
 * <ul>
 *   <li>create -> state=I，source 恒必填（非空即 Verification 对 source 的唯一要求）、policy 必传</li>
 *   <li>markSent() -> state=P（重复 markSent / 非 I 状态 -> exception）</li>
 *   <li>verify(correct code) -> state=V</li>
 *   <li>use() after verify -> state=U</li>
 *   <li>verify(wrong code) -> exception</li>
 *   <li>verify expired -> exception</li>
 *   <li>use without verify -> exception</li>
 *   <li>verify used verification -> exception</li>
 * </ul>
 */
@DisplayName("Verification 实体")
class VerificationTest {

    private static final VerificationSource UCC_SOURCE = VerificationSource.of("UCC", "1");
    private static final Mobile MOBILE = Mobile.of("13800138000");
    private static final Email EMAIL = Email.of("test@example.com");
    private static final String VALID_CODE = "123456";

    // 测试用的验证码生成器（字符集参数忽略，恒返回固定码）
    private static final RandomStringGenerator CODE_GENERATOR =
            (length, alphabet) -> new RandomString(VALID_CODE);

    private static Verification uccSms() {
        return Verification.createBuilder()
                .source(UCC_SOURCE)
                .recipient(new SmsRecipient(MOBILE))
                .generator(CODE_GENERATOR)
                .policy(VerificationCodePolicy.DEFAULT_SMS)
                .build();
    }

    private static Verification restoredSms(VerificationState state, Instant expireAt) {
        return Verification.builder()
                .id(Uuid.random())
                .source(UCC_SOURCE)
                .state(state)
                .code(new VerificationCode(VALID_CODE, expireAt))
                .recipient(new SmsRecipient(MOBILE))
                .build();
    }

    @Nested
    @DisplayName("工厂方法")
    class FactoryTests {

        @Test
        @DisplayName("createBuilder -> state=INITIALIZED，source/recipient/policy 生效")
        void should_createInitializedState_when_validRequest() {
            var verification = uccSms();

            assertThat(verification.getState()).isEqualTo(VerificationState.I);
            assertThat(verification.isInitialized()).isTrue();
            assertThat(verification.getCode().code()).isEqualTo(VALID_CODE);
            assertThat(verification.getSource()).isEqualTo(UCC_SOURCE);
            assertThat(verification.getRecipient()).isEqualTo(new SmsRecipient(MOBILE));
        }

        @Test
        @DisplayName("source 不透明——聚合原样持有任意场景/主体串（URG 端点值 subject 同构）")
        void should_storeSourceAsIs_when_created() {
            var source = VerificationSource.of("URG", "13800138000");
            var verification = Verification.createBuilder()
                    .source(source)
                    .recipient(new SmsRecipient(MOBILE))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build();

            assertThat(verification.getSource()).isEqualTo(source);
        }

        @Test
        @DisplayName("createBuilder 缺 source -> 拒绝（source 恒必填——非空即唯一要求，见 ADR-0026）")
        void should_throw_when_sourceIsNull() {
            assertThatThrownBy(() -> Verification.createBuilder()
                    .recipient(new SmsRecipient(MOBILE))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("createBuilder 缺 policy -> 拒绝（policy 必传——聚合不做通道→策略映射，见 ADR-0026）")
        void should_throw_when_policyIsNull() {
            assertThatThrownBy(() -> Verification.createBuilder()
                    .source(UCC_SOURCE)
                    .recipient(new SmsRecipient(MOBILE))
                    .generator(CODE_GENERATOR)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be null");
        }

        @Test
        @DisplayName("recipient 多属性 DP：channel/target 属性 + 双参工厂 + JSON 对象往返")
        void should_roundTripRecipients_when_multiAttributeDp() {
            var sms = new SmsRecipient(MOBILE);
            assertThat(sms.channel()).isEqualTo(VerificationChannel.S);
            assertThat(sms.target()).isEqualTo(MOBILE);
            assertThat(new SmsRecipient(Mobile.of(MOBILE.value()))).isEqualTo(sms);
            assertThat(VerificationRecipient.of("S", MOBILE.value())).isEqualTo(sms);

            var smsJson = DomainTestUtil.MAPPER.writeValueAsString(sms);
            var smsTree = DomainTestUtil.MAPPER.readTree(smsJson);
            assertThat(smsTree.get("channel").asString()).isEqualTo("S");
            assertThat(smsTree.get("target").asString()).isEqualTo(MOBILE.value());
            assertThat(DomainTestUtil.MAPPER.readValue(smsJson, VerificationRecipient.class)).isEqualTo(sms);

            var email = new EmailRecipient(EMAIL);
            assertThat(email.channel()).isEqualTo(VerificationChannel.E);
            assertThat(email.target()).isEqualTo(EMAIL);
            assertThat(VerificationRecipient.of("E", EMAIL.value())).isEqualTo(email);

            var emailJson = DomainTestUtil.MAPPER.writeValueAsString(email);
            var emailTree = DomainTestUtil.MAPPER.readTree(emailJson);
            assertThat(emailTree.get("channel").asString()).isEqualTo("E");
            assertThat(emailTree.get("target").asString()).isEqualTo(EMAIL.value());
            assertThat(DomainTestUtil.MAPPER.readValue(emailJson, VerificationRecipient.class)).isEqualTo(email);

            assertThatThrownBy(() -> VerificationRecipient.of("X", "whatever"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown VerificationChannel");
        }
    }

    @Nested
    @DisplayName("JSON 序列化（恢复构造器契约）")
    class SerializationTests {

        @Test
        @DisplayName("往返：序列化 → 反序列化 全字段同步（@JsonCreator 恢复路径唯一入口）")
        void should_roundTrip() {
            var original = restoredSms(VerificationState.P, Instant.parse("2026-08-15T12:00:00Z"));

            var json = DomainTestUtil.MAPPER.writeValueAsString(original);
            var restored = DomainTestUtil.MAPPER.readValue(json, Verification.class);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("缺少 id 的 JSON 拒绝（required=true 双屏障，docs/conventions/framework-type-contracts.md JSON 契约）")
        void should_reject_when_missingId() {
            var original = restoredSms(VerificationState.P, Instant.parse("2026-08-15T12:00:00Z"));
            var tree = DomainTestUtil.MAPPER.readTree(DomainTestUtil.MAPPER.writeValueAsString(original));
            ((tools.jackson.databind.node.ObjectNode) tree).remove("id");
            var json = DomainTestUtil.MAPPER.writeValueAsString(tree);

            assertThatThrownBy(() -> DomainTestUtil.MAPPER.readValue(json, Verification.class))
                    .isInstanceOf(tools.jackson.core.JacksonException.class);
        }
    }

    @Nested
    @DisplayName("标记已发送（I → P）")
    class MarkSentTests {

        @Test
        @DisplayName("markSent -> 状态变为 PENDING")
        void should_markPending_when_markSent() {
            var verification = uccSms();

            verification.markSent();

            assertThat(verification.getState()).isEqualTo(VerificationState.P);
            assertThat(verification.isPending()).isTrue();
            assertThat(verification.isInitialized()).isFalse();
        }

        @Test
        @DisplayName("markSent from non-initialized state -> exception")
        void should_throw_when_markSentFromNonInitialized() {
            var verification = restoredSms(VerificationState.P, Instant.EPOCH);

            assertThatThrownBy(verification::markSent)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("verification must be initialized before sending");
        }

        @Test
        @DisplayName("markSent twice -> exception")
        void should_throw_when_markSentTwice() {
            var verification = uccSms();
            verification.markSent();

            assertThatThrownBy(verification::markSent)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("verification must be initialized before sending");
        }
    }

    @Nested
    @DisplayName("验证码校验")
    class VerifyTests {

        @Test
        @DisplayName("verify(correct code) -> state=VERIFIED")
        void should_markVerified_when_correctCode() {
            var verification = uccSms();
            verification.markSent();

            verification.verify(Instant.now(), new RandomString(VALID_CODE));

            assertThat(verification.getState()).isEqualTo(VerificationState.V);
            assertThat(verification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("verify(wrong code) -> exception")
        void should_throw_when_codeMismatch() {
            var verification = uccSms();
            verification.markSent();

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString("wrong")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid verification code");
        }

        @Test
        @DisplayName("verify expired -> exception")
        void should_throw_when_codeExpired() {
            var verification = restoredSms(VerificationState.P, Instant.EPOCH);

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString(VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("verify already used verification -> exception")
        void should_throw_when_alreadyUsed() {
            var verification = uccSms();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));
            verification.use();

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString(VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("verify from non-pending state -> exception")
        void should_throw_when_alreadyVerified() {
            var verification = uccSms();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString(VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("标记已使用")
    class UseTests {

        @Test
        @DisplayName("use after verify -> state=USED")
        void should_markUsed_when_usedAfterVerify() {
            var verification = uccSms();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));

            verification.use();

            assertThat(verification.getState()).isEqualTo(VerificationState.U);
            assertThat(verification.isVerified()).isFalse();
            assertThat(verification.isPending()).isFalse();
        }

        @Test
        @DisplayName("use without verify -> exception")
        void should_throw_when_useWithoutVerify() {
            var verification = uccSms();
            verification.markSent();

            assertThatThrownBy(verification::use)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("verification must be verified");
        }

        @Test
        @DisplayName("use twice -> exception")
        void should_throw_when_useTwice() {
            var verification = uccSms();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));
            verification.use();

            assertThatThrownBy(verification::use)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
