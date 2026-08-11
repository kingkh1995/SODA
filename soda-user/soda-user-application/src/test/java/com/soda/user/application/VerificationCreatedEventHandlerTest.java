package com.soda.user.application;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.EmailContent;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.SmsContent;
import com.soda.component.domain.types.UUId;
import com.soda.user.application.event.VerificationCreatedEventHandler;
import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.Verification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link VerificationCreatedEventHandler} 单元测试 — 验证码投递（AFTER_COMMIT 事件监听）。
 * <p>
 * 契约（见 ADR-0011）：sender 保证投递成功后才返回（无重试）；send 返回后状态转 PENDING
 * 并落库（PENDING 蕴含已送达）；send 抛异常（契约违反）时异常上抛、记录保持 INITIALIZED。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationCreatedEventHandler")
class VerificationCreatedEventHandlerTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final Mobile NEW_MOBILE = new Mobile("13900139000");
    private static final Email NEW_EMAIL = new Email("new@test.com");
    private static final String VALID_CODE = "123456";

    @Mock
    private SmsSender smsSender;
    @Mock
    private EmailSender emailSender;
    @Mock
    private VerificationGateway verificationGateway;

    private VerificationCreatedEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new VerificationCreatedEventHandler(smsSender, emailSender, verificationGateway);
    }

    private static SmsVerification initializedSmsVerification() {
        return SmsVerification.builder()
                .id(UUId.random())
                .userId(USER_ID.toLongId())
                .scene(VerificationScene.CC)
                .status(VerificationStatus.I)
                .target(NEW_MOBILE)
                .code(new VerificationCode(new RandomString(VALID_CODE), Instant.now().plus(Duration.ofMinutes(5))))
                .build();
    }

    private static EmailVerification initializedEmailVerification() {
        return EmailVerification.builder()
                .id(UUId.random())
                .userId(USER_ID.toLongId())
                .scene(VerificationScene.CC)
                .status(VerificationStatus.I)
                .target(NEW_EMAIL)
                .code(new VerificationCode(new RandomString(VALID_CODE), Instant.now().plus(Duration.ofMinutes(30))))
                .build();
    }

    @Test
    @DisplayName("短信验证：发送成功 → 状态转 PENDING 并落库")
    void should_sendSmsAndPersistPending_when_smsVerificationCreated() {
        var verification = initializedSmsVerification();

        handler.onVerificationCreated(new VerificationCreatedEvent(verification));

        verify(smsSender).send(eq(NEW_MOBILE), any(SmsContent.class));
        ArgumentCaptor<Verification<?>> captor = ArgumentCaptor.forClass(Verification.class);
        verify(verificationGateway).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VerificationStatus.P);
    }

    @Test
    @DisplayName("邮件验证：发送成功 → 状态转 PENDING 并落库")
    void should_sendEmailAndPersistPending_when_emailVerificationCreated() {
        var verification = initializedEmailVerification();

        handler.onVerificationCreated(new VerificationCreatedEvent(verification));

        verify(emailSender).send(eq(NEW_EMAIL), any(EmailContent.class));
        ArgumentCaptor<Verification<?>> captor = ArgumentCaptor.forClass(Verification.class);
        verify(verificationGateway).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VerificationStatus.P);
    }

    @Test
    @DisplayName("发送失败（契约违反）：异常上抛，不落库，记录保持 INITIALIZED")
    void should_notPersist_when_sendFails() {
        var verification = initializedSmsVerification();
        doThrow(new IllegalStateException("sms channel down")).when(smsSender).send(eq(NEW_MOBILE), any(SmsContent.class));

        assertThatThrownBy(() -> handler.onVerificationCreated(new VerificationCreatedEvent(verification)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sms channel down");

        verify(verificationGateway, never()).save(any());
        assertThat(verification.getStatus()).isEqualTo(VerificationStatus.I);
    }
}
