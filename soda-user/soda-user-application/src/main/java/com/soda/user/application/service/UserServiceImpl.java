package com.soda.user.application.service;

import com.soda.component.application.AbstractAppService;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RawCredential;
import com.soda.component.domain.types.Sex;
import com.soda.user.api.UserService;
import com.soda.user.api.command.ChangeUsernameCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeleteUserCommand;
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
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * 用户聚合根的 ApplicationService 实现 — 编排 {@link UserGateway}、{@link DomainEventBus}、{@link CredentialHasher}。
 * <p>
 * 凭证/验证码相关操作迁至 {@link UserAuthServiceImpl}。
 */
@Slf4j
@Transactional
@Service
public class UserServiceImpl extends AbstractAppService<User, UserId, UserGateway> implements UserService {

    private final UserDTOConvertor userConvertor;
    private final DomainEventBus domainEventBus;
    private final CredentialHasher credentialHasher;

    public UserServiceImpl(UserGateway userGateway, UserDTOConvertor userConvertor,
                           DomainEventBus domainEventBus, CredentialHasher credentialHasher) {
        super(User.class, userGateway);
        this.userConvertor = userConvertor;
        this.domainEventBus = domainEventBus;
        this.credentialHasher = credentialHasher;
    }

    @Override
    public UserDTO createUser(CreateUserCommand command) {
        log.info("createUser: command={}", command);
        var username = new Username(command.username());
        var mobile = Optional.ofNullable(command.mobile()).map(Mobile::new).orElse(null);
        var email = Optional.ofNullable(command.email()).map(Email::new).orElse(null);
        Assert.isTrue(!gateway.existsByUsername(username),
                "Username already exists: " + command.username());
        if (mobile != null) {
            Assert.isTrue(!gateway.existsByMobile(mobile),
                    "Mobile already exists: " + command.mobile());
        }
        if (email != null) {
            Assert.isTrue(!gateway.existsByEmail(email),
                    "Email already exists: " + command.email());
        }
        var user = User.createBuilder()
                .username(username)
                .nickname(new Nickname(command.nickname()))
                .mobile(mobile)
                .email(email)
                .sex(Optional.ofNullable(command.sex()).map(Sex::of).orElse(null))
                .avatar(Optional.ofNullable(command.avatar()).map(Avatar::new).orElse(null))
                .passwordHash(credentialHasher.hash(new RawCredential(command.password())))
                .build();
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
        return userConvertor.convert(user);
    }

    @Override
    public void updateUser(UpdateUserCommand command) {
        log.info("updateUser: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        Optional.ofNullable(command.nickname()).map(Nickname::new).ifPresent(user::changeNickname);
        Optional.ofNullable(command.sex()).map(Sex::of).ifPresent(user::changeSex);
        Optional.ofNullable(command.avatar()).map(Avatar::new).ifPresent(user::changeAvatar);
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
    }

    @Override
    public void deleteUser(DeleteUserCommand command) {
        log.info("deleteUser: command={}", command);
        var user = require(new UserId(command.userId()));
        user.deregister();
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
    }

    @Override
    public void disableUser(DisableUserCommand command) {
        log.info("disableUser: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        user.disable();
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
    }

    @Override
    public void enableUser(EnableUserCommand command) {
        log.info("enableUser: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        user.enable();
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
    }

    @Override
    public void changeUsername(ChangeUsernameCommand command) {
        log.info("changeUsername: command={}", command);
        var userId = new UserId(command.userId());
        var user = require(userId);
        var newUsername = new Username(command.newUsername());
        Assert.isTrue(!gateway.existsByUsername(newUsername),
                "Username already exists: " + command.newUsername());
        user.changeUsername(newUsername);
        gateway.save(user);
        domainEventBus.publishAll(user.flushEvents());
    }
}
