package com.soda.user.application.service;

import com.soda.component.application.AbstractAppService;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.EmailContent;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.RawCredential;
import com.soda.component.domain.types.SmsContent;
import com.soda.component.domain.types.VerificationChannel;
import com.soda.user.api.UserAuthService;
import com.soda.user.api.command.ChangeEmailCommand;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.ChangePasswordCommand;
import com.soda.user.api.command.VerifyEmailCommand;
import com.soda.user.api.command.VerifyMobileCommand;
import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.User;
import com.soda.user.domain.Verification;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.gateway.VerificationGateway.VerificationQuery;
import com.soda.user.domain.service.CredentialChangeDomainService;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 用户凭证相关的 ApplicationService 实现。
 * <p>
 * 仅做用例编排：加载主体聚合（{@code User}）→ 执行用例流程 → 持久化（先存 user 后存 verification）。
 * 验证码发起（生成码、构造 PENDING 验证聚合、发送）只涉及单聚合创建，在本类内直接展开；
 * 跨聚合的消费流程（verify → change → use）委托 {@link CredentialChangeDomainService}
 * （见 framework-conventions「ApplicationService 编排规范」）。
 */
@Slf4j
@Service
public class UserAuthServiceImpl extends AbstractAppService<User, UserId, UserGateway> implements UserAuthService {

    private final VerificationGateway verificationGateway;
    private final CredentialChangeDomainService credentialChangeService;
    private final RandomStringGenerator randomStringGenerator;
    private final SmsSender smsSender;
    private final EmailSender emailSender;
    private final DomainEventBus domainEventBus;
    private final CredentialHasher credentialHasher;

    public UserAuthServiceImpl(UserGateway userGateway, VerificationGateway verificationGateway,
                               CredentialChangeDomainService credentialChangeService,
                               RandomStringGenerator randomStringGenerator,
                               SmsSender smsSender, EmailSender emailSender,
                               DomainEventBus domainEventBus, CredentialHasher credentialHasher) {
        super(User.class, userGateway);
        this.verificationGateway = verificationGateway;
        this.credentialChangeService = credentialChangeService;
        this.randomStringGenerator = randomStringGenerator;
        this.smsSender = smsSender;
        this.emailSender = emailSender;
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
    public void verifyMobile(VerifyMobileCommand command) {
        log.info("verifyMobile: command={}", command);
        var userId = new UserId(command.userId());
        require(userId);
        var target = new Mobile(command.newMobile());
        if (verificationGateway.hasUnexpiredPending(userId.toLongId(), VerificationScene.CC)) {
            throw new IllegalArgumentException("An unexpired pending verification already exists");
        }
        var verification = SmsVerification.createBuilder()
                .userId(userId.toLongId())
                .scene(VerificationScene.CC)
                .target(target)
                .policy(VerificationCodePolicy.DEFAULT_SMS)
                .generator(randomStringGenerator)
                .build();
        smsSender.send(target, new SmsContent("您的验证码: " + verification.getCode().code()));
        verificationGateway.save(verification);
    }

    @Override
    public void changeMobile(ChangeMobileCommand command) {
        log.info("changeMobile: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        var pendingVerification = SmsVerification.class.cast(requireLatestUnexpiredPending(userId, VerificationChannel.S));
        credentialChangeService.changeMobile(user, pendingVerification, new RandomString(command.code()));
        gateway.save(user);
        verificationGateway.save(pendingVerification);
    }

    @Override
    public void verifyEmail(VerifyEmailCommand command) {
        log.info("verifyEmail: command={}", command);
        var userId = new UserId(command.userId());
        require(userId);
        var target = new Email(command.newEmail());
        if (verificationGateway.hasUnexpiredPending(userId.toLongId(), VerificationScene.CC)) {
            throw new IllegalArgumentException("An unexpired pending verification already exists");
        }
        var verification = EmailVerification.createBuilder()
                .userId(userId.toLongId())
                .scene(VerificationScene.CC)
                .target(target)
                .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                .generator(randomStringGenerator)
                .build();
        emailSender.send(target, new EmailContent("验证码", "您的验证码: " + verification.getCode().code()));
        verificationGateway.save(verification);
    }

    @Override
    public void changeEmail(ChangeEmailCommand command) {
        log.info("changeEmail: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        var pendingVerification = EmailVerification.class.cast(requireLatestUnexpiredPending(userId, VerificationChannel.E));
        credentialChangeService.changeEmail(user, pendingVerification, new RandomString(command.code()));
        gateway.save(user);
        verificationGateway.save(pendingVerification);
    }

    /**
     * 断言用户存在最新且未过期的 PENDING 验证并返回 — changeMobile / changeEmail 的公共查询。
     * <p>
     * 基于 {@code findLatestByUserId} 的便捷封装：场景固定为 {@link VerificationScene#CC}，
     * {@code status} 固定为 {@link VerificationStatus#P}，判定时刻固定为 {@link Instant#now()}；
     * 渠道过滤由 {@code channel} 承担（channel 与实体类型一一对应，调用方按渠道强转收敛类型）。
     * 「必须存在」语义：无匹配时抛 {@link IllegalArgumentException}（属应用层校验职责，
     * 对齐 {@link AbstractAppService#require}）。
     *
     * @param userId  用户 ID
     * @param channel 验证渠道
     * @return 匹配的最新且未过期的 PENDING 验证实体
     * @throws IllegalArgumentException 无匹配验证时
     */
    private Verification<?> requireLatestUnexpiredPending(UserId userId, VerificationChannel channel) {
        return verificationGateway.findLatestByUserId(userId.toLongId(),
                        new VerificationQuery(VerificationScene.CC, VerificationStatus.P, channel, Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("No pending " + channel.desc() + " verification"));
    }
}
