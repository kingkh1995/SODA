package com.soda.user.domain.service;

import com.soda.component.domain.DomainService;
import com.soda.component.domain.types.RandomString;
import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.User;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 跨聚合领域服务 — 凭证（手机号 / 邮箱）变更的「消费验证码」流程。
 * <p>
 * 属于领域层，协调 {@link User} 与 {@link SmsVerification}/{@link EmailVerification}
 * 两个实体的动作序列（verify → change → use）。按项目 DDD 规范
 * （framework-conventions「DomainService」条目）：
 * <ul>
 *   <li>仅承载跨聚合编排——「同时更改多个领域」的用例流程才进领域服务；
 *       验证码的发起（生成码、构造 INITIALIZED 验证聚合、经聚合 {@code send(sender)} 发送）只涉及单聚合创建，
 *       由 ApplicationService 直接执行（见 {@code UserAuthServiceImpl.verifyMobile}/{@code verifyEmail}）</li>
 *   <li><b>禁止持久化</b>——save 一律由 ApplicationService 执行（本类不持有任何 Repository/Gateway 的写端口）</li>
 *   <li><b>不建议查询加载</b>——聚合以参数注入，加载留在 ApplicationService</li>
 * </ul>
 * <p>
 * 规则（与 ADR-0011 一致）：
 * <ul>
 *   <li>change：验证码必须待验证且未过期（{@code verify} 内校验）；验证通过才变更凭证
 *       （{@code User.changeXxx} 内校验目标一致性）；变更完成后验证聚合标记 USED，不可再次消费</li>
 * </ul>
 *
 * @see User#changeMobile(SmsVerification)
 * @see User#changeEmail(EmailVerification)
 */
@Service
public final class CredentialChangeDomainService implements DomainService {

    /**
     * 消费短信验证码完成手机号换绑：verify → {@code user.changeMobile} → use。
     * 持久化（先存 user 后存 verification）由 ApplicationService 执行。
     */
    public void changeMobile(User user, SmsVerification verification, RandomString code) {
        verification.verify(Instant.now(), code);
        user.changeMobile(verification);
        verification.use();
    }

    /**
     * 消费邮箱验证码完成邮箱换绑：verify → {@code user.changeEmail} → use。
     * 持久化（先存 user 后存 verification）由 ApplicationService 执行。
     */
    public void changeEmail(User user, EmailVerification verification, RandomString code) {
        verification.verify(Instant.now(), code);
        user.changeEmail(verification);
        verification.use();
    }
}
