package com.soda.user.application;

import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.RawCredential;
import com.soda.user.api.command.CreateUserCommand;
import com.soda.user.api.command.DisableUserCommand;
import com.soda.user.api.command.EnableUserCommand;
import com.soda.user.application.convertor.UserDTOConvertor;
import com.soda.user.application.service.UserServiceImpl;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.gateway.UserGateway;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final CredentialHash STUB_HASH = new CredentialHash(
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    @Mock
    private UserGateway userGateway;
    @Mock
    private DomainEventBus domainEventBus;
    @Mock
    private CredentialHasher credentialHasher;
    private UserServiceImpl service;
    private UserDTOConvertor userDTOConvertor;

    private static User createEnabledUser() {
        var passwordAccount = PasswordAuthAccount.restoreBuilder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(com.soda.component.domain.types.Active.TRUE)
                .passwordHash(STUB_HASH)
                .build();
        return User.restoreBuilder()
                .id(USER_ID)
                .username(new Username("testuser"))
                .nickname(new Nickname("Test_User"))
                .state(UserState.E)
                .accounts(List.of(passwordAccount))
                .build();
    }

    @BeforeEach
    void setUp() {
        userDTOConvertor = new UserDTOConvertor();
        service = new UserServiceImpl(userGateway, userDTOConvertor, domainEventBus, credentialHasher);
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

            when(credentialHasher.hash(any(RawCredential.class))).thenReturn(STUB_HASH);
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
                    .isInstanceOf(IllegalArgumentException.class)
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
                    .isInstanceOf(IllegalArgumentException.class)
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
                    .isInstanceOf(IllegalArgumentException.class)
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
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("启用用户")
    class EnableUser {

        @Test
        @DisplayName("启用用户并发布事件")
        void should_enableUserAndFireEvent_when_enable() {
            var user = User.restoreBuilder()
                    .id(USER_ID)
                    .username(new Username("test"))
                    .nickname(new Nickname("Test"))
                    .state(UserState.D)
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
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
