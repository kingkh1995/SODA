package com.soda.user.domain;

import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.UUId;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证实体测试。
 * <p>
 * 测试场景：
 * <ul>
 *   <li>create -> status=P</li>
 *   <li>verify(correct code) -> status=V</li>
 *   <li>use() after verify -> status=U</li>
 *   <li>verify(wrong code) -> exception</li>
 *   <li>verify expired -> exception</li>
 *   <li>use without verify -> exception</li>
 *   <li>verify used verification -> exception</li>
 * </ul>
 */
@DisplayName("Verification 实体")
class VerificationTest {

    private static final LongId USER_ID = new LongId(1L);
    private static final Mobile MOBILE = new Mobile("13800138000");
    private static final Email EMAIL = new Email("test@example.com");
    private static final VerificationCodePolicy POLICY = new VerificationCodePolicy(6, Duration.ofMinutes(5));
    private static final String VALID_CODE = "123456";

    // 测试用的验证码生成器
    private static final RandomStringGenerator CODE_GENERATOR =
            length -> new RandomString(VALID_CODE);

    // ─── factories ───

    @Nested
    @DisplayName("工厂方法")
    class FactoryTests {

        @Test
        @DisplayName("createBuilder SmsVerification -> status=PENDING")
        void createSmsVerification_pendingStatus() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThat(verification.getScene()).isEqualTo(VerificationScene.LG);
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.P);
            assertThat(verification.getCode().code()).isEqualTo(VALID_CODE);
            assertThat(verification.getTarget()).isEqualTo(MOBILE);
            assertThat(verification.getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("createBuilder EmailVerification -> status=PENDING")
        void createEmailVerification_pendingStatus() {
            var verification = EmailVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.RG)
                    .target(EMAIL)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThat(verification.getScene()).isEqualTo(VerificationScene.RG);
            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.P);
            assertThat(verification.getCode().code()).isEqualTo(VALID_CODE);
            assertThat(verification.getTarget()).isEqualTo(EMAIL);
            assertThat(verification.getUserId()).isEqualTo(USER_ID);
        }
    }

    // ─── verify ───

    @Nested
    @DisplayName("验证码校验")
    class VerifyTests {

        @Test
        @DisplayName("verify(correct code) -> status=VERIFIED")
        void verify_correctCode_statusVerified() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            verification.verify(Instant.now(), new RandomString(VALID_CODE));

            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.V);
            assertThat(verification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("verify(wrong code) -> exception")
        void verify_wrongCode_throwsException() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString("wrong")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid verification code");
        }

        @Test
        @DisplayName("verify expired -> exception")
        void verify_expiredCode_throwsException() {
            var created = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            var verification = SmsVerification.restoreBuilder()
                    .id(created.getId())
                    .scene(VerificationScene.LG)
                    .status(VerificationStatus.P)
                    .code(new VerificationCode(VALID_CODE, Instant.EPOCH))
                    .policy(POLICY)
                    .target(MOBILE)
                    .userId(USER_ID)
                    .build();

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString(VALID_CODE)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("verify already used verification -> exception")
        void verify_alreadyUsed_throwsException() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));
            verification.use();

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString(VALID_CODE)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("verify(null) -> NPE（契约违反，ADR-0015）")
        void verify_nullInput_throwsNullPointerException() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThatThrownBy(() -> verification.verify(Instant.now(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("verify from non-pending state -> exception")
        void verify_fromVerifiedState_throwsException() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));

            assertThatThrownBy(() -> verification.verify(Instant.now(), new RandomString(VALID_CODE)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ─── use ───

    @Nested
    @DisplayName("标记已使用")
    class UseTests {

        @Test
        @DisplayName("use() after verify -> status=USED")
        void use_afterVerify_statusUsed() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));

            verification.use();

            assertThat(verification.getStatus()).isEqualTo(VerificationStatus.U);
        }

        @Test
        @DisplayName("use without verify -> exception")
        void use_withoutVerify_throwsException() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThatThrownBy(() -> verification.use())
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("use already used -> exception")
        void use_alreadyUsed_throwsException() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));
            verification.use();

            assertThatThrownBy(() -> verification.use())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ─── isExpired ───

    @Nested
    @DisplayName("过期判断")
    class ExpiryTests {

        @Test
        @DisplayName("isExpiredAt(now) false for future expiry")
        void isExpired_future_false() {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThat(verification.isExpiredAt(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("isExpiredAt(now) true for past expiry")
        void isExpired_past_true() {
            var created = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            var verification = SmsVerification.restoreBuilder()
                    .id(created.getId())
                    .scene(VerificationScene.LG)
                    .status(VerificationStatus.P)
                    .code(new VerificationCode(VALID_CODE, Instant.EPOCH))
                    .policy(POLICY)
                    .target(MOBILE)
                    .userId(USER_ID)
                    .build();

            assertThat(verification.isExpiredAt(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("isExpiredAt 在 expireAt 精确时刻仍为 false（含端点）")
        void isExpired_atExactExpiry_false() {
            var createdAt = Instant.now();
            var verification = SmsVerification.restoreBuilder()
                    .id(UUId.random())
                    .scene(VerificationScene.LG)
                    .status(VerificationStatus.P)
                    .code(new VerificationCode(VALID_CODE, createdAt))
                    .policy(POLICY)
                    .target(MOBILE)
                    .userId(USER_ID)
                    .build();

            // 过期边界：expireAt 时刻本身仍有效（严格晚于），+1ns 才过期
            assertThat(verification.isExpiredAt(createdAt)).isFalse();
            assertThat(verification.isExpiredAt(createdAt.plusNanos(1))).isTrue();
        }
    }

    // ─── Jackson 序列化 ───

    @Nested
    @DisplayName("Jackson 序列化")
    class JacksonTests {

        @Test
        @DisplayName("多态 round-trip 保留 type 标识与状态")
        void roundTrip_preservesTypeAndState() throws Exception {
            var verification = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString(VALID_CODE));
            verification.use();

            var json = DomainTestUtil.MAPPER.writeValueAsString(verification);
            assertThat(json).contains("\"channel\":\"S\"");

            var restored = DomainTestUtil.MAPPER.readValue(json, Verification.class);
            assertThat(restored).isInstanceOf(SmsVerification.class);
            assertThat(restored).isEqualTo(verification);
            assertThat(restored.getStatus()).isEqualTo(VerificationStatus.U);
        }

        @Test
        @DisplayName("Email 子类型 round-trip")
        void roundTrip_emailSubtype() throws Exception {
            var verification = EmailVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.CC)
                    .target(EMAIL)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();

            var json = DomainTestUtil.MAPPER.writeValueAsString(verification);
            assertThat(json).contains("\"channel\":\"E\"");

            var restored = DomainTestUtil.MAPPER.readValue(json, Verification.class);
            assertThat(restored).isInstanceOf(EmailVerification.class);
            assertThat(restored).isEqualTo(verification);
        }
    }

    // ─── equals & hashCode ───

    @Nested
    @DisplayName("相等性与哈希")
    class EqualityTests {

        @Test
        @DisplayName("same ID -> equal")
        void sameId_equal() {
            var v1 = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            var v2 = SmsVerification.restoreBuilder()
                    .id(v1.getId())
                    .scene(v1.getScene())
                    .status(v1.getStatus())
                    .code(v1.getCode())
                    .policy(v1.getPolicy())
                    .target(MOBILE)
                    .userId(USER_ID)
                    .build();

            assertThat(v2).isEqualTo(v1);
            assertThat(v2.hashCode()).isEqualTo(v1.hashCode());
        }

        @Test
        @DisplayName("different ID -> not equal")
        void differentId_notEqual() {
            var v1 = SmsVerification.createBuilder()
                    .userId(USER_ID)
                    .scene(VerificationScene.LG)
                    .target(MOBILE)
                    .policy(POLICY)
                    .generator(CODE_GENERATOR)
                    .build();
            var v2 = SmsVerification.restoreBuilder()
                    .id(UUId.random())
                    .scene(v1.getScene())
                    .status(v1.getStatus())
                    .code(v1.getCode())
                    .policy(v1.getPolicy())
                    .target(MOBILE)
                    .userId(USER_ID)
                    .build();

            assertThat(v2).isNotEqualTo(v1);
        }
    }
}
