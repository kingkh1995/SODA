package com.soda.user.application.service;

import com.soda.component.application.AbstractAppService;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.PasswordHasher;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.SecretValue;
import com.soda.user.api.UserAuthService;
import com.soda.user.api.command.ChangeEmailCommand;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.ChangePasswordCommand;
import com.soda.user.api.command.RequestChangeEmailCodeCommand;
import com.soda.user.api.command.RequestChangeMobileCodeCommand;
import com.soda.user.application.factory.UserVerificationFactory;
import com.soda.user.domain.User;
import com.soda.user.domain.Verification;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.service.CredentialChangeDomainService;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.VerificationRecipient;
import com.soda.user.domain.types.VerificationState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 用户凭证相关的 ApplicationService 实现 — UCC 发码与消费的编排方
 * （主体为 User，Verification 是协助方聚合，见 ADR-0026）。
 * <p>
 * 发码（{@link #requestChangeMobileCode}/{@link #requestChangeEmailCode}）：前置
 * （加载用户启用态 + 非终态 + target ≠ 当前值 + 目标全局唯一 + 无活跃验证）→
 * {@link UserVerificationFactory} 构造 INITIALIZED 验证聚合 → 同事务惰性 DELETE 腾槽 →
 * save → 发布 {@code VerificationCreatedEvent}（物理发送由投递侧监听器在事务提交后执行，见 ADR-0011）。
 * <p>
 * 消费（verify → change → use）委托 {@link CredentialChangeDomainService}，按 <b>source 键控</b>
 * 反查（{@code VerificationSource.of(UCC, userId)}——命令不含 target，换绑的新联系方式隐含在
 * 验证记录中，「按 source 加载」即主体匹配守卫；source 匹配守卫在 {@code User.changeMobile/changeEmail}
 * 内保留为防御纵深，见 ADR-0026）。
 */
@Slf4j
@Transactional
@Service
public class UserAuthServiceImpl
        extends AbstractAppService<User, UserId, UserGateway>
        implements UserAuthService {

    private final VerificationGateway verificationGateway;
    private final CredentialChangeDomainService credentialChangeService;
    private final DomainEventBus domainEventBus;
    private final PasswordHasher passwordHasher;
    private final UserVerificationFactory userVerificationFactory;

    public UserAuthServiceImpl(UserGateway userGateway,
                               VerificationGateway verificationGateway,
                               CredentialChangeDomainService credentialChangeService,
                               DomainEventBus domainEventBus,
                               PasswordHasher passwordHasher,
                               UserVerificationFactory userVerificationFactory) {
        super(User.class, userGateway);
        this.verificationGateway = verificationGateway;
        this.credentialChangeService = credentialChangeService;
        this.domainEventBus = domainEventBus;
        this.passwordHasher = passwordHasher;
        this.userVerificationFactory = userVerificationFactory;
    }

    // ─── UCC 发码 ───

    @Override
    public void requestChangeMobileCode(RequestChangeMobileCodeCommand command) {
        log.info("requestChangeMobileCode: command={}", command);
        var user = requireEnabled(new UserId(command.userId()));
        var target = Mobile.of(command.newMobile());
        Assert.isTrue(!target.equals(user.getMobile().orElse(null)),
                "Cannot change to the same mobile");
        Assert.isTrue(!gateway.existsByMobile(target),
                "Mobile already exists: " + target.value());
        requestCode(user.getId(), new SmsRecipient(target));
    }

    @Override
    public void requestChangeEmailCode(RequestChangeEmailCodeCommand command) {
        log.info("requestChangeEmailCode: command={}", command);
        var user = requireEnabled(new UserId(command.userId()));
        var target = Email.of(command.newEmail());
        Assert.isTrue(!target.equals(user.getEmail().orElse(null)),
                "Cannot change to the same email");
        Assert.isTrue(!gateway.existsByEmail(target),
                "Email already exists: " + target.value());
        requestCode(user.getId(), new EmailRecipient(target));
    }

    // ─── 凭证变更消费 ───

    @Override
    public void changePassword(ChangePasswordCommand command) {
        log.info("changePassword: userId={}", command.userId());
        var user = requireEnabled(new UserId(command.userId()));
        user.changePassword(new SecretValue(command.oldPassword()),
                new SecretValue(command.newPassword()), passwordHasher);
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
    }

    @Override
    public void changeMobile(ChangeMobileCommand command) {
        log.info("changeMobile: command={}", command);
        var user = requireEnabled(new UserId(command.userId()));
        var pendingVerification = requirePendingCredentialChangeVerification(user.getId());
        credentialChangeService.change(user, pendingVerification, new RandomString(command.code()));
        gateway.save(user);
        verificationGateway.save(pendingVerification);
    }

    @Override
    public void changeEmail(ChangeEmailCommand command) {
        log.info("changeEmail: command={}", command);
        var user = requireEnabled(new UserId(command.userId()));
        var pendingVerification = requirePendingCredentialChangeVerification(user.getId());
        credentialChangeService.change(user, pendingVerification, new RandomString(command.code()));
        gateway.save(user);
        verificationGateway.save(pendingVerification);
    }

    // ─── private helpers ───

    /**
     * 发码公共尾部 — 先构造（source 取自已构造的验证实体，服务层零并行推导）→ 槽位预检
     * （Assert 守卫，existsBy 键存在性契约——活跃/过期判定归基础设施，见 ADR-0025 活跃验证唯一性）→
     * save（惰性 DELETE 过期 I/P 行收敛在 gateway 实现内腾槽，见 ADR-0025）→ 发布创建事件。
     * <p>
     * 并发撞 {@code uk_active_key}：预检 + DB 兜底（唯一索引仲裁）——异常<b>原样上抛</b>，
     * 不翻译（基础设施异常语义即契约，见 ADR-0025 原样上抛）。
     */
    private void requestCode(UserId userId, VerificationRecipient<?> recipient) {
        var verification = userVerificationFactory.newCredentialChangeVerification(userId, recipient);
        Assert.isTrue(!verificationGateway.existsBySource(verification.getSource()),
                "An active verification already exists for source " + verification.getSource().compositeKey());
        verificationGateway.save(verification);
        domainEventBus.publishAll(verification.flushEvents());
    }

    /**
     * 加载用户并断言凭证用例前置（非终态 + 启用态）——发码与消费共用（见 ADR-0026）：
     * 发码不触发聚合变更，启用态无 {@code mustEnable} 兜底；消费路径聚合 {@code mustEnable} 仍兜底，
     * 此处前置提前失败（R 态用户得到「terminal state」而非「must be enabled」的准确消息）。
     * 组合领域守卫（{@code requireNotTerminal} + {@link User#mustEnable()}，单一规则来源）。
     */
    private User requireEnabled(UserId userId) {
        var user = requireNotTerminal(userId);
        user.mustEnable();
        return user;
    }

    /**
     * 断言该用户最新且未过期的 UCC（credential change）PENDING 验证并返回 —
     * changeMobile / changeEmail 的公共查询（方法名即场景，见 ADR-0026）。
     * <p>
     * 按 source 键控加载（{@code credentialChangeSource}——消费命令不含 target，换绑的
     * 新联系方式隐含在验证记录中；source 匹配守卫在 {@code User.changeXxx} 保留为防御纵深）；
     * 未过期过滤为基础设施实现细节。
     */
    private Verification requirePendingCredentialChangeVerification(UserId userId) {
        return verificationGateway
                .findLatestBySourceAndStateIn(
                        userVerificationFactory.newCredentialChangeSource(userId),
                        List.of(VerificationState.P))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pending verification found for user " + userId.value()));
    }
}
