package com.soda.user.application;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.EmailContent;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.SmsContent;
import com.soda.component.domain.types.UUId;
import com.soda.user.application.event.VerificationCreatedEventHandler;
import com.soda.user.domain.Verification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
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
 * 契约（见 ADR-0011/0025）：sender 保证投递成功后才返回（无重试）；分派按投递端点
 * （target 类型——UCC 场景 subject 是用户无通道语义）选择 sender；投递返回后经
 * {@code markSent} 转 PENDING 并落库（PENDING 蕴含已送达）；投递抛异常（契约违反）时
 * 异常上抛、记录保持 INITIALIZED。
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

    private static Verification initializedVerification(Mobile target) {
        return Verification.builder()
                .id(UUId.random())
                .source(VerificationSource.of("UCC", "1"))
                .state(VerificationState.I)
                .recipient(new SmsRecipient(target))
                .code(new VerificationCode(VALID_CODE, Instant.now().plus(Duration.ofMinutes(5))))
                .build();
    }

    private static Verification initializedVerification(Email target) {
        return Verification.builder()
                .id(UUId.random())
                .source(VerificationSource.of("UCC", "1"))
                .state(VerificationState.I)
                .recipient(new EmailRecipient(target))
                .code(new VerificationCode(VALID_CODE, Instant.now().plus(Duration.ofMinutes(30))))
                .build();
    }

    @BeforeEach
    void setUp() {
        handler = new VerificationCreatedEventHandler(smsSender, emailSender, verificationGateway);
    }

    @Test
    @DisplayName("短信验证（UCC，subject=用户，target=手机）：按 target 分派 SmsSender → markSent → PENDING 落库")
    void should_sendSmsAndPersistPending_when_smsVerificationCreated() {
        var verification = initializedVerification(NEW_MOBILE);

        handler.onVerificationCreated(new VerificationCreatedEvent(verification));

        verify(smsSender).send(eq(NEW_MOBILE), any(SmsContent.class));
        ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
        verify(verificationGateway).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(VerificationState.P);
        assertThat(captor.getValue().isPending()).isTrue();
    }

    @Test
    @DisplayName("邮件验证（UCC，subject=用户，target=邮箱）：按 target 分派 EmailSender → PENDING 落库")
    void should_sendEmailAndPersistPending_when_emailVerificationCreated() {
        var verification = initializedVerification(NEW_EMAIL);

        handler.onVerificationCreated(new VerificationCreatedEvent(verification));

        verify(emailSender).send(eq(NEW_EMAIL), any(EmailContent.class));
        ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
        verify(verificationGateway).save(captor.capture());
        assertThat(captor.getValue().getState()).isEqualTo(VerificationState.P);
    }

    @Test
    @DisplayName("发送失败（契约违反）：异常上抛，不落库，记录保持 INITIALIZED")
    void should_notPersist_when_sendFails() {
        var verification = initializedVerification(NEW_MOBILE);
        doThrow(new IllegalStateException("sms channel down"))
                .when(smsSender).send(eq(NEW_MOBILE), any(SmsContent.class));

        assertThatThrownBy(() -> handler.onVerificationCreated(new VerificationCreatedEvent(verification)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sms channel down");

        verify(verificationGateway, never()).save(any());
        assertThat(verification.getState()).isEqualTo(VerificationState.I);
    }
}
