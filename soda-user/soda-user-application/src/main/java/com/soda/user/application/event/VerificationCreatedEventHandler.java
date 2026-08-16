package com.soda.user.application.event;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.EmailContent;
import com.soda.component.domain.types.SmsContent;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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
 * {@link EmailSender}/{@link SmsSender} 类级契约）——本监听器无重试；仅在投递
 * 返回后经 {@code markSent} 转 PENDING 并落库（"PENDING 蕴含已送达"不变量）。投递抛异常
 * 视为投递契约违反：记录保持 INITIALIZED（无变更不落库），用户重发请求自愈。
 * <p>
 * 异常传播（2026-08-11 实测）：Spring 7 的 {@code TransactionSynchronization.afterCompletion}
 * 回调异常被框架吞掉（记录 ERROR 日志，不传播给发布方）——调用方无法感知投递失败，
 * 只能靠记录保持 I 态 + 用户重发自愈（弱保证语义，2026-08-11 范围确认）。
 * <p>
 * 分派（2026-08-16，见 ADR-0026）：按投递端点（recipient 类型）选择通道 sender——
 * {@code SmsRecipient}→SmsSender、{@code EmailRecipient}→EmailSender（<b>channel 只在 VerificationRecipient</b>，
 * 判别源从 subject 迁至 recipient；持久化拆 channel+target 两列，2026-08-16 修订）。单类聚合无子类型分派
 * （2026-08-15 多态塌缩，见 ADR-0025）。
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
     * 允许无事务上下文时立即执行，如单元测试）；{@code REQUIRES_NEW} 开启独立事务持久化 P 状态
     * （Spring 7 限制：AFTER_COMMIT 监听器方法须为 REQUIRES_NEW/NOT_SUPPORTED，且投递落 P 不应
     * 并入发布方事务）。
     *
     * @param event 验证实体创建事件（携带 INITIALIZED 验证实体）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onVerificationCreated(VerificationCreatedEvent event) {
        var verification = event.verification();
        switch (verification.getRecipient()) {
            case SmsRecipient sms -> smsSender.send(sms.target(),
                    new SmsContent(verification.getCode().code()));
            case EmailRecipient email -> emailSender.send(email.target(),
                    new EmailContent("验证码", "您的验证码: " + verification.getCode().code()));
            default -> throw new IllegalStateException(
                    "Unsupported delivery recipient: " + verification.getRecipient());
        }
        verification.markSent();
        verificationGateway.save(verification);
    }
}
