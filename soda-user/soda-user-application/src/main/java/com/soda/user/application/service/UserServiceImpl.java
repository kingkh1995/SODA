package com.soda.user.application.service;

import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RawCredential;
import com.soda.user.api.UserService;
import com.soda.user.api.command.ChangeUsernameCommand;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeleteUserCommand;
import com.soda.user.api.command.UpdatePasswordCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.api.command.UpdateUserStatusCommand;
import com.soda.user.domain.AuthAccount;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.event.PasswordChangedEvent;
import com.soda.user.domain.event.UserRemovedEvent;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.Sex;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserStatus;
import com.soda.user.domain.types.Username;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户聚合根的 ApplicationService 实现 — 编排 {@link UserGateway}、{@link DomainEventBus}、{@link CredentialHasher}。
 * <p>
 * 合并原 6 个 *AppService 的逻辑，实现 {@link UserService} 定义的 6 个方法。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserGateway userGateway;
    private final DomainEventBus domainEventBus;
    private final CredentialHasher credentialHasher;

    public UserServiceImpl(UserGateway userGateway, DomainEventBus domainEventBus,
                           CredentialHasher credentialHasher) {
        this.userGateway = Objects.requireNonNull(userGateway);
        this.domainEventBus = Objects.requireNonNull(domainEventBus);
        this.credentialHasher = Objects.requireNonNull(credentialHasher);
    }

    @Override
    public Long createUser(CreateUserCommand command) {
        Objects.requireNonNull(command);

        // 校验用户名唯一性
        var username = new Username(command.username());
        if (userGateway.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + command.username());
        }

        // 构建 User（不含 Account）
        var user = User.createBuilder()
                .username(username)
                .nickname(new Nickname(command.nickname()))
                .mobile(command.mobile() != null ? new Mobile(command.mobile()) : null)
                .email(command.email() != null ? new Email(command.email()) : null)
                .sex(command.sex() != null ? Sex.of(command.sex()) : null)
                .avatar(command.avatar() != null ? new Avatar(command.avatar()) : null)
                .build();
        // 首次持久化获取 UserId
        var userId = userGateway.save(user);
        Objects.requireNonNull(command.password(), "password must not be null");

        // 创建并添加 PasswordAuthAccount
        var passwordHash = credentialHasher.hash(new RawCredential(command.password()));
        var passwordAccount = PasswordAuthAccount.createBuilder()
                .userId(userId)
                .passwordHash(passwordHash)
                .build();
        user.addAccount(passwordAccount);

        // 再次持久化（含 Account）
        userGateway.save(user);

        // 发布领域事件
        domainEventBus.fireAll(user.flushEvents());

        return userId.value();
    }

    @Override
    public void updateUser(UpdateUserCommand command) {
        Objects.requireNonNull(command);

        var userId = new UserId(command.userId());
        var user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        if (command.nickname() != null) {
            user.setNickname(new Nickname(command.nickname()));
        }
        if (command.mobile() != null) {
            user.setMobile(new Mobile(command.mobile()));
        }
        if (command.email() != null) {
            user.setEmail(new Email(command.email()));
        }
        if (command.sex() != null) {
            user.setSex(Sex.of(command.sex()));
        }
        if (command.avatar() != null) {
            user.setAvatar(new Avatar(command.avatar()));
        }

        userGateway.save(user);
        domainEventBus.fireAll(user.flushEvents());
    }

    @Override
    public void deleteUser(DeleteUserCommand command) {
        Objects.requireNonNull(command);

        var userId = new UserId(command.userId());
        var user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        userGateway.remove(user);
        domainEventBus.fire(new UserRemovedEvent(userId));
    }

    @Override
    public void updateStatus(UpdateUserStatusCommand command) {
        Objects.requireNonNull(command);

        var userId = new UserId(command.userId());
        var user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        user.changeStatus(UserStatus.of(command.status()));

        userGateway.save(user);
        domainEventBus.fireAll(user.flushEvents());
    }

    @Override
    public void changePassword(UpdatePasswordCommand command) {
        Objects.requireNonNull(command);

        var userId = new UserId(command.userId());
        var user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        var passwordAccount = user.findAccount(AuthAccount.ofType(AuthAccountType.P))
                .map(PasswordAuthAccount.class::cast)
                .orElseThrow(() -> new IllegalStateException("Password account not found for user: " + command.userId()));

        passwordAccount.changePassword(new RawCredential(command.newPassword()), credentialHasher);
        domainEventBus.fire(new PasswordChangedEvent(userId));

        userGateway.save(user);
        domainEventBus.fireAll(user.flushEvents());
    }

    @Override
    public void changeUsername(ChangeUsernameCommand command) {
        Objects.requireNonNull(command);

        var userId = new UserId(command.userId());
        var user = userGateway.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + command.userId()));

        var newUsername = new Username(command.newUsername());
        if (userGateway.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Username already exists: " + command.newUsername());
        }

        user.changeUsername(newUsername);

        userGateway.save(user);
        domainEventBus.fireAll(user.flushEvents());
    }
}
