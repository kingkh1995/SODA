package com.soda.user.application.event;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.VerificationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 验证实体创建事件监听器 — 在请求事务提交后投递验证码并落库 PENDING。
 * <p>
 * 职责：验证码的物理发送（外部副作用）。监听 {@link VerificationCreatedEvent}（创建工厂注册，
 * ApplicationService 持久化后发布），经 {@code AFTER_COMMIT} 在事务提交后执行——DB 事务不跨
 * 外部投递通道（SMTP / 短信）持有，且验证记录先于发送持久化（不存在"有码无记录"幽灵码）。
 * <p>
 * 投递保证（见 ADR-0011）：sender 实现契约保证投递成功后才返回（见
 * {@link EmailSender}/{@link SmsSender} 类级契约）——本监听器无重试；仅在 {@code send}
 * 返回后经 {@code markPending} 转 PENDING 并落库（"PENDING 蕴含已送达"不变量）。send 抛异常
 * 视为投递契约违反：异常上抛、记录保持 INITIALIZED（无变更不落库），用户重发请求自愈。
 * <p>
 * 分派：JEP 441 密封类模式匹配（ADR-0016 规则 6）——按子类型选通道 sender，不做枚举分支。
 */
@Component
@RequiredArgsConstructor
public class VerificationCreatedEventHandler {

    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final VerificationGateway verificationGateway;

    /**
     * 投递验证码并落库 PENDING。
     * <p>
     * {@code @TransactionalEventListener(AFTER_COMMIT)}：发布所在事务提交后执行（fallbackExecution
     * 允许无事务上下文时立即执行，如单元测试）；{@code @Transactional} 开启新事务持久化 P 状态。
     *
     * @param event 验证实体创建事件（携带 INITIALIZED 验证实体）
     */
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onVerificationCreated(VerificationCreatedEvent event) {
        var verification = event.verification();
        switch (verification) {
            case SmsVerification sms -> sms.send(smsSender);
            case EmailVerification email -> email.send(emailSender);
        }
        verificationGateway.save(verification);
    }
}
