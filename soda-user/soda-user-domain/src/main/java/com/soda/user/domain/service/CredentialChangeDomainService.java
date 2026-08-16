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
 * AppService 前置/拦截，见 ADR-0026）。
 * <p>
 * 职责（仅承载跨聚合编排——「同时更改多个领域」的用例流程才进领域服务）：
 * <ul>
 *   <li>验证码的发起（前置 + 生成码、构造 INITIALIZED 验证聚合、注册
 *       {@code VerificationCreatedEvent}）由 {@code UserAuthService.requestChangeMobileCode/
 *       requestChangeEmailCode} 编排（应用层前置 + {@code UserVerificationFactory} 创建，
 *       2026-08-16 回归 User 侧，见 ADR-0026）；物理发送由投递侧监听器在事务提交后执行
 *       （见 ADR-0011）</li>
 *   <li>验证码的消费（{@link #change(User, Verification, RandomString)}）：verify →
 *       换绑（recipient 类型分派）→ use——换绑目标（手机号/邮箱）由验证投递端点决定：
 *       {@code SmsRecipient} → {@link User#changeMobile(Verification)}、
 *       {@code EmailRecipient} → {@link User#changeEmail(Verification)}；
 *       User 方法内保留 recipient 类型校验（防御纵深）</li>
 *   <li>变更完成后验证聚合标记 USED，不可再次消费</li>
 * </ul>
 *
 * @see User#changeMobile(Verification)
 * @see User#changeEmail(Verification)
 */
@Service
public final class CredentialChangeDomainService implements DomainService {

    /**
     * 消费验证码完成换绑（2026-08-16 会话修订——原 changeMobile/changeEmail 两方法统一为单方法，
     * 分派键 = 验证投递端点类型）：verify → recipient 分派（{@code SmsRecipient} →
     * {@code user.changeMobile}、{@code EmailRecipient} → {@code user.changeEmail}）→ use。
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
