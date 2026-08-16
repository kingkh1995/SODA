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
import com.soda.user.application.factory.UserVerificationFactory;
import com.soda.user.application.service.UserAuthServiceImpl;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.Verification;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.gateway.UserGateway;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.service.CredentialChangeDomainService;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link UserAuthServiceImpl} 单元测试（2026-08-16，见 ADR-0026——发码用例回归 User 侧，
 * {@code VerificationServiceImplTest} 并入本类）。
 * <p>
 * 发码用例（requestChangeMobileCode / requestChangeEmailCode）：前置（加载用户启用态 + 非终态
 * + target ≠ 当前值 + 目标全局唯一 + 无活跃验证）→ {@link UserVerificationFactory} 构造
 * INITIALIZED 验证聚合 → save → 发布 {@link VerificationCreatedEvent}。
 * <p>
 * 消费用例（changePassword / changeMobile / changeEmail）：加载 User + 按 source 键控加载
 * 待验证聚合（findLatestBySourceAndStateIn）→ verify → change → use（{@link CredentialChangeDomainService}）
 * → 同事务双 save；失败路径（错码/过期）不落库（实体无变更）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAuthServiceImpl")
class UserAuthServiceImplTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final VerificationSource UCC_SOURCE = VerificationSource.of("UCC", "1");
    private static final Mobile NEW_MOBILE = new Mobile("13900139000");
    private static final Email NEW_EMAIL = new Email("new@test.com");
    private static final String VALID_CODE = "123456";
    private static final CredentialHash STUB_HASH = new CredentialHash(
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    @Mock
    private UserGateway userGateway;
    @Mock
    private VerificationGateway verificationGateway;
    @Mock
    private DomainEventBus domainEventBus;
    @Mock
    private CredentialHasher credentialHasher;
    @Mock
    private RandomStringGenerator randomStringGenerator;

    private UserAuthServiceImpl service;

    private static User createUser() {
        return createUserWith(null, null);
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

    private static Verification pendingVerification(Mobile target) {
        return Verification.builder()
                .id(UUId.random())
                .source(UCC_SOURCE)
                .state(VerificationState.P)
                .recipient(new SmsRecipient(target))
                .code(new VerificationCode(VALID_CODE, Instant.now().plus(Duration.ofMinutes(5))))
                .build();
    }

    private static Verification expiredVerification() {
        return Verification.builder()
                .id(UUId.random())
                .source(UCC_SOURCE)
                .state(VerificationState.P)
                .recipient(new SmsRecipient(NEW_MOBILE))
                .code(new VerificationCode(VALID_CODE, Instant.EPOCH))
                .build();
    }

    private static Verification pendingEmailVerification() {
        return Verification.builder()
                .id(UUId.random())
                .source(UCC_SOURCE)
                .state(VerificationState.P)
                .recipient(new EmailRecipient(NEW_EMAIL))
                .code(new VerificationCode(VALID_CODE, Instant.now().plus(Duration.ofMinutes(30))))
                .build();
    }

    private void stubPendingVerification(Verification pending) {
        when(verificationGateway.findLatestBySourceAndStateIn(
                UCC_SOURCE, List.of(VerificationState.P)))
                .thenReturn(pending == null ? Optional.empty() : Optional.of(pending));
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

    @BeforeEach
    void setUp() {
        service = new UserAuthServiceImpl(userGateway, verificationGateway,
                new CredentialChangeDomainService(),
                domainEventBus, credentialHasher,
                new UserVerificationFactory(randomStringGenerator));
    }

    // ─── UCC 发码 ───

    @Nested
    @DisplayName("请求发送换绑手机号验证码（scene=UCC, channel=S）")
    class RequestChangeMobileCode {

        @Test
        @DisplayName("创建 INITIALIZED 验证聚合（source=UCC:userId、recipient=Sms），save 并发布创建事件")
        void should_saveInitializedVerificationAndPublishEvent() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));

            service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value()));

            ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            Verification saved = captor.getValue();
            assertThat(saved.getState()).isEqualTo(VerificationState.I);
            assertThat(saved.getSource()).isEqualTo(UCC_SOURCE);
            assertThat(saved.getRecipient()).isEqualTo(new SmsRecipient(NEW_MOBILE));
            assertThat(saved.getCode().code()).isEqualTo(VALID_CODE);
            assertPublishedVerificationCreatedEvent();
        }

        @Test
        @DisplayName("用户处于禁用态（D）：拒绝发码（启用态前置，ADR-0021），不落库不发布")
        void should_throw_when_userDisabled() {
            when(userGateway.findById(USER_ID))
                    .thenReturn(Optional.of(User.builder()
                            .id(USER_ID).version(Version.of(1))
                            .username(new Username("testuser")).nickname(new Nickname("Test_User"))
                            .state(UserState.D)
                            .passwordAccount(stubPasswordAccount())
                            .accounts(List.of()).build()));

            assertThatThrownBy(() -> service.requestChangeMobileCode(
                    new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("user must be enabled");
            verify(verificationGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }

        @Test
        @DisplayName("用户处于注销终态（R）：拒绝发码（终态禁写），不落库不发布")
        void should_throw_when_userDeregistered() {
            when(userGateway.findById(USER_ID))
                    .thenReturn(Optional.of(User.builder()
                            .id(USER_ID).version(Version.of(1))
                            .username(new Username("testuser")).nickname(new Nickname("Test_User"))
                            .state(UserState.R)
                            .passwordAccount(stubPasswordAccount())
                            .accounts(List.of()).build()));

            assertThatThrownBy(() -> service.requestChangeMobileCode(
                    new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("terminal state");
            verify(verificationGateway, never()).save(any());
        }

        @Test
        @DisplayName("目标与原手机号相同：抛异常，不落库不发布")
        void should_throw_when_sameMobile() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUserWith(NEW_MOBILE, null)));

            assertThatThrownBy(() -> service.requestChangeMobileCode(
                    new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
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

            assertThatThrownBy(() -> service.requestChangeMobileCode(
                    new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Mobile already exists: " + NEW_MOBILE.value());
            verify(verificationGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void should_throw_when_userNotFound() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.requestChangeMobileCode(
                    new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
            verify(verificationGateway, never()).save(any());
        }

        @Test
        @DisplayName("槽位已占（existsBySource=true）：拒绝，不落库")
        void should_throw_when_slotOccupied() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));
            when(verificationGateway.existsBySource(UCC_SOURCE)).thenReturn(true);

            assertThatThrownBy(() -> service.requestChangeMobileCode(
                    new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("active verification already exists");

            verify(verificationGateway, never()).save(any());
        }

        @Test
        @DisplayName("槽位空闲：创建并保存（惰性腾槽收敛在 gateway.save 内，见 ADR-0026）")
        void should_createAndSave_when_slotFree() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(verificationGateway.existsBySource(UCC_SOURCE)).thenReturn(false);
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));

            service.requestChangeMobileCode(new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value()));

            verify(verificationGateway).save(any(Verification.class));
            assertPublishedVerificationCreatedEvent();
        }

        @Test
        @DisplayName("并发撞 uk_active_key（save 抛 DataIntegrityViolation）：原样上抛（预检 + DB 兜底，无翻译）")
        void should_propagateConstraintViolation_when_concurrentDuplicate() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));
            when(verificationGateway.save(any(Verification.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_active_key"));

            assertThatThrownBy(() -> service.requestChangeMobileCode(
                    new RequestChangeMobileCodeCommand(1L, NEW_MOBILE.value())))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uk_active_key");
            verify(domainEventBus, never()).publishAll(any());
        }
    }

    @Nested
    @DisplayName("请求发送换绑邮箱验证码（scene=UCC, channel=E）")
    class RequestChangeEmailCode {

        @Test
        @DisplayName("创建 INITIALIZED 验证聚合（recipient=Email），save 并发布创建事件")
        void should_saveInitializedVerificationAndPublishEvent() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(randomStringGenerator.generate(any(PositiveInt.class), any(Alphabet.class)))
                    .thenReturn(new RandomString(VALID_CODE));

            service.requestChangeEmailCode(new RequestChangeEmailCodeCommand(1L, NEW_EMAIL.value()));

            ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            Verification saved = captor.getValue();
            assertThat(saved.getState()).isEqualTo(VerificationState.I);
            assertThat(saved.getSource()).isEqualTo(UCC_SOURCE);
            assertThat(saved.getRecipient()).isEqualTo(new EmailRecipient(NEW_EMAIL));
            assertPublishedVerificationCreatedEvent();
        }

        @Test
        @DisplayName("目标与原邮箱相同：抛异常，不落库不发布")
        void should_throw_when_sameEmail() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUserWith(null, NEW_EMAIL)));

            assertThatThrownBy(() -> service.requestChangeEmailCode(
                    new RequestChangeEmailCodeCommand(1L, NEW_EMAIL.value())))
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

            assertThatThrownBy(() -> service.requestChangeEmailCode(
                    new RequestChangeEmailCodeCommand(1L, NEW_EMAIL.value())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already exists: " + NEW_EMAIL.value());
            verify(verificationGateway, never()).save(any());
            verify(domainEventBus, never()).publishAll(any());
        }
    }

    // ─── 修改密码 ───

    @Nested
    @DisplayName("修改密码")
    class ChangePassword {

        @Test
        @DisplayName("成功重哈希并保存用户、发布事件")
        void should_changePasswordAndPublishEvents_when_userExists() {
            var user = createUser();
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

    // ─── 确认手机号变更 ───

    @Nested
    @DisplayName("确认手机号变更")
    class ChangeMobile {

        @Test
        @DisplayName("验证成功：用户保存、验证聚合落库为 USED")
        void should_saveUserAndUsedVerification_when_codeMatches() {
            var user = createUser();
            var pending = pendingVerification(NEW_MOBILE);
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            stubPendingVerification(pending);

            service.changeMobile(new ChangeMobileCommand(1L, VALID_CODE));

            assertThat(user.getMobile()).hasValue(NEW_MOBILE);
            verify(userGateway).save(user);
            ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(VerificationState.U);
        }

        @Test
        @DisplayName("错码：抛异常且不落库（实体无变更），用户不保存")
        void should_notSaveAnything_when_codeMismatch() {
            var user = createUser();
            var pending = pendingVerification(NEW_MOBILE);
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            stubPendingVerification(pending);

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
            var expired = expiredVerification();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            stubPendingVerification(expired);

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
        @DisplayName("无待验证聚合时抛出异常（按 source 键控查询）")
        void should_throw_when_noPendingVerification() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            stubPendingVerification(null);

            assertThatThrownBy(() ->
                    service.changeMobile(new ChangeMobileCommand(1L, VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No pending verification");
            verify(userGateway, never()).save(any());
        }
    }

    // ─── 确认邮箱变更 ───

    @Nested
    @DisplayName("确认邮箱变更")
    class ChangeEmail {

        @Test
        @DisplayName("验证成功：用户保存、验证聚合落库为 USED")
        void should_saveUserAndUsedVerification_when_codeMatches() {
            var user = createUser();
            var pending = pendingEmailVerification();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            stubPendingVerification(pending);

            service.changeEmail(new ChangeEmailCommand(1L, VALID_CODE));

            assertThat(user.getEmail()).hasValue(NEW_EMAIL);
            verify(userGateway).save(user);
            ArgumentCaptor<Verification> captor = ArgumentCaptor.forClass(Verification.class);
            verify(verificationGateway).save(captor.capture());
            assertThat(captor.getValue().getState()).isEqualTo(VerificationState.U);
        }

        @Test
        @DisplayName("错码：抛异常且不落库（实体无变更），用户不保存")
        void should_notSaveAnything_when_codeMismatch() {
            var user = createUser();
            var pending = pendingEmailVerification();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
            stubPendingVerification(pending);

            assertThatThrownBy(() ->
                    service.changeEmail(new ChangeEmailCommand(1L, "000000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid verification code");

            verify(verificationGateway, never()).save(any());
            verify(userGateway, never()).save(any());
        }

        @Test
        @DisplayName("无待验证聚合时抛出异常（按 source 键控查询）")
        void should_throw_when_noPendingVerification() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            stubPendingVerification(null);

            assertThatThrownBy(() ->
                    service.changeEmail(new ChangeEmailCommand(1L, VALID_CODE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No pending verification");
            verify(userGateway, never()).save(any());
        }
    }

    private static PasswordAuthAccount stubPasswordAccount() {
        return PasswordAuthAccount.builder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(Active.TRUE)
                .passwordHash(STUB_HASH)
                .build();
    }
}
