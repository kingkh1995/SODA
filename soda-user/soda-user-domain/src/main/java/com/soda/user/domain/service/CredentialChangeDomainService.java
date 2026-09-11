package com.soda.user.domain.service;

import com.soda.component.domain.DomainService;
import com.soda.component.domain.types.RandomString;
import com.soda.user.domain.User;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 凭证变更领域服务 — 跨聚合编排（同时更改 User 与验证聚合）。
 * <p>
 * 无字段无 gateway 依赖（{@code DomainService} 约定——纯内存编排；跨实例查询由
 * AppService 前置拦截，见 ADR-0026）。
 * <p>
 * 发码不在本服务：由 {@code UserAuthService.requestChangeMobile/requestChangeEmail}
 * 编排（应用层前置 + {@code UserVerificationFactory} 构造 INITIALIZED 验证），物理发送由
 * 投递侧监听器在事务提交后执行（见 ADR-0011/0026）。本服务只承载消费编排：
 * <ul>
 *   <li>{@link #change(User, Verification, RandomString)}：verify → recipient 分派换绑 → use</li>
 *   <li>完成后验证聚合标记 USED，不可再次消费</li>
 * </ul>
 *
 * @see User#changeMobile(Verification)
 * @see User#changeEmail(Verification)
 */
@Service
public final class CredentialChangeDomainService implements DomainService {

    /**
     * 消费验证码完成换绑——分派键 = 验证投递端点类型：verify → recipient 分派
     * （{@code SmsRecipient} → {@code user.changeMobile}、{@code EmailRecipient} →
     * {@code user.changeEmail}，User 方法内保留 recipient 类型校验作防御纵深）→ use。
     * 持久化（先存 user 后存 verification）由 ApplicationService 执行。
     */
    public void change(User user, Verification verification, RandomString code) {
        verification.verify(Instant.now(), code);
        switch (verification.getRecipient()) {
            case SmsRecipient _ -> user.changeMobile(verification);
            case EmailRecipient _ -> user.changeEmail(verification);
        }
        verification.use();
    }
}
