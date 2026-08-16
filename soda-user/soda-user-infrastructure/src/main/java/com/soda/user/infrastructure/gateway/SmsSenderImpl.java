package com.soda.user.infrastructure.gateway;

import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.SmsContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link SmsSender} 的开发期 log 桩实现（2026-08-11 决策）。
 * <p>
 * 契约（ADR-0011）：返回即已确认投递——本实现 log 记录投递目标后返回，
 * 满足「PENDING 蕴含已送达」语义，使端到端链路（发码 → 监听器 → 落 P）可真实运行。
 * 真实通道（阿里云 SMS 等）后续替换本实现，类级契约不变。
 * <p>
 * 注意：不记录验证码明文（log 仅含目标与长度，防敏感泄漏）。
 */
@Slf4j
@Component
public class SmsSenderImpl implements SmsSender {

    @Override
    public void send(Mobile to, SmsContent content) {
        log.info("SMS deliver (log stub): to={}, contentLength={}", to.value(), content.value().length());
    }
}
