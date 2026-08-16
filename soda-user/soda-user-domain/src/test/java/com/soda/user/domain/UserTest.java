package com.soda.user.domain;

import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.gateway.EmailSender;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.RawCredential;
import com.soda.component.domain.types.Sex;
import com.soda.component.domain.types.Version;
import com.soda.user.domain.event.UserCreatedEvent;
import com.soda.user.domain.event.UserDeregisteredEvent;
import com.soda.user.domain.event.UserStateChangedEvent;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.SocialType;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import java.time.Instant;
import java.util.List;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link User} 聚合根单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>创建时生成默认状态，无 ID，自动附加密码账户</li>
 *   <li>恢复后状态与持久化一致</li>
 *   <li>创建时注册 {@link UserCreatedEvent}，entityId 延迟求值</li>
 * </ul>
 */
@DisplayName("User 聚合根")
class UserTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final Username USERNAME = new Username("testuser");
    private static final Nickname NICKNAME = new Nickname("Test_User");

    private static final CredentialHash STUB_HASH = new CredentialHash(
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    private static final CredentialHasher PASSWORD_HASHER = new CredentialHasher() {
        @Override
        public CredentialHash hash(RawCredential credential) {
            return STUB_HASH;
        }

        @Override
        public boolean matches(RawCredential credential, CredentialHash hash) {
            return "password123".equals(credential.rawValue());
        }
    };
    private static final RandomStringGenerator
            CODE_GENERATOR = (length, alphabet) -> new RandomString("123456");

    // 测试用的发送桩（不真正外发）
    private static final SmsSender SMS_SENDER = (to, content) -> {
    };
    private static final EmailSender EMAIL_SENDER = (to, content) -> {
    };

    // ─── helpers ───

    private static PasswordAuthAccount stubPasswordAccount() {
        return PasswordAuthAccount.builder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(Active.TRUE)
                .passwordHash(STUB_HASH)
                .build();
    }

    private static User fullUserWithPasswordAccount() {
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .passwordAccount(stubPasswordAccount())
                .accounts(List.of())
                .build();
    }

    private static User disabledUser() {
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.D)
                .passwordAccount(stubPasswordAccount())
                .build();
    }

    private static User fullUserWithSmsAccount() {
        var mobile = new Mobile("13800138000");
        var smsAccount = SmsAuthAccount.builder()
                .id(SmsAuthAccountId.from(mobile))
                .active(Active.TRUE)
                .build();
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .passwordAccount(stubPasswordAccount())
                .accounts(List.of(smsAccount))
                .build();
    }

    private static User fullUserWithEmailAccount() {
        var email = new Email("test@example.com");
        var emailAccount = EmailAuthAccount.builder()
                .id(EmailAuthAccountId.from(email))
                .active(Active.TRUE)
                .build();
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .passwordAccount(stubPasswordAccount())
                .accounts(List.of(emailAccount))
                .build();
    }

    private static User fullUserWithSocialAccount() {
        var socialAccount = SocialAuthAccount.createBuilder()
                .socialType(SocialType.GE)
                .openId("openid123")
                .build();
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .passwordAccount(stubPasswordAccount())
                .accounts(List.of(socialAccount))
                .build();
    }

    private static User fullUserWithMixedAccounts() {
        var mobile = new Mobile("13800138000");
        var smsAccount = SmsAuthAccount.builder()
                .id(SmsAuthAccountId.from(mobile))
                .active(Active.TRUE)
                .build();
        var email = new Email("test@example.com");
        var emailAccount = EmailAuthAccount.builder()
                .id(EmailAuthAccountId.from(email))
                .active(Active.TRUE)
                .build();
        var socialAccount = SocialAuthAccount.createBuilder()
                .socialType(SocialType.GE)
                .openId("openid123")
                .build();
        return User.builder()
                .id(USER_ID)
                .version(Version.of(1))
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .passwordAccount(stubPasswordAccount())
                .accounts(List.of(smsAccount, emailAccount, socialAccount))
                .build();
    }

    // ─── construction ───

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("默认值创建 User（自动附加密码账户）")
        void should_createWithDefaults_when_requiredFieldsOnly() {
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .passwordHash(STUB_HASH)
                    .build();

            assertThat(user.getId()).isNull();
            assertThat(user.getUsername()).isEqualTo(USERNAME);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getState()).isEqualTo(UserState.E);
            assertThat(user.getSex()).isEmpty();
            assertThat(user.getPasswordAccount()).isNotNull();
            assertThat(user.getAccounts()).isEmpty();
        }

        @Test
        @DisplayName("构造器缺 passwordAccount -> IAE（构造器校验，与 DP 一致）")
        void should_rejectConstruction_when_missingPasswordAccount() {
            assertThatThrownBy(() -> User.builder()
                    .id(USER_ID)
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("创建时注册 UserCreatedEvent")
        void should_registerUserCreatedEvent_when_created() {
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .passwordHash(STUB_HASH)
                    .build();
            var events = user.flushEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(UserCreatedEvent.class);
            // entityId 延迟求值：assignId 前返回 null（jspecify 契约，见 ADR-0015）
            assertThat(events.getFirst().entityId()).isNull();
        }

        @Test
        @DisplayName("携带可选字段创建")
        void should_includeOptionalFields_when_provided() {
            var mobile = new Mobile("13800138000");
            var email = new Email("test@example.com");
            var avatar = new Avatar("https://example.com/avatar.png");
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .mobile(mobile)
                    .email(email)
                    .sex(Sex.F)
                    .avatar(avatar)
                    .passwordHash(STUB_HASH)
                    .build();

            assertThat(user.getMobile()).hasValue(mobile);
            assertThat(user.getEmail()).hasValue(email);
            assertThat(user.getSex()).hasValue(Sex.F);
            assertThat(user.getAvatar()).hasValue(avatar);
            assertThat(user.getPasswordAccount()).isNotNull();
            assertThat(user.getAccounts()).hasSize(2);
            assertThat(user.getAccounts()).filteredOn(a -> a instanceof SmsAuthAccount).hasSize(1);
            assertThat(user.getAccounts()).filteredOn(a -> a instanceof EmailAuthAccount).hasSize(1);
        }

        @Test
        @DisplayName("创建时 version 为 INITIAL")
        void should_initVersion_when_created() {
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .passwordHash(STUB_HASH)
                    .build();

            assertThat(user.getVersion()).isSameAs(Version.INITIAL);
        }
    }

    // ─── restoration ───

    @Nested
    @DisplayName("恢复")
    class Restoration {

        @Test
        @DisplayName("恢复后状态与持久化一致")
        void should_restoreAllFields_when_usingBuilder() {
            var mobile = new Mobile("13800138000");
            var email = new Email("test@example.com");
            var avatar = new Avatar("https://example.com/avatar.png");
            var user = User.builder()
                    .id(USER_ID)
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .mobile(mobile)
                    .email(email)
                    .sex(Sex.F)
                    .avatar(avatar)
                    .state(UserState.D)
                    .passwordAccount(stubPasswordAccount())
                    .passwordAccount(stubPasswordAccount())
                    .build();

            assertThat(user.getId()).isEqualTo(USER_ID);
            assertThat(user.getUsername()).isEqualTo(USERNAME);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getMobile()).hasValue(mobile);
            assertThat(user.getEmail()).hasValue(email);
            assertThat(user.getSex()).hasValue(Sex.F);
            assertThat(user.getAvatar()).hasValue(avatar);
            assertThat(user.getState()).isEqualTo(UserState.D);
            assertThat(user.getVersion()).isEqualTo(Version.of(1));
            assertThat(user.getPasswordAccount()).isNotNull();
            assertThat(user.getAccounts()).isEmpty();
            // restore 不应产生新事件
            assertThat(user.flushEvents()).isEmpty();
        }

        @Test
        @DisplayName("恢复时空可选字段为空")
        void should_restoreNullOptionals_when_notProvided() {
            var user = User.builder()
                    .id(USER_ID)
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .passwordAccount(stubPasswordAccount())
                    .build();

            assertThat(user.getId()).isEqualTo(USER_ID);
            assertThat(user.getMobile()).isEmpty();
            assertThat(user.getEmail()).isEmpty();
            assertThat(user.getSex()).isEmpty();
            assertThat(user.getAvatar()).isEmpty();
        }
    }

    // ─── events ───

    @Nested
    @DisplayName("领域事件")
    class Events {

        @Test
        @DisplayName("flushEvents 包含 UserCreatedEvent")
        void should_containUserCreatedEvent_when_flushAfterCreate() {
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .passwordHash(STUB_HASH)
                    .build();
            var events = user.flushEvents();
            assertThat(events).isNotEmpty();
            assertThat(events.getFirst()).isInstanceOf(UserCreatedEvent.class);
        }

        @Test
        @DisplayName("UserCreatedEvent.entityId 延迟求值")
        void should_resolveEntityIdLazily_when_assignIdAfterCreate() {
            var userId = new UserId(42L);
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .passwordHash(STUB_HASH)
                    .build();
            user.assignId(userId);

            var events = user.flushEvents();
            var event = (UserCreatedEvent) events.getFirst();
            assertThat(event.entityId()).isEqualTo(userId);
        }
    }

    // ─── state transitions ───

    @Nested
    @DisplayName("状态跃迁")
    class StateTransitions {

        @Test
        @DisplayName("disable() 将 E→D 且注册 UserStateChangedEvent")
        void should_disableUser_when_stateIsE() {
            var user = fullUserWithPasswordAccount();
            assertThat(user.getState()).isEqualTo(UserState.E);

            user.disable();

            assertThat(user.getState()).isEqualTo(UserState.D);
            var events = user.flushEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(UserStateChangedEvent.class);
        }

        @Test
        @DisplayName("disable() 已是 D 则 no-op，不发事件")
        void should_doNothing_when_alreadyDisabled() {
            var user = User.builder()
                    .id(new UserId(1L))
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.D)
                    .passwordAccount(stubPasswordAccount())
                    .build();

            user.disable();

            assertThat(user.getState()).isEqualTo(UserState.D);
            assertThat(user.flushEvents()).isEmpty();
        }

        @Test
        @DisplayName("enable() 将 D→E 且注册 UserStateChangedEvent")
        void should_enableUser_when_stateIsD() {
            var user = User.builder()
                    .id(new UserId(1L))
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.D)
                    .passwordAccount(stubPasswordAccount())
                    .build();
            assertThat(user.getState()).isEqualTo(UserState.D);

            user.enable();

            assertThat(user.getState()).isEqualTo(UserState.E);
            var events = user.flushEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(UserStateChangedEvent.class);
        }

        @Test
        @DisplayName("enable() 已是 E 则 no-op，不发事件")
        void should_doNothing_when_alreadyEnabled() {
            var user = fullUserWithPasswordAccount();
            assertThat(user.getState()).isEqualTo(UserState.E);

            user.enable();

            assertThat(user.getState()).isEqualTo(UserState.E);
            assertThat(user.flushEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("注销与生命周期守卫")
    class Deregistration {

        @Test
        @DisplayName("deregister() 前置：非禁用状态抛 IAE")
        void should_rejectDeregister_when_notDisabled() {
            var user = fullUserWithPasswordAccount();
            assertThat(user.getState()).isEqualTo(UserState.E);

            assertThatThrownBy(user::deregister)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only disabled user can be deregistered");
            // 前置失败不注册事件
            assertThat(user.flushEvents()).isEmpty();
        }

        @Test
        @DisplayName("deregister() 禁用状态成功：D→R 并注册 UserDeregisteredEvent")
        void should_deregisterAndRegisterEvent_when_disabled() {
            var user = disabledUser();
            assertThat(user.getState()).isEqualTo(UserState.D);

            user.deregister();

            assertThat(user.getState()).isEqualTo(UserState.R);
            var events = user.flushEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(UserDeregisteredEvent.class);
            assertThat(events.getFirst().entityId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("重复 deregister() 抛 IAE（吸收态守卫）")
        void should_rejectSecondDeregister_when_alreadyDeregistered() {
            var user = disabledUser();
            user.deregister();
            user.flushEvents();

            assertThatThrownBy(user::deregister)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only disabled user can be deregistered");
        }

        @Test
        @DisplayName("deregister() 后行为方法调用抛 IAE，不注册新事件")
        void should_rejectBehaviorMethods_when_deregistered() {
            var user = disabledUser();
            user.deregister();
            user.flushEvents(); // 模拟 appservice publishAll 消费注销事件

            assertThatThrownBy(user::disable)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("user already deregistered");
            assertThatThrownBy(user::enable).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(user::deregister).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changeUsername(new Username("newuser")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changeNickname(new Nickname("New_Name")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changeAvatar(new Avatar("https://example.com/new.png")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changePassword(new RawCredential("password123"), PASSWORD_HASHER))
                    .isInstanceOf(IllegalArgumentException.class);
            // 守卫在方法首行：R（吸收态）下先抛 IAE，验证聚合参数不会触达（null 仅作占位）
            assertThatThrownBy(() -> user.changeMobile(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changeEmail(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(user.flushEvents()).isEmpty();
        }

        @Test
        @DisplayName("禁用状态变更方法抛 IAE（mustEnable 守卫）")
        void should_rejectChanges_when_disabled() {
            var user = disabledUser();

            assertThatThrownBy(() -> user.changeUsername(new Username("newuser")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("user must be enabled");
            assertThatThrownBy(() -> user.changeNickname(new Nickname("New_Name")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changeSex(Sex.M))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changeAvatar(new Avatar("https://example.com/new.png")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changePassword(new RawCredential("password123"), PASSWORD_HASHER))
                    .isInstanceOf(IllegalArgumentException.class);
            // 守卫在方法首行：D 态下先抛 IAE，验证聚合参数不会触达（null 仅作占位）
            assertThatThrownBy(() -> user.changeMobile(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> user.changeEmail(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(user.flushEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("属性修改")
    class ProfileChanges {

        @Test
        @DisplayName("changeUsername 修改用户名")
        void should_changeUsername() {
            var user = fullUserWithPasswordAccount();
            var newName = new Username("newuser");
            user.changeUsername(newName);
            assertThat(user.getUsername()).isEqualTo(newName);
        }

        @Test
        @DisplayName("changeNickname 修改昵称")
        void should_changeNickname() {
            var user = fullUserWithPasswordAccount();
            var newName = new Nickname("New_Name");
            user.changeNickname(newName);
            assertThat(user.getNickname()).isEqualTo(newName);
        }

        @Test
        @DisplayName("修改密码成功并注册 PasswordChangedEvent")
        void should_changePassword() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", Long.toString(USER_ID.value())))
                    .recipient(new SmsRecipient(new Mobile("13900139000")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            user.changeMobile(verification);

            assertThat(user.getMobile()).hasValue(new Mobile("13900139000"));
        }

        @Test
        @DisplayName("changeMobile 拒绝未验证的验证聚合")
        void should_rejectChangeMobile_when_verificationNotVerified() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", Long.toString(USER_ID.value())))
                    .recipient(new SmsRecipient(new Mobile("13900139000")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build();

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification must be verified");
        }

        @Test
        @DisplayName("changeMobile 拒绝非 CC 场景的验证聚合")
        void should_rejectChangeMobile_when_sceneNotCredentialChange() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("ULG", Long.toString(USER_ID.value())))
                    .recipient(new SmsRecipient(new Mobile("13900139000")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification scene must be credential change");
        }

        @Test
        @DisplayName("changeMobile 拒绝目标用户不匹配的验证聚合")
        void should_rejectChangeMobile_when_verificationUserIdMismatch() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", "999"))
                    .recipient(new SmsRecipient(new Mobile("13900139000")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification subject must match");
        }

        @Test
        @DisplayName("changeMobile 拒绝已使用的验证聚合")
        void should_rejectChangeMobile_when_verificationUsed() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", Long.toString(USER_ID.value())))
                    .recipient(new SmsRecipient(new Mobile("13900139000")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));
            verification.use();

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification must be verified");
        }

        @Test
        @DisplayName("changeMobile 拒绝相同手机号")
        void should_rejectChangeMobile_when_sameMobile() {
            var user = User.builder()
                    .id(USER_ID)
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .mobile(new Mobile("13900139000"))
                    .passwordAccount(stubPasswordAccount())
                    .build();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", Long.toString(USER_ID.value())))
                    .recipient(new SmsRecipient(new Mobile("13900139000")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot change to the same mobile");
        }

        @Test
        @DisplayName("changeEmail 修改邮箱")
        void should_changeEmail() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", Long.toString(USER_ID.value())))
                    .recipient(new EmailRecipient(new Email("new@test.com")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            user.changeEmail(verification);

            assertThat(user.getEmail()).hasValue(new Email("new@test.com"));
        }

        @Test
        @DisplayName("changeEmail 拒绝未验证的验证聚合")
        void should_rejectChangeEmail_when_verificationNotVerified() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", Long.toString(USER_ID.value())))
                    .recipient(new EmailRecipient(new Email("new@test.com")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .build();

            assertThatThrownBy(() -> user.changeEmail(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification must be verified");
        }

        @Test
        @DisplayName("changeEmail 拒绝非 CC 场景的验证聚合")
        void should_rejectChangeEmail_when_sceneNotCredentialChange() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("ULG", "new@test.com"))
                    .recipient(new EmailRecipient(new Email("new@test.com")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeEmail(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification scene must be credential change");
        }

        @Test
        @DisplayName("changeEmail 拒绝目标用户不匹配的验证聚合")
        void should_rejectChangeEmail_when_verificationUserIdMismatch() {
            var user = fullUserWithPasswordAccount();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", "999"))
                    .recipient(new EmailRecipient(new Email("new@test.com")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeEmail(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification subject must match");
        }

        @Test
        @DisplayName("changeEmail 拒绝相同邮箱")
        void should_rejectChangeEmail_when_sameEmail() {
            var user = User.builder()
                    .id(USER_ID)
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .email(new Email("new@test.com"))
                    .passwordAccount(stubPasswordAccount())
                    .build();
            var verification = Verification.createBuilder()
                    .source(VerificationSource.of("UCC", Long.toString(USER_ID.value())))
                    .recipient(new EmailRecipient(new Email("new@test.com")))
                    .generator(CODE_GENERATOR)
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .build();
            verification.markSent();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeEmail(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot change to the same email");
        }

        @Test
        @DisplayName("changeSex 修改性别")
        void should_changeSex() {
            var user = fullUserWithPasswordAccount();
            user.changeSex(Sex.M);
            assertThat(user.getSex()).hasValue(Sex.M);
        }

        @Test
        @DisplayName("changeAvatar 修改头像")
        void should_changeAvatar() {
            var user = fullUserWithPasswordAccount();
            var newAvatar = new Avatar("https://example.com/new.png");
            user.changeAvatar(newAvatar);
            assertThat(user.getAvatar()).hasValue(newAvatar);
        }
    }


    // ─── serialization ───

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("密码账户 Jackson round-trip")
        void should_serializeDeserialize_when_passwordAccount() throws Exception {
            var original = fullUserWithPasswordAccount();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("短信账户 Jackson round-trip")
        void should_serializeDeserialize_when_smsAccount() throws Exception {
            var original = fullUserWithSmsAccount();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("邮箱账户 Jackson round-trip")
        void should_serializeDeserialize_when_emailAccount() throws Exception {
            var original = fullUserWithEmailAccount();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("社交账户 Jackson round-trip")
        void should_serializeDeserialize_when_socialAccount() throws Exception {
            var original = fullUserWithSocialAccount();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("混合账户 Jackson round-trip")
        void should_serializeDeserialize_when_mixedAccounts() throws Exception {
            var original = fullUserWithMixedAccounts();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("缺少 id 的 JSON 拒绝")
        void should_reject_when_missingId() {
            var json = """
                    {"username":"testuser","nickname":"Test_User","state":"E"}
                    """;
            assertThatThrownBy(() -> MAPPER.readValue(json, User.class))
                    .isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("缺少 version 的 JSON 拒绝")
        void should_reject_when_missingVersion() {
            var json = """
                    {"id":1,"username":"testuser","nickname":"Test_User","state":"E"}
                    """;
            assertThatThrownBy(() -> MAPPER.readValue(json, User.class))
                    .isInstanceOf(JacksonException.class);
        }

        @Test
        @DisplayName("未知账户判别符拒绝")
        void should_reject_when_unknownAccountType() {
            var json = """
                    {"id":1,"username":"testuser","nickname":"Test_User","state":"E","accounts":[{"authAccountType":"X"}]}
                    """;
            assertThatThrownBy(() -> MAPPER.readValue(json, User.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    // ─── identity ───

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同字段相等")
        void should_beEqual_when_sameFields() {
            var a = fullUserWithPasswordAccount();
            var b = fullUserWithPasswordAccount();
            var user2 = User.builder()
                    .id(new UserId(2L))
                    .version(Version.of(1))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .passwordAccount(stubPasswordAccount())
                    .build();
            assertThat(a).isEqualTo(b);
            assertThat(a).isNotEqualTo(user2);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 包含类名")
        void should_containClassName_when_toString() {
            var user = fullUserWithPasswordAccount();
            assertThat(user.toString()).contains("User@");
        }
    }
}
