package com.soda.user.application.service;

import com.soda.component.api.error.ConflictException;
import com.soda.component.application.AbstractAppService;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.PasswordHasher;
import com.soda.component.domain.types.ConcurrencyVersion;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.SecretValue;
import com.soda.component.domain.types.Sex;
import com.soda.component.domain.types.UpdateMask;
import com.soda.user.api.UserService;
import com.soda.user.api.command.ChangeUsernameCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeregisterUserCommand;
import com.soda.user.api.command.DisableUserCommand;
import com.soda.user.api.command.EnableUserCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.api.dto.UserDTO;
import com.soda.user.application.convertor.UserDTOConvertor;
import com.soda.user.domain.User;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.Username;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * 用户聚合根的 ApplicationService 实现 — 编排 {@link UserGateway}、{@link DomainEventBus}、{@link PasswordHasher}。
 * <p>
 * 凭证/验证码用例由 {@link UserAuthServiceImpl} 承载，本类只管资料与状态机用例。
 */
@Slf4j
@Transactional
@Service
public class UserServiceImpl extends AbstractAppService<User, UserId, UserGateway> implements UserService {

    /**
     * update_mask 允许的字段名（AIP-161 白名单）——与请求体字段名一致，含未知名 → 400 INVALID_ARGUMENT。
     */
    private static final String FIELD_NICKNAME = "nickname";
    private static final String FIELD_SEX = "sex";
    private static final String FIELD_AVATAR = "avatar";
    private static final Set<String> UPDATABLE_FIELDS = Set.of(FIELD_NICKNAME, FIELD_SEX, FIELD_AVATAR);

    private final UserDTOConvertor userDtoConvertor;
    private final PasswordHasher passwordHasher;

    public UserServiceImpl(UserGateway userGateway, UserDTOConvertor userDtoConvertor, DomainEventBus domainEventBus, PasswordHasher passwordHasher) {
        super(User.class, userGateway, domainEventBus);
        this.userDtoConvertor = userDtoConvertor;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserDTO createUser(CreateUserCommand command) {
        log.info("createUser: command={}", command);
        var username = new Username(command.username());
        var mobile = Optional.ofNullable(command.mobile()).map(Mobile::of).orElse(null);
        var email = Optional.ofNullable(command.email()).map(Email::of).orElse(null);
        if (gateway.existsByUsername(username)) {
            throw ConflictException.alreadyExists("Username", command.username());
        }
        if (mobile != null && gateway.existsByMobile(mobile)) {
            throw ConflictException.alreadyExists("Mobile", command.mobile());
        }
        if (email != null && gateway.existsByEmail(email)) {
            throw ConflictException.alreadyExists("Email", command.email());
        }
        var user = User.createBuilder()
                .username(username)
                .nickname(new Nickname(command.nickname()))
                .mobile(mobile)
                .email(email)
                .sex(Optional.ofNullable(command.sex()).map(Sex::of).orElse(null))
                .avatar(Optional.ofNullable(command.avatar()).map(Avatar::new).orElse(null))
                .passwordHash(passwordHasher.hash(new SecretValue(command.password())))
                .build();
        saveAndPublishEvents(user);
        return userDtoConvertor.convert(user);
    }

    @Override
    public UserDTO updateUser(UpdateUserCommand command) {
        log.info("updateUser: command={}", command);
        var user = requireIfMatch(new UserId(command.userId()),
                Optional.ofNullable(command.expectedVersion()).map(ConcurrencyVersion::of).orElse(null));
        // 掩码语态（省略 / 精确覆盖 / 全量替换）归一化与判定单一源在 UpdateMask（AIP-134 §3.5 / AIP-161）。
        // 值合法性归各 DP 构造器（Nickname / Sex / Avatar 自校验抛 IAE → 400），本层不校验 Command 属性。
        var updateMask = UpdateMask.parse(command.updateMask(), UPDATABLE_FIELDS);
        if (updateMask.covers(FIELD_NICKNAME, command.nickname())) {
            user.changeNickname(new Nickname(command.nickname()));
        }
        if (updateMask.covers(FIELD_SEX, command.sex())) {
            user.changeSex(Optional.ofNullable(command.sex()).map(Sex::of).orElse(null));
        }
        if (updateMask.covers(FIELD_AVATAR, command.avatar())) {
            user.changeAvatar(Optional.ofNullable(command.avatar()).map(Avatar::new).orElse(null));
        }
        saveAndPublishEvents(user);
        return userDtoConvertor.convert(user);
    }

    @Override
    public UserDTO deregisterUser(DeregisterUserCommand command) {
        log.info("deregisterUser: command={}", command);
        var user = require(new UserId(command.userId()));
        user.deregister();
        saveAndPublishEvents(user);
        // R 态内存快照：落库三键置 NULL 系网关表示决策（ADR-0023），聚合仍持释放前原值；
        // 语义即"注销前最后一次可用画像 + state=R"，不重读（重读得 REMOVED 占位，无业务意义）。
        return userDtoConvertor.convert(user);
    }

    @Override
    public void disableUser(DisableUserCommand command) {
        log.info("disableUser: command={}", command);
        var user = require(new UserId(command.userId()));
        user.disable();
        saveAndPublishEvents(user);
    }

    @Override
    public void enableUser(EnableUserCommand command) {
        log.info("enableUser: command={}", command);
        var user = require(new UserId(command.userId()));
        user.enable();
        saveAndPublishEvents(user);
    }

    @Override
    public void changeUsername(ChangeUsernameCommand command) {
        log.info("changeUsername: command={}", command);
        var user = require(new UserId(command.userId()));
        var newUsername = new Username(command.newUsername());
        if (gateway.existsByUsername(newUsername)) {
            throw ConflictException.alreadyExists("Username", command.newUsername());
        }
        user.changeUsername(newUsername);
        saveAndPublishEvents(user);
    }
}
