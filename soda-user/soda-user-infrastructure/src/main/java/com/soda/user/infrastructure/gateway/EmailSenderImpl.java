package com.soda.user.infrastructure.gateway;

import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.EmailContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link EmailSender} 的开发期 log 桩实现（2026-08-11 决策）。
 * <p>
 * 契约（ADR-0011）：返回即已确认投递——本实现 log 记录投递目标后返回，
 * 满足「PENDING 蕴含已送达」语义，使端到端链路（发码 → 监听器 → 落 P）可真实运行。
 * 真实通道（JavaMail / 阿里云邮件等）后续替换本实现，类级契约不变。
 * <p>
 * 注意：不记录验证码明文（log 仅含目标与主题，防敏感泄漏）。
 */
@Slf4j
@Component
public class EmailSenderImpl implements EmailSender {

    @Override
    public void send(Email to, EmailContent content) {
        log.info("send: to={}, subject={}", to.value(), content.subject());
    }
}
