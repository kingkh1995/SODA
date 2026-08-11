package com.soda.user.application;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.SmsContent;
import com.soda.component.domain.types.UUId;
import com.soda.user.application.event.VerificationCreatedEventHandler;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.Verification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link VerificationCreatedEventHandler} 事务接线测试 — AFTER_COMMIT 语义。
 * <p>
 * 单元测试（直接调用 {@code onVerificationCreated}）无法覆盖的接线由本测试补齐：
 * 用真实的 Spring 监听器发现机制（{@link GenericApplicationContext} +
 * {@link TransactionalEventListenerFactory} 处理 {@code @TransactionalEventListener}）
 * 驱动事件发布，配合 {@link TransactionSynchronizationManager} 模拟事务提交/回滚，验证：
 * <ul>
 *   <li>事务中发布不立即投递，提交后才投递并落库（AFTER_COMMIT）</li>
 *   <li>事务回滚不投递（AFTER_COMMIT 监听器不触发）</li>
 *   <li>无事务上下文时 fallbackExecution 立即执行</li>
 * </ul>
 * 行为契约（发送 → PENDING → 落库、发送失败语义）见 {@link VerificationCreatedEventHandlerTest}。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationCreatedEventHandler 事务接线")
class VerificationCreatedEventHandlerWiringTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final Mobile NEW_MOBILE = new Mobile("13900139000");
    private static final String VALID_CODE = "123456";

    @Mock
    private SmsSender smsSender;
    @Mock
    private EmailSender emailSender;
    @Mock
    private VerificationGateway verificationGateway;

    private GenericApplicationContext context;

    @BeforeEach
    void setUp() {
        var handler = new VerificationCreatedEventHandler(smsSender, emailSender, verificationGateway);
        context = new GenericApplicationContext();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
        context.getBeanFactory()
                .registerSingleton("transactionalEventListenerFactory", new TransactionalEventListenerFactory());
        context.getBeanFactory().registerSingleton("verificationCreatedEventHandler", handler);
        context.refresh();
    }

    @AfterEach
    void tearDown() {
        context.close();
        TransactionSynchronizationManager.clear();
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

    @Test
    @DisplayName("事务提交后投递并落库（AFTER_COMMIT）")
    void should_sendAndPersist_when_transactionCommits() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        context.publishEvent(new VerificationCreatedEvent(initializedSmsVerification()));

        // 提交前不投递（仅注册事务同步，AFTER_COMMIT 前不执行）
        verify(smsSender, never()).send(any(), any());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

        verify(smsSender).send(eq(NEW_MOBILE), any(SmsContent.class));
        verify(verificationGateway).save(any(Verification.class));
    }

    @Test
    @DisplayName("事务回滚不投递（AFTER_COMMIT 监听器不触发）")
    void should_notSend_when_transactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        context.publishEvent(new VerificationCreatedEvent(initializedSmsVerification()));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(smsSender, never()).send(any(), any());
        verify(verificationGateway, never()).save(any());
    }

    @Test
    @DisplayName("无事务上下文时立即执行（fallbackExecution）")
    void should_executeImmediately_when_noTransactionActive() {
        context.publishEvent(new VerificationCreatedEvent(initializedSmsVerification()));

        verify(smsSender).send(eq(NEW_MOBILE), any(SmsContent.class));
        verify(verificationGateway).save(any(Verification.class));
    }
}
