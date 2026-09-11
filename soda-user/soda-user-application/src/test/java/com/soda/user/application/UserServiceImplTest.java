package com.soda.user.application;

import com.soda.component.api.error.ConflictException;
import com.soda.component.api.error.NotFoundException;
import com.soda.component.api.error.PreconditionFailedException;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.PasswordHasher;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.ConcurrencyVersion;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.SecretValue;
import com.soda.component.domain.types.Sex;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DeregisterUserCommand;
import com.soda.user.api.command.DisableUserCommand;
import com.soda.user.api.command.EnableUserCommand;
import com.soda.user.api.command.UpdateUserCommand;
import com.soda.user.application.convertor.UserDTOConvertor;
import com.soda.user.application.service.UserServiceImpl;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.soda.user.domain.types.UserState.D;
import static com.soda.user.domain.types.UserState.E;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final PasswordHash STUB_HASH = PasswordHash.of(
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    @Mock
    private UserGateway userGateway;
    @Mock
    private DomainEventBus domainEventBus;
    @Mock
    private PasswordHasher passwordHasher;
    private UserServiceImpl service;
    private UserDTOConvertor userDTOConvertor;

    private static User createEnabledUser() {
        var passwordAccount = PasswordAuthAccount.builder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(com.soda.component.domain.types.Active.TRUE)
                .passwordHash(STUB_HASH)
                .build();
        return User.builder()
                .id(USER_ID)
                .version(ConcurrencyVersion.of(1))
                .username(new Username("testuser"))
                .nickname(new Nickname("Test_User"))
                .state(UserState.E)
                .passwordAccount(passwordAccount)
                .accounts(List.of())
                .build();
    }

    private static User createUserWithSexAndAvatar(Sex sex, Avatar avatar) {
        var passwordAccount = PasswordAuthAccount.builder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(Active.TRUE)
                .passwordHash(STUB_HASH)
                .build();
        return User.builder()
                .id(USER_ID)
                .version(ConcurrencyVersion.of(1))
                .username(new Username("testuser"))
                .nickname(new Nickname("Test_User"))
                .state(UserState.E)
                .sex(sex)
                .avatar(avatar)
                .passwordAccount(passwordAccount)
                .accounts(List.of())
                .build();
    }

    @BeforeEach
    void setUp() {
        userDTOConvertor = new UserDTOConvertor();
        service = new UserServiceImpl(userGateway, userDTOConvertor, domainEventBus, passwordHasher);
    }

    @Nested
    @DisplayName("创建用户")
    class CreateUser {

        @Test
        @DisplayName("成功创建用户并返回完整用户对象")
        void should_returnUser_when_createSucceeds() {
            var command = new CreateUserCommand(
                    "testuser", "password123", "Test_User",
                    null, null, null, null);

            when(passwordHasher.hash(any(SecretValue.class))).thenReturn(STUB_HASH);
            when(userGateway.save(any(User.class)))
                    .thenAnswer(invocation -> {
                        User user = invocation.getArgument(0);
                        user.assignId(USER_ID);
                        return USER_ID;
                    });

            var result = service.createUser(command);

            assertThat(result.id()).isEqualTo(USER_ID.value());
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.nickname()).isEqualTo("Test_User");
            assertThat(result.mobile()).isNull();
            assertThat(result.email()).isNull();
            assertThat(result.version()).isZero();
            assertThat(result.sex()).isNull();
            assertThat(result.avatar()).isNull();
            assertThat(result.state()).isEqualTo("E");
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("用户名已存在时抛出异常")
        void should_throw_when_usernameExists() {
            var command = new CreateUserCommand(
                    "existing", "password123", "Existing",
                    null, null, null, null);

            when(userGateway.existsByUsername(any())).thenReturn(true);

            assertThatThrownBy(() -> service.createUser(command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName("手机号已存在时抛出异常")
        void should_throw_when_mobileExists() {
            var command = new CreateUserCommand(
                    "testuser", "password123", "Test_User",
                    "13800138000", null, null, null);

            when(userGateway.existsByMobile(any())).thenReturn(true);

            assertThatThrownBy(() -> service.createUser(command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Mobile already exists");
        }

        @Test
        @DisplayName("邮箱已存在时抛出异常")
        void should_throw_when_emailExists() {
            var command = new CreateUserCommand(
                    "testuser", "password123", "Test_User",
                    null, "test@example.com", null, null);

            when(userGateway.existsByEmail(any())).thenReturn(true);

            assertThatThrownBy(() -> service.createUser(command))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email already exists");
        }
    }

    @Nested
    @DisplayName("禁用用户")
    class DisableUser {

        @Test
        @DisplayName("禁用用户并发布事件")
        void should_disableUserAndFireEvent_when_disable() {
            var user = createEnabledUser();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));

            service.disableUser(new DisableUserCommand(USER_ID.value()));

            assertThat(user.getState()).isEqualTo(D);
            verify(domainEventBus).publishAll(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void should_throw_when_userNotFound() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.disableUser(new DisableUserCommand(USER_ID.value())))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("启用用户")
    class EnableUser {

        @Test
        @DisplayName("启用用户并发布事件")
        void should_enableUserAndFireEvent_when_enable() {
            var user = User.builder()
                    .id(USER_ID)
                    .version(ConcurrencyVersion.of(1))
                    .username(new Username("test"))
                    .nickname(new Nickname("Test"))
                    .state(UserState.D)
                    .passwordAccount(PasswordAuthAccount.builder()
                            .id(PasswordAuthAccountId.from(USER_ID))
                            .active(Active.TRUE)
                            .passwordHash(STUB_HASH)
                            .build())
                    .accounts(List.of())
                    .build();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));

            service.enableUser(new EnableUserCommand(USER_ID.value()));

            assertThat(user.getState()).isEqualTo(E);
            verify(domainEventBus).publishAll(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void should_throw_when_userNotFound() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.enableUser(new EnableUserCommand(USER_ID.value())))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("更新用户资料")
    class UpdateUser {

        @Test
        @DisplayName("省略 updateMask → 仅非 null 字段更新（保持 Optional 语义）")
        void should_ignoreNullFields_when_updateMaskOmitted() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), null, "新昵称", null, null, 1);

            var dto = service.updateUser(command);

            assertThat(dto.nickname()).isEqualTo("新昵称");
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("updateMask=\"\" → 视同省略（仍按 Optional 链应用非 null 值）")
        void should_treatBlank_asOmitted() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), "   ", null, "F", null, 1);

            var dto = service.updateUser(command);

            assertThat(dto.sex()).isEqualTo("F");
        }

        @Test
        @DisplayName("updateMask=\"sex\" + 请求 sex=null → 清空 sex（AIP-134 精确覆盖）")
        void should_clearSex_when_updateMaskHitsAndValueIsNull() {
            var initial = createUserWithSexAndAvatar(Sex.F, null);
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(initial));
            var command = new UpdateUserCommand(USER_ID.value(), "sex", null, null, null, 1);

            var dto = service.updateUser(command);

            assertThat(dto.sex()).isNull();
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("updateMask=\"avatar\" + 请求 avatar=null → 清空 avatar（AIP-134 精确覆盖）")
        void should_clearAvatar_when_updateMaskHitsAndValueIsNull() {
            when(userGateway.findById(USER_ID))
                    .thenReturn(Optional.of(createUserWithSexAndAvatar(null,
                            new Avatar("https://example.com/a.png"))));
            var command = new UpdateUserCommand(USER_ID.value(), "avatar", null, null, null, 1);

            var dto = service.updateUser(command);

            assertThat(dto.avatar()).isNull();
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("updateMask=\"nickname,sex\" → DTO 含更新后业务字段（响应即资源本身）")
        void should_returnUpdatedFields_when_updateMaskHits() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), "nickname,sex", "新昵称", "F", null, 1);

            var dto = service.updateUser(command);

            assertThat(dto.nickname()).isEqualTo("新昵称");
            assertThat(dto.sex()).isEqualTo("F");
        }

        @Test
        @DisplayName("updateMask=\"*\" → 全量替换（请求体缺省的可空字段一并清空）")
        void should_replaceAllFields_when_updateMaskWildcard() {
            var initial = createUserWithSexAndAvatar(Sex.F, new Avatar("https://example.com/a.png"));
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(initial));
            var command = new UpdateUserCommand(USER_ID.value(), "*", "新昵称", null, null, 1);

            var dto = service.updateUser(command);

            assertThat(dto.nickname()).isEqualTo("新昵称");
            assertThat(dto.sex()).isNull();
            assertThat(dto.avatar()).isNull();
            verify(userGateway).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("更新用户资料 — update_mask 校验")
    class UpdateUserMaskValidation {

        @Test
        @DisplayName("updateMask 含未知字段 → IllegalArgumentException")
        void should_throw_when_updateMaskUnknownField() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), "password", null, null, null, 1);

            assertThatThrownBy(() -> service.updateUser(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown update_mask fields");
        }

        @Test
        @DisplayName("updateMask=\"nickname\" + 请求 nickname=null → IllegalArgumentException（DP 构造器拦截 →400）")
        void should_throw_when_updateMaskNicknameHitAndValueBlank() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), "nickname", null, null, null, 1);

            assertThatThrownBy(() -> service.updateUser(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("updateMask=\"*\" + 请求 nickname=null → IllegalArgumentException（全量替换下必填字段不可缺）")
        void should_throw_when_updateMaskWildcardAndNicknameBlank() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), "*", null, null, null, 1);

            assertThatThrownBy(() -> service.updateUser(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }

        @Test
        @DisplayName("省略 updateMask + 显式 nickname=\"\" → IllegalArgumentException（不静默忽略客户端显式非法值）")
        void should_throw_when_omittedMaskAndNicknameExplicitlyBlank() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), null, "", null, null, 1);

            assertThatThrownBy(() -> service.updateUser(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not be blank");
        }
    }

    @Nested
    @DisplayName("更新用户资料 — 乐观锁版本守卫")
    class UpdateUserVersionGuard {

        @Test
        @DisplayName("If-Match 缺失（expectedVersion=null）→ 放行（宽松策略，见 ADR-0039）")
        void should_passThrough_when_expectedVersionMissing() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), null, "新昵称", null, null, null);

            var dto = service.updateUser(command);

            assertThat(dto.nickname()).isEqualTo("新昵称");
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("expectedVersion 匹配当前版本 → 放行")
        void should_passThrough_when_expectedVersionMatches() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), null, "新昵称", null, null, 1);

            var dto = service.updateUser(command);

            assertThat(dto.nickname()).isEqualTo("新昵称");
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("expectedVersion 失配 → PreconditionFailedException（译 412，见 ADR-0037 / ADR-0039）")
        void should_throw_when_expectedVersionMismatches() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createEnabledUser()));
            var command = new UpdateUserCommand(USER_ID.value(), null, "新昵称", null, null, 999);

            assertThatThrownBy(() -> service.updateUser(command))
                    .isInstanceOf(PreconditionFailedException.class)
                    .hasMessageContaining("If-Match")
                    .hasMessageContaining("does not match current");
            verify(userGateway, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("删除用户")
    class DeleteUser {

        @Test
        @DisplayName("注销禁用用户：D→R 并保存，返回 R 态内存快照（释放前原键 + state=R）")
        void should_deregisterUserAndPersist_when_disabled() {
            var user = User.builder()
                    .id(USER_ID)
                    .version(ConcurrencyVersion.of(1))
                    .username(new Username("testuser"))
                    .nickname(new Nickname("Test_User"))
                    .state(UserState.D)
                    .passwordAccount(PasswordAuthAccount.builder()
                            .id(PasswordAuthAccountId.from(USER_ID))
                            .active(Active.TRUE)
                            .passwordHash(STUB_HASH)
                            .build())
                    .accounts(List.of())
                    .build();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));

            var dto = service.deregisterUser(new DeregisterUserCommand(USER_ID.value()));

            assertThat(user.getState()).isEqualTo(UserState.R);
            assertThat(dto.state()).isEqualTo("R");
            assertThat(dto.username()).isEqualTo("testuser");
            verify(userGateway).save(user);
            verify(domainEventBus).publishAll(any());
        }

        @Test
        @DisplayName("非禁用状态注销抛 IAE，不保存不发布")
        void should_throw_when_notDisabled() {
            var user = createEnabledUser();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.deregisterUser(new DeregisterUserCommand(USER_ID.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only disabled user can be deregistered");

            verify(userGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void should_throw_when_userNotFound() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.deregisterUser(new DeregisterUserCommand(USER_ID.value())))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("已注销用户再次注销抛 IAE，不保存不发布")
        void should_throw_when_alreadyDeregistered() {
            var user = User.builder()
                    .id(USER_ID)
                    .version(ConcurrencyVersion.of(1))
                    .username(new Username("testuser"))
                    .nickname(new Nickname("Test_User"))
                    .state(UserState.R)
                    .passwordAccount(PasswordAuthAccount.builder()
                            .id(PasswordAuthAccountId.from(USER_ID))
                            .active(Active.TRUE)
                            .passwordHash(STUB_HASH)
                            .build())
                    .accounts(List.of())
                    .build();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.deregisterUser(new DeregisterUserCommand(USER_ID.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only disabled user can be deregistered");

            verify(userGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }
    }
}
