package com.soda.user.application;

import com.soda.component.domain.DomainEvent;
import com.soda.component.domain.DomainEventBus;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PositiveInt;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.RawCredential;
import com.soda.component.domain.types.UUId;
import com.soda.component.domain.types.Version;
import com.soda.user.api.command.ChangeEmailCommand;
import com.soda.user.api.command.ChangeMobileCommand;
import com.soda.user.api.command.ChangePasswordCommand;
import com.soda.user.api.command.RequestChangeEmailCodeCommand;
import com.soda.user.api.command.RequestChangeMobileCodeCommand;
import com.soda.user.application.service.UserAuthServiceImpl;
import com.soda.user.domain.EmailVerification;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.SmsVerification;
import com.soda.user.domain.User;
import com.soda.user.domain.Verification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.gateway.VerificationGateway.VerificationQuery;
import com.soda.user.domain.service.CredentialChangeDomainService;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserAuthServiceImpl} 单元测试。
 * <p>
 * 验证手机号 / 邮箱变更的两步流程：
 * <ul>
 *   <li>requestChangeXxxCode：查询前置（同值 / 唯一 / 无未过期 PENDING）→ User 创建 INITIALIZED
 *       验证聚合（注册 {@link VerificationCreatedEvent}）→ save → 发布事件（物理发送在
 *       {@code VerificationCreatedEventHandler}，见 ADR-0011，本测试不覆盖）</li>
 *   <li>changeXxx：verify → change → use 编排；失败路径（错码/过期）不落库（实体无变更）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthServiceImpl")
class UserAuthServiceImplTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final Mobile NEW_MOBILE = new Mobile("13900139000");
    private static final Email NEW_EMAIL = new Email("new@test.com");
    private static final String VALID_CODE = "123456";
    private static final CredentialHash STUB_HASH = new CredentialHash(
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    @Mock
    private UserGateway userGateway;
    // CALLS_REAL_METHODS：default 方法 hasUnexpiredPending 真实执行，路由到下方 stub 的抽象查询
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private VerificationGateway verificationGateway;
    @Mock
    private DomainEventBus domainEventBus;
    @Mock
    private CredentialHasher credentialHasher;
    @Mock
    private RandomStringGenerator randomStringGenerator;

    private UserAuthServiceImpl service;

    private static User createUser() {
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(new Username("testuser"))
                .nickname(new Nickname("Test_User"))
                .state(UserState.E)
                .passwordAccount(PasswordAuthAccount.builder()
                        .id(PasswordAuthAccountId.from(USER_ID))
                        .active(Active.TRUE)
                        .passwordHash(STUB_HASH)
                        .build())
                .accounts(List.of())
                .build();
    }

    private static User createUserWithMobile() {
        return createUserWith(NEW_MOBILE, null);
    }

    private static User createUserWithEmail() {
        return createUserWith(null, NEW_EMAIL);
    }

    private static User createUserWith(Mobile mobile, Email email) {
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(new Username("testuser"))
                .nickname(new Nickname("Test_User"))
                .state(UserState.E)
                .mobile(mobile)
                .email(email)
                .passwordAccount(PasswordAuthAccount.builder()
                        .id(PasswordAuthAccountId.from(USER_ID))
                        .active(Active.TRUE)
                        .passwordHash(STUB_HASH)
                        .build())
                .accounts(List.of())
                .build();
    }

    private static User createUserWithPassword() {
        var passwordAccount = PasswordAuthAccount.builder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(com.soda.component.domain.types.Active.TRUE)
                .passwordHash(STUB_HASH)
                .build();
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(new Username("testuser"))
                .nickname(new Nickname("Test_User"))
                .state(UserState.E)
                .passwordAccount(passwordAccount)
                .accounts(List.of())
                .build();
    }

    private static SmsVerification pendingSmsVerification(String code) {
        return SmsVerification.builder()
                .id(UUId.random())
                .userId(USER_ID.toLongId())
                .scene(VerificationScene.CC)
                .status(VerificationStatus.P)
                .target(NEW_MOBILE)
                .code(new VerificationCode(new RandomString(code), Instant.now().plus(Duration.ofMinutes(5))))
                .build();
    }

    private static SmsVerification expiredSmsVerification() {
        return SmsVerification.builder()
                .id(UUId.random())
                .userId(USER_ID.toLongId())
                .scene(VerificationScene.CC)
                .status(VerificationStatus.P)
                .target(NEW_MOBILE)
                .code(new VerificationCode(new RandomString(VALID_CODE), Instant.EPOCH))
                .build();
    }

    private static EmailVerification pendingEmailVerification(String code) {
        return EmailVerification.builder()
                .id(UUId.random())
                .userId(USER_ID.toLongId())
                .scene(VerificationScene.CC)
                .status(VerificationStatus.P)
                .target(NEW_EMAIL)
                .code(new VerificationCode(new RandomString(code), Instant.now().plus(Duration.ofMinutes(30))))
                .build();
    }

    @BeforeEach
    void setUp() {
        service = new UserAuthServiceImpl(userGateway, verificationGateway,
                new CredentialChangeDomainService(),
                randomStringGenerator,
                domainEventBus, credentialHasher);
    }

    @Nested
    @DisplayName("修改密码")
    class ChangePassword {

        @Test
        @DisplayName("成功重哈希并保存用户、发布事件")
        void should_changePasswordAndPublishEvents_when_userExists() {
            var user = createUserWithPassword();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            when(credentialHasher.hash(any(RawCredential.class))).thenReturn(STUB_HASH);

            service.changePassword(new ChangePasswordCommand(1L, "newPassword123"));

            verify(userGateway).save(user);
            verify(domainEventBus).publishAll(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void should_throw_when_userNotFound() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordCommand(1L, "newPassword123")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("发送手机号换绑验证码")
    class RequestChangeMobileCode {

        @Test
        @DisplayName("User 创建 INITIALIZED 验证聚合，save 并发布创建事件（发送由监听器执行）")
        void should_saveInitializedVerificationAndPublishEvent() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));

            service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value()));

            ArgumentCaptor<Verification<?>> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            SmsVerification saved = (SmsVerification) captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(VerificationStatus.I);
            assertThat(saved.getScene()).isEqualTo(VerificationScene.CC);
            assertThat(saved.getTarget()).isEqualTo(NEW_MOBILE);
            assertThat(saved.getCode().code()).isEqualTo(new RandomString(VALID_CODE));
            assertPublishedVerificationCreatedEvent();
        }

        @Test
        @DisplayName("目标与原手机号相同：抛异常，不落库不发布")
        void should_throw_when_sameMobile() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUserWithMobile()));

            assertThatThrownBy(() ->
                    service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cannot change to the same mobile");
            verify(verificationGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }

        @Test
        @DisplayName("目标手机号已被其他用户占用：抛异常，不落库不发布")
        void should_throw_when_mobileAlreadyUsed() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(userGateway.existsByMobile(NEW_MOBILE)).thenReturn(true);

            assertThatThrownBy(() ->
                    service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Mobile already exists: " + NEW_MOBILE.value());
            verify(verificationGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void should_throw_when_userNotFound() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
            verify(verificationGateway, never()).save(any());
        }

        @Test
        @DisplayName("已存在未过期 PENDING 时抛出异常，不重发不落库")
        void should_throw_when_unexpiredPendingExists() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class)))
                    .thenReturn(Optional.of(pendingSmsVerification(VALID_CODE)));

            assertThatThrownBy(() ->
                    service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unexpired pending verification");

            verify(verificationGateway, never()).save(any());
        }

        @Test
        @DisplayName("已存在未过期但目标不同的 PENDING 时同样拒绝（唯一性按 (userId, scene) 判定）")
        void should_throw_when_pendingTargetDiffers() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class)))
                    .thenReturn(Optional.of(pendingSmsVerification(VALID_CODE)));

            assertThatThrownBy(() ->
                    service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, "13800138000")))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(verificationGateway, never()).save(any());
        }

        @Test
        @DisplayName("已存在但已过期的 PENDING 时重新发送（过期由仓库按 validUntil 过滤）")
        void should_resend_when_pendingExpired() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            // 仓库契约：validUntil 时刻已过期的 PENDING 不会被返回
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class)))
                    .thenReturn(Optional.empty());
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));

            service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value()));

            verify(verificationGateway).save(any(Verification.class));
            assertPublishedVerificationCreatedEvent();
        }
    }

    @Nested
    @DisplayName("确认手机号变更")
    class ChangeMobile {

        @Test
        @DisplayName("验证成功：用户保存、验证聚合落库为 USED")
        void should_saveUserAndUsedVerification_when_codeMatches() {
            var user = createUser();
            var pending = pendingSmsVerification(VALID_CODE);
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class), eq(SmsVerification.class)))
                    .thenReturn(Optional.of(pending));

            service.changeMobile(new ChangeMobileCommand(1L, VALID_CODE));

            assertThat(user.getMobile()).hasValue(NEW_MOBILE);
            verify(userGateway).save(user);
            ArgumentCaptor<Verification<?>> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            Verification saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(VerificationStatus.U);
        }

        @Test
        @DisplayName("错码：抛异常且不落库（实体无变更），用户不保存")
        void should_notSaveAnything_when_codeMismatch() {
            var user = createUser();
            var pending = pendingSmsVerification(VALID_CODE);
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class), eq(SmsVerification.class)))
                    .thenReturn(Optional.of(pending));

            assertThatThrownBy(() ->
                    service.changeMobile(new ChangeMobileCommand(1L, "000000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid verification code");

            verify(verificationGateway, never()).save(any());
            verify(userGateway, never()).save(any());
        }

        @Test
        @DisplayName("过期码：抛异常且验证聚合不落库，用户不保存")
        void should_reject_when_codeExpired() {
            var user = createUser();
            var expired = expiredSmsVerification();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class), eq(SmsVerification.class)))
                    .thenReturn(Optional.of(expired));

            assertThatThrownBy(() ->
                    service.changeMobile(new ChangeMobileCommand(1L, VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(verificationGateway, never()).save(any());
            verify(userGateway, never()).save(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void should_throw_when_userNotFound() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.changeMobile(new ChangeMobileCommand(1L, VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
            verify(verificationGateway, never()).save(any());
        }

        @Test
        @DisplayName("无待验证聚合时抛出异常（查询按 CC 场景过滤）")
        void should_throw_when_noPendingVerification() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            ArgumentCaptor<VerificationQuery> queryCaptor = ArgumentCaptor.forClass(VerificationQuery.class);
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), queryCaptor.capture(), eq(SmsVerification.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.changeMobile(new ChangeMobileCommand(1L, VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No pending verification of type SmsVerification");
            assertThat(queryCaptor.getValue().scene()).isEqualTo(VerificationScene.CC);
            verify(userGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("发送邮箱换绑验证码")
    class RequestChangeEmailCode {

        @Test
        @DisplayName("User 创建 INITIALIZED 验证聚合，save 并发布创建事件（发送由监听器执行）")
        void should_saveInitializedVerificationAndPublishEvent() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));

            service.requestChangeEmailCode(new RequestChangeEmailCodeCommand(1L, NEW_EMAIL.value()));

            ArgumentCaptor<Verification<?>> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            EmailVerification saved = (EmailVerification) captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(VerificationStatus.I);
            assertThat(saved.getScene()).isEqualTo(VerificationScene.CC);
            assertThat(saved.getTarget()).isEqualTo(NEW_EMAIL);
            assertPublishedVerificationCreatedEvent();
        }

        @Test
        @DisplayName("目标与原邮箱相同：抛异常，不落库不发布")
        void should_throw_when_sameEmail() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUserWithEmail()));

            assertThatThrownBy(() ->
                    service.requestChangeEmailCode(new RequestChangeEmailCodeCommand(1L, NEW_EMAIL.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("cannot change to the same email");
            verify(verificationGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }

        @Test
        @DisplayName("目标邮箱已被其他用户占用：抛异常，不落库不发布")
        void should_throw_when_emailAlreadyUsed() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(userGateway.existsByEmail(NEW_EMAIL)).thenReturn(true);

            assertThatThrownBy(() ->
                    service.requestChangeEmailCode(new RequestChangeEmailCodeCommand(1L, NEW_EMAIL.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists: " + NEW_EMAIL.value());
            verify(verificationGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }

        @Test
        @DisplayName("已存在未过期 PENDING 时抛出异常，不重发不落库")
        void should_throw_when_unexpiredPendingExists() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class)))
                    .thenReturn(Optional.of(pendingEmailVerification(VALID_CODE)));

            assertThatThrownBy(() ->
                    service.requestChangeEmailCode(new RequestChangeEmailCodeCommand(1L, NEW_EMAIL.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unexpired pending verification");

            verify(verificationGateway, never()).save(any());
        }
    }

    @Nested
    @DisplayName("确认邮箱变更")
    class ChangeEmail {

        @Test
        @DisplayName("验证成功：用户保存、验证聚合落库为 USED")
        void should_saveUserAndUsedVerification_when_codeMatches() {
            var user = createUser();
            var pending = pendingEmailVerification(VALID_CODE);
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class), eq(EmailVerification.class)))
                    .thenReturn(Optional.of(pending));

            service.changeEmail(new ChangeEmailCommand(1L, VALID_CODE));

            assertThat(user.getEmail()).hasValue(NEW_EMAIL);
            verify(userGateway).save(user);
            ArgumentCaptor<Verification<?>> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(VerificationStatus.U);
        }

        @Test
        @DisplayName("错码：抛异常且不落库（实体无变更），用户不保存")
        void should_notSaveAnything_when_codeMismatch() {
            var user = createUser();
            var pending = pendingEmailVerification(VALID_CODE);
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), any(VerificationQuery.class), eq(EmailVerification.class)))
                    .thenReturn(Optional.of(pending));

            assertThatThrownBy(() ->
                    service.changeEmail(new ChangeEmailCommand(1L, "000000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid verification code");

            verify(verificationGateway, never()).save(any());
            verify(userGateway, never()).save(any());
        }

        @Test
        @DisplayName("无待验证聚合时抛出异常（查询按 CC 场景过滤）")
        void should_throw_when_noPendingVerification() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            ArgumentCaptor<VerificationQuery> queryCaptor = ArgumentCaptor.forClass(VerificationQuery.class);
            when(verificationGateway.findLatestByUserId(eq(USER_ID.toLongId()), queryCaptor.capture(), eq(EmailVerification.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.changeEmail(new ChangeEmailCommand(1L, VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No pending verification of type EmailVerification");
            assertThat(queryCaptor.getValue().scene()).isEqualTo(VerificationScene.CC);
            verify(userGateway, never()).save(any());
        }
    }

    /**
     * 断言最近一次发布的事件恰为单个 {@link VerificationCreatedEvent}。
     */
    @SuppressWarnings("unchecked")
    private void assertPublishedVerificationCreatedEvent() {
        ArgumentCaptor<Iterable<DomainEvent<?>>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(domainEventBus).publishAll(captor.capture());
        List<DomainEvent<?>> events = new ArrayList<>();
        captor.getValue().forEach(events::add);
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(VerificationCreatedEvent.class);
    }
}
