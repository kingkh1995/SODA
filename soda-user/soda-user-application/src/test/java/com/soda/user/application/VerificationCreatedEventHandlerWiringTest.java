package com.soda.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.SmsContent;
import com.soda.component.domain.types.Uuid;
import com.soda.user.application.event.VerificationCreatedEventHandler;
import com.soda.user.domain.Verification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;
import java.time.Instant;

/**
 * {@link VerificationCreatedEventHandler} 事件接线测试 — 普通 {@code @EventListener} 同步语义。
 *
 * <p>使用真实 Spring 事件监听器发现机制验证事件在发布调用栈内立即处理；不绑定事务完成阶段，也不创建独立事务。发送、 {@code markSent} 与保存的领域行为见 {@link
 * VerificationCreatedEventHandlerTest}。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationCreatedEventHandler 同步事件接线")
class VerificationCreatedEventHandlerWiringTest {

    private static final Mobile NEW_MOBILE = Mobile.of("13900139000");
    private static final String VALID_CODE = "123456";

    @Mock private SmsSender smsSender;
    @Mock private EmailSender emailSender;
    @Mock private VerificationGateway verificationGateway;

    private GenericApplicationContext context;

    private static Verification initializedSmsVerification() {
        return Verification.builder()
                .id(Uuid.random())
                .source(new VerificationSource("UCC", "1"))
                .state(VerificationState.I)
                .recipient(new SmsRecipient(NEW_MOBILE))
                .code(new VerificationCode(VALID_CODE, Instant.now().plus(Duration.ofMinutes(5))))
                .build();
    }

    @BeforeEach
    void setUp() {
        var handler =
                new VerificationCreatedEventHandler(smsSender, emailSender, verificationGateway);
        context = new GenericApplicationContext();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
        context.getBeanFactory().registerSingleton("verificationCreatedEventHandler", handler);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("事件发布时同步投递并落库")
    void should_sendAndPersist_when_eventPublished() {
        context.publishEvent(new VerificationCreatedEvent(initializedSmsVerification()));

        verify(smsSender).send(eq(NEW_MOBILE), any(SmsContent.class));
        verify(verificationGateway).save(any(Verification.class));
    }

    @Test
    @DisplayName("发送失败向事件发布方传播且不保存 PENDING")
    void should_propagateFailure_when_senderFails() {
        doThrow(new IllegalStateException("sms channel down"))
                .when(smsSender)
                .send(eq(NEW_MOBILE), any(SmsContent.class));

        assertThatThrownBy(
                        () ->
                                context.publishEvent(
                                        new VerificationCreatedEvent(initializedSmsVerification())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("sms channel down");

        verify(verificationGateway, never()).save(any(Verification.class));
    }
}
