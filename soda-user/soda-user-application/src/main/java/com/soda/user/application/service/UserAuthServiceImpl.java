package com.soda.user.application.service;

import com.soda.component.application.AbstractAppService;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.RawCredential;
import com.soda.user.api.UserAuthService;
import com.soda.user.api.command.ChangeEmailCommand;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.ChangePasswordCommand;
import com.soda.user.api.command.RequestChangeEmailCodeCommand;
import com.soda.user.api.command.RequestChangeMobileCodeCommand;
import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.User;
import com.soda.user.domain.Verification;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.gateway.VerificationGateway.VerificationQuery;
import com.soda.user.domain.service.CredentialChangeDomainService;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * 用户凭证相关的 ApplicationService 实现。
 * <p>
 * 仅做用例编排：加载主体聚合（{@code User}）→ 执行用例流程 → 持久化。
 * 发码用例（{@link #requestChangeMobileCode}/{@link #requestChangeEmailCode}）：
 * 查询前置（目标唯一、无未过期 PENDING）→ 委托 {@code User.requestChangeXxxCode}
 * （User 自检规则 + 创建 INITIALIZED 验证聚合并注册 {@code VerificationCreatedEvent}）
 * → save 验证聚合 → 发布事件；物理发送由投递侧监听器在事务提交后执行（见 ADR-0011）。
 * 跨聚合的消费流程（verify → change → use）委托 {@link CredentialChangeDomainService}
 * （见 framework-conventions「ApplicationService 编排规范」）。
 */
@Slf4j
@Transactional
@Service
public class UserAuthServiceImpl extends AbstractAppService<User, UserId, UserGateway> implements UserAuthService {

    private final VerificationGateway verificationGateway;
    private final CredentialChangeDomainService credentialChangeService;
    private final RandomStringGenerator randomStringGenerator;
    private final DomainEventBus domainEventBus;
    private final CredentialHasher credentialHasher;

    public UserAuthServiceImpl(UserGateway userGateway, VerificationGateway verificationGateway,
                               CredentialChangeDomainService credentialChangeService,
                               RandomStringGenerator randomStringGenerator,
                               DomainEventBus domainEventBus, CredentialHasher credentialHasher) {
        super(User.class, userGateway);
        this.verificationGateway = verificationGateway;
        this.credentialChangeService = credentialChangeService;
        this.randomStringGenerator = randomStringGenerator;
        this.domainEventBus = domainEventBus;
        this.credentialHasher = credentialHasher;
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        log.info("changePassword: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        user.changePassword(new RawCredential(command.newPassword()), credentialHasher);
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
    }

    @Override
    public void requestChangeMobileCode(RequestChangeMobileCodeCommand command) {
        log.info("requestChangeMobileCode: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        var target = new Mobile(command.newMobile());
        Assert.isTrue(!target.equals(user.getMobile().orElse(null)), "cannot change to the same mobile");
        Assert.isTrue(!gateway.existsByMobile(target), "Mobile already exists: " + command.newMobile());
        if (verificationGateway.hasUnexpiredPending(userId.toLongId(), VerificationScene.CC)) {
            throw new IllegalArgumentException("An unexpired pending verification already exists");
        }
        var verification = user.requestChangeMobileCode(target, randomStringGenerator);
        verificationGateway.save(verification);
        domainEventBus.publishAll(verification.flushEvents());
    }

    @Override
    public void changeMobile(ChangeMobileCommand command) {
        log.info("changeMobile: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        var pendingVerification = requireLatestUnexpiredPending(userId, VerificationScene.CC, SmsVerification.class);
        credentialChangeService.changeMobile(user, pendingVerification, new RandomString(command.code()));
        gateway.save(user);
        verificationGateway.save(pendingVerification);
    }

    @Override
    public void requestChangeEmailCode(RequestChangeEmailCodeCommand command) {
        log.info("requestChangeEmailCode: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        var target = new Email(command.newEmail());
        Assert.isTrue(!target.equals(user.getEmail().orElse(null)), "cannot change to the same email");
        Assert.isTrue(!gateway.existsByEmail(target), "Email already exists: " + command.newEmail());
        if (verificationGateway.hasUnexpiredPending(userId.toLongId(), VerificationScene.CC)) {
            throw new IllegalArgumentException("An unexpired pending verification already exists");
        }
        var verification = user.requestChangeEmailCode(target, randomStringGenerator);
        verificationGateway.save(verification);
        domainEventBus.publishAll(verification.flushEvents());
    }

    @Override
    public void changeEmail(ChangeEmailCommand command) {
        log.info("changeEmail: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        var pendingVerification = requireLatestUnexpiredPending(userId, VerificationScene.CC, EmailVerification.class);
        credentialChangeService.changeEmail(user, pendingVerification, new RandomString(command.code()));
        gateway.save(user);
        verificationGateway.save(pendingVerification);
    }

    /**
     * 断言用户存在最新且未过期的 PENDING 验证并返回 — changeMobile / changeEmail 的公共查询。
     * <p>
     * 基于 {@code findLatestByUserId} 的便捷封装：场景由参数指定（换绑固定传
     * {@link VerificationScene#CC}），{@code status} 固定为 {@link VerificationStatus#P}，
     * 判定时刻固定为 {@link Instant#now()}；
     * 类型收敛变体（ADR-0016）：判别值由 gateway 实现按 {@code type} 推导，
     * 调用方零反查、零强转。
     * 「必须存在」语义：无匹配时抛 {@link IllegalArgumentException}（属应用层校验职责，
     * 对齐 {@link AbstractAppService#require}）。
     *
     * @param userId 用户 ID
     * @param scene  验证场景（过滤条件，换绑为 {@link VerificationScene#CC}）
     * @param type   验证实体类型（{@link SmsVerification} / {@link EmailVerification}）
     * @return 匹配的最新且未过期的 PENDING 验证实体
     * @throws IllegalArgumentException 无匹配验证时
     */
    private <T extends Verification<?>> T requireLatestUnexpiredPending(UserId userId, VerificationScene scene, Class<T> type) {
        return verificationGateway.findLatestByUserId(userId.toLongId(),
                        new VerificationQuery(scene, VerificationStatus.P, Instant.now()), type)
                .orElseThrow(() -> new IllegalArgumentException("No pending verification of type " + type.getSimpleName()));
    }
}
