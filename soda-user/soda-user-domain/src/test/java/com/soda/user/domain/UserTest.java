package com.soda.user.domain;

import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.RawCredential;
import com.soda.component.domain.types.Sex;
import com.soda.user.domain.event.UserCreatedEvent;
import com.soda.user.domain.event.UserStateChangedEvent;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.SocialType;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationScene;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            CODE_GENERATOR = length -> new RandomString("123456");

    // ─── helpers ───

    private static User fullUserWithPasswordAccount() {
        var hash = PASSWORD_HASHER.hash(new RawCredential("password123"));
        var passwordAccount = PasswordAuthAccount.restoreBuilder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(Active.TRUE)
                .passwordHash(hash)
                .build();
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .accounts(List.of(passwordAccount))
                .build();
    }

    private static User fullUserWithSmsAccount() {
        var mobile = new Mobile("13800138000");
        var smsAccount = SmsAuthAccount.restoreBuilder()
                .id(SmsAuthAccountId.from(mobile))
                .active(Active.TRUE)
                .verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY)
                .build();
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .accounts(List.of(smsAccount))
                .build();
    }

    private static User fullUserWithEmailAccount() {
        var email = new Email("test@example.com");
        var emailAccount = EmailAuthAccount.restoreBuilder()
                .id(EmailAuthAccountId.from(email))
                .active(Active.TRUE)
                .verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY)
                .build();
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .accounts(List.of(emailAccount))
                .build();
    }

    private static User fullUserWithSocialAccount() {
        var socialAccount = SocialAuthAccount.createBuilder()
                .socialType(SocialType.GE)
                .openId("openid123")
                .build();
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .accounts(List.of(socialAccount))
                .build();
    }

    private static User fullUserWithMixedAccounts() {
        var hash = PASSWORD_HASHER.hash(new RawCredential("password123"));
        var passwordAccount = PasswordAuthAccount.restoreBuilder()
                .id(PasswordAuthAccountId.from(USER_ID))
                .active(Active.TRUE)
                .passwordHash(hash)
                .build();
        var mobile = new Mobile("13800138000");
        var smsAccount = SmsAuthAccount.restoreBuilder()
                .id(SmsAuthAccountId.from(mobile))
                .active(Active.TRUE)
                .verificationCodePolicy(SmsAuthAccount.DEFAULT_POLICY)
                .build();
        var email = new Email("test@example.com");
        var emailAccount = EmailAuthAccount.restoreBuilder()
                .id(EmailAuthAccountId.from(email))
                .active(Active.TRUE)
                .verificationCodePolicy(EmailAuthAccount.DEFAULT_POLICY)
                .build();
        var socialAccount = SocialAuthAccount.createBuilder()
                .socialType(SocialType.GE)
                .openId("openid123")
                .build();
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .state(UserState.E)
                .accounts(List.of(passwordAccount, smsAccount, emailAccount, socialAccount))
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
            assertThat(user.getAccounts()).hasSize(1);
            assertThat(user.getAccounts().getFirst()).isInstanceOf(PasswordAuthAccount.class);
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
            // event.entityId 在 flush 时 id 尚为 null → requireId 触发 NPE（防御编程，ADR-0015）；
            // 延迟求值语义见 Events 测试「UserCreatedEvent.entityId 延迟求值」
            assertThatThrownBy(() -> events.getFirst().entityId())
                    .isInstanceOf(NullPointerException.class);
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
            assertThat(user.getAccounts()).hasSize(3);
            assertThat(user.getAccounts()).filteredOn(a -> a instanceof PasswordAuthAccount).hasSize(1);
            assertThat(user.getAccounts()).filteredOn(a -> a instanceof SmsAuthAccount).hasSize(1);
            assertThat(user.getAccounts()).filteredOn(a -> a instanceof EmailAuthAccount).hasSize(1);
        }
    }

    // ─── restoration ───

    @Nested
    @DisplayName("恢复")
    class Restoration {

        @Test
        @DisplayName("恢复后状态与持久化一致")
        void should_restoreAllFields_when_usingRestoreBuilder() {
            var mobile = new Mobile("13800138000");
            var email = new Email("test@example.com");
            var avatar = new Avatar("https://example.com/avatar.png");
            var passwordAccount = PasswordAuthAccount.restoreBuilder()
                    .id(PasswordAuthAccountId.from(USER_ID))
                    .active(Active.TRUE)
                    .passwordHash(PASSWORD_HASHER.hash(new RawCredential("pwd")))
                    .build();
            var accounts = List.<AuthAccount<?>>of(passwordAccount);

            var user = User.restoreBuilder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .mobile(mobile)
                    .email(email)
                    .sex(Sex.F)
                    .avatar(avatar)
                    .state(UserState.D)
                    .accounts(accounts)
                    .build();

            assertThat(user.getId()).isEqualTo(USER_ID);
            assertThat(user.getUsername()).isEqualTo(USERNAME);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getMobile()).hasValue(mobile);
            assertThat(user.getEmail()).hasValue(email);
            assertThat(user.getSex()).hasValue(Sex.F);
            assertThat(user.getAvatar()).hasValue(avatar);
            assertThat(user.getState()).isEqualTo(UserState.D);
            assertThat(user.getAccounts()).hasSize(1);
            // restore 不应产生新事件
            assertThat(user.flushEvents()).isEmpty();
        }

        @Test
        @DisplayName("恢复时空可选字段为空")
        void should_restoreNullOptionals_when_notProvided() {
            var user = User.restoreBuilder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .accounts(List.of())
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
            var user = User.restoreBuilder()
                    .id(new UserId(1L))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.D)
                    .accounts(List.of())
                    .build();

            user.disable();

            assertThat(user.getState()).isEqualTo(UserState.D);
            assertThat(user.flushEvents()).isEmpty();
        }

        @Test
        @DisplayName("enable() 将 D→E 且注册 UserStateChangedEvent")
        void should_enableUser_when_stateIsD() {
            var user = User.restoreBuilder()
                    .id(new UserId(1L))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.D)
                    .accounts(List.of())
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
        @DisplayName("changeMobile 修改手机号")
        void should_changeMobile() {
            var user = fullUserWithPasswordAccount();
            var verification = SmsVerification.createBuilder()
                    .userId(new LongId(1L))
                    .scene(VerificationScene.CC)
                    .target(new Mobile("13900139000"))
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString("123456"));

            user.changeMobile(verification);

            assertThat(user.getMobile()).hasValue(new Mobile("13900139000"));
        }

        @Test
        @DisplayName("changeMobile 拒绝未验证的验证聚合")
        void should_rejectChangeMobile_when_verificationNotVerified() {
            var user = fullUserWithPasswordAccount();
            var verification = SmsVerification.createBuilder()
                    .userId(new LongId(1L))
                    .scene(VerificationScene.CC)
                    .target(new Mobile("13900139000"))
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification must be verified");
        }

        @Test
        @DisplayName("changeMobile 拒绝目标用户不匹配的验证聚合")
        void should_rejectChangeMobile_when_verificationUserIdMismatch() {
            var user = fullUserWithPasswordAccount();
            var verification = SmsVerification.createBuilder()
                    .userId(new LongId(999L))
                    .scene(VerificationScene.CC)
                    .target(new Mobile("13900139000"))
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification userId must match");
        }

        @Test
        @DisplayName("changeMobile 拒绝已使用的验证聚合")
        void should_rejectChangeMobile_when_verificationUsed() {
            var user = fullUserWithPasswordAccount();
            var verification = SmsVerification.createBuilder()
                    .userId(new LongId(1L))
                    .scene(VerificationScene.CC)
                    .target(new Mobile("13900139000"))
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString("123456"));
            verification.use();

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification must be verified");
        }

        @Test
        @DisplayName("changeMobile 拒绝相同手机号")
        void should_rejectChangeMobile_when_sameMobile() {
            var user = User.restoreBuilder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .mobile(new Mobile("13900139000"))
                    .accounts(List.of())
                    .build();
            var verification = SmsVerification.createBuilder()
                    .userId(new LongId(1L))
                    .scene(VerificationScene.CC)
                    .target(new Mobile("13900139000"))
                    .policy(VerificationCodePolicy.DEFAULT_SMS)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeMobile(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot change to the same mobile");
        }

        @Test
        @DisplayName("changeEmail 修改邮箱")
        void should_changeEmail() {
            var user = fullUserWithPasswordAccount();
            var verification = EmailVerification.createBuilder()
                    .userId(new LongId(1L))
                    .scene(VerificationScene.CC)
                    .target(new Email("new@test.com"))
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString("123456"));

            user.changeEmail(verification);

            assertThat(user.getEmail()).hasValue(new Email("new@test.com"));
        }

        @Test
        @DisplayName("changeEmail 拒绝未验证的验证聚合")
        void should_rejectChangeEmail_when_verificationNotVerified() {
            var user = fullUserWithPasswordAccount();
            var verification = EmailVerification.createBuilder()
                    .userId(new LongId(1L))
                    .scene(VerificationScene.CC)
                    .target(new Email("new@test.com"))
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .generator(CODE_GENERATOR)
                    .build();

            assertThatThrownBy(() -> user.changeEmail(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification must be verified");
        }

        @Test
        @DisplayName("changeEmail 拒绝目标用户不匹配的验证聚合")
        void should_rejectChangeEmail_when_verificationUserIdMismatch() {
            var user = fullUserWithPasswordAccount();
            var verification = EmailVerification.createBuilder()
                    .userId(new LongId(999L))
                    .scene(VerificationScene.CC)
                    .target(new Email("new@test.com"))
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .generator(CODE_GENERATOR)
                    .build();
            verification.verify(Instant.now(), new RandomString("123456"));

            assertThatThrownBy(() -> user.changeEmail(verification))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("verification userId must match");
        }

        @Test
        @DisplayName("changeEmail 拒绝相同邮箱")
        void should_rejectChangeEmail_when_sameEmail() {
            var user = User.restoreBuilder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .email(new Email("new@test.com"))
                    .accounts(List.of())
                    .build();
            var verification = EmailVerification.createBuilder()
                    .userId(new LongId(1L))
                    .scene(VerificationScene.CC)
                    .target(new Email("new@test.com"))
                    .policy(VerificationCodePolicy.DEFAULT_EMAIL)
                    .generator(CODE_GENERATOR)
                    .build();
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
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getUsername()).isEqualTo(original.getUsername());
            assertThat(restored.getNickname()).isEqualTo(original.getNickname());
            assertThat(restored.getState()).isEqualTo(original.getState());
            assertThat(restored.getAccounts()).hasSize(original.getAccounts().size());
            assertThat(restored.getAccounts().get(0).getId()).isEqualTo(original.getAccounts().get(0).getId());
            assertThat(restored.getAccounts().get(0).getAuthAccountType())
                    .isEqualTo(original.getAccounts().get(0).getAuthAccountType());
        }

        @Test
        @DisplayName("短信账户 Jackson round-trip")
        void should_serializeDeserialize_when_smsAccount() throws Exception {
            var original = fullUserWithSmsAccount();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getAccounts()).hasSize(original.getAccounts().size());
            assertThat(restored.getAccounts().get(0).getAuthAccountType()).isEqualTo(AuthAccountType.S);
        }

        @Test
        @DisplayName("邮箱账户 Jackson round-trip")
        void should_serializeDeserialize_when_emailAccount() throws Exception {
            var original = fullUserWithEmailAccount();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getAccounts()).hasSize(original.getAccounts().size());
            assertThat(restored.getAccounts().get(0).getAuthAccountType()).isEqualTo(AuthAccountType.E);
        }

        @Test
        @DisplayName("社交账户 Jackson round-trip")
        void should_serializeDeserialize_when_socialAccount() throws Exception {
            var original = fullUserWithSocialAccount();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getAccounts()).hasSize(original.getAccounts().size());
            assertThat(restored.getAccounts().get(0).getAuthAccountType()).isEqualTo(AuthAccountType.O);
        }

        @Test
        @DisplayName("混合账户 Jackson round-trip")
        void should_serializeDeserialize_when_mixedAccounts() throws Exception {
            var original = fullUserWithMixedAccounts();
            var json = MAPPER.writeValueAsString(original);
            var restored = MAPPER.readValue(json, User.class);
            assertThat(restored.getId()).isEqualTo(original.getId());
            assertThat(restored.getAccounts()).hasSize(4);
            var restoredTypes = restored.getAccounts().stream()
                    .map(a -> a.getAuthAccountType())
                    .toList();
            assertThat(restoredTypes).containsExactlyInAnyOrder(
                    AuthAccountType.P, AuthAccountType.S, AuthAccountType.E, AuthAccountType.O
            );
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
            var user2 = User.restoreBuilder()
                    .id(new UserId(2L))
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .state(UserState.E)
                    .accounts(List.of())
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
