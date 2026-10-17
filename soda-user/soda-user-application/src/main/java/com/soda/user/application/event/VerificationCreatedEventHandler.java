package com.soda.user.application.event;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.EmailContent;
import com.soda.component.domain.types.SmsContent;
import com.soda.user.domain.Verification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 验证创建事件监听器 — 在发布方事务内同步投递验证码并落库 PENDING。
 *
 * <p>职责：验证码的物理发送（外部副作用）。监听 {@link VerificationCreatedEvent}，由创建工厂注册、ApplicationService
 * 在事务内持久化后发布；本监听器通过普通 {@code @EventListener} 同步消费事件中的 {@link Verification} 聚合。
 *
 * <p>发布方负责数据库事务；发送与状态落库加入同一事务。外部发送与数据库提交不具备跨资源原子性：发送成功后数据库仍可能提交失败，形成可接受的双写窗口；发送方异常继续向发布方传播并触发事务回滚（见
 * ADR-0044）。
 *
 * <p>分派（见 ADR-0026）：按投递端点（recipient 类型）选择通道 sender—— {@code SmsRecipient}→SmsSender、{@code
 * EmailRecipient}→EmailSender（<b>channel 只在 VerificationRecipient</b>；持久化拆 channel+target 两列）。
 */
@Component
@RequiredArgsConstructor
public class VerificationCreatedEventHandler {

    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final VerificationGateway verificationGateway;

    /**
     * 投递验证码并落库 PENDING。
     *
     * <p>普通 {@code @EventListener} 在发布方事务中同步执行；handler 消费事件携带的 INITIALIZED 聚合，发送成功后执行 {@code
     * markSent} 并保存。
     *
     * @param event 验证实体创建事件（携带 INITIALIZED 验证实体）
     */
    @EventListener
    @Transactional
    public void onVerificationCreated(VerificationCreatedEvent event) {
        var verification = event.verification();
        switch (verification.getRecipient()) {
            case SmsRecipient sms ->
                    smsSender.send(sms.target(), new SmsContent(verification.getCode().code()));
            case EmailRecipient email ->
                    emailSender.send(
                            email.target(),
                            new EmailContent("验证码", "您的验证码: " + verification.getCode().code()));
        }
        verification.markSent();
        verificationGateway.save(verification);
    }
}
