package com.soda.user.domain;

import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RawCredential;
import com.soda.user.domain.event.UserCreatedEvent;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.Sex;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.SocialType;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserStatus;
import com.soda.user.domain.types.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link User} 聚合根单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>创建时生成默认状态，无 ID，无 Account</li>
 *   <li>恢复后状态与持久化一致</li>
 *   <li>{@link User#authenticate(AuthAccountType, String, CredentialHasher)} 分发到正确子类</li>
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
            return "password123".equals(credential.internalValue());
        }
    };

    // ─── helpers ───

    private static User fullUserWithPasswordAccount() {
        var hash = PASSWORD_HASHER.hash(new RawCredential("password123"));
        var passwordAccount = new PasswordAuthAccount(PasswordAuthAccountId.from(USER_ID), Active.TRUE, hash);
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .status(UserStatus.E)
                .accounts(List.of(passwordAccount))
                .build();
    }

    private static User fullUserWithSmsAccount() {
        var mobile = new Mobile("13800138000");
        var smsAccount = new SmsAuthAccount(SmsAuthAccountId.from(mobile), Active.TRUE, null, null);
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .status(UserStatus.E)
                .accounts(List.of(smsAccount))
                .build();
    }

    private static User fullUserWithEmailAccount() {
        var email = new Email("test@example.com");
        var emailAccount = new EmailAuthAccount(EmailAuthAccountId.from(email), Active.TRUE, null, null);
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .status(UserStatus.E)
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
                .status(UserStatus.E)
                .accounts(List.of(socialAccount))
                .build();
    }

    private static User fullUserWithMixedAccounts() {
        var hash = PASSWORD_HASHER.hash(new RawCredential("password123"));
        var passwordAccount = new PasswordAuthAccount(PasswordAuthAccountId.from(USER_ID), Active.TRUE, hash);
        var mobile = new Mobile("13800138000");
        var smsAccount = new SmsAuthAccount(SmsAuthAccountId.from(mobile), Active.TRUE, null, null);
        var email = new Email("test@example.com");
        var emailAccount = new EmailAuthAccount(EmailAuthAccountId.from(email), Active.TRUE, null, null);
        var socialAccount = SocialAuthAccount.createBuilder()
                .socialType(SocialType.GE)
                .openId("openid123")
                .build();
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .status(UserStatus.E)
                .accounts(List.of(passwordAccount, smsAccount, emailAccount, socialAccount))
                .build();
    }

    // ─── construction ───

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("默认值创建 User")
        void should_createWithDefaults_when_requiredFieldsOnly() {
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .build();

            assertThat(user.getId()).isNull();
            assertThat(user.getUsername()).isEqualTo(USERNAME);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getStatus()).isEqualTo(UserStatus.E);
            assertThat(user.getSex()).isEmpty();
            assertThat(user.getAccounts()).isEmpty();
        }

        @Test
        @DisplayName("创建时注册 UserCreatedEvent")
        void should_registerUserCreatedEvent_when_created() {
            var user = User.createBuilder()
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .build();
            var events = user.flushEvents();
            assertThat(events).hasSize(1);
            assertThat(events.getFirst()).isInstanceOf(UserCreatedEvent.class);
            // event.entityId 在 flush 时 id 尚为 null，返回 null
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
                    .build();

            assertThat(user.getMobile()).hasValue(mobile);
            assertThat(user.getEmail()).hasValue(email);
            assertThat(user.getSex()).hasValue(Sex.F);
            assertThat(user.getAvatar()).hasValue(avatar);
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
            var passwordAccount = new PasswordAuthAccount(
                    PasswordAuthAccountId.from(USER_ID), Active.TRUE,
                    PASSWORD_HASHER.hash(new RawCredential("pwd")));
            var accounts = List.<AuthAccount<?>>of(passwordAccount);

            var user = User.restoreBuilder()
                    .id(USER_ID)
                    .username(USERNAME)
                    .nickname(NICKNAME)
                    .mobile(mobile)
                    .email(email)
                    .sex(Sex.F)
                    .avatar(avatar)
                    .status(UserStatus.D)
                    .accounts(accounts)
                    .build();

            assertThat(user.getId()).isEqualTo(USER_ID);
            assertThat(user.getUsername()).isEqualTo(USERNAME);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getMobile()).hasValue(mobile);
            assertThat(user.getEmail()).hasValue(email);
            assertThat(user.getSex()).hasValue(Sex.F);
            assertThat(user.getAvatar()).hasValue(avatar);
            assertThat(user.getStatus()).isEqualTo(UserStatus.D);
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
                    .status(UserStatus.E)
                    .accounts(List.of())
                    .build();

            assertThat(user.getId()).isEqualTo(USER_ID);
            assertThat(user.getMobile()).isEmpty();
            assertThat(user.getEmail()).isEmpty();
            assertThat(user.getSex()).isEmpty();
            assertThat(user.getAvatar()).isEmpty();
        }
    }

    // ─── authentication ───

    @Nested
    @DisplayName("认证")
    class Authentication {

        @Test
        @DisplayName("密码账户正确密码返回 true")
        void should_returnTrue_when_passwordMatches() {
            var user = fullUserWithPasswordAccount();
            assertThat(user.authenticate(AuthAccountType.P, "password123", PASSWORD_HASHER)).isTrue();
        }

        @Test
        @DisplayName("密码账户错误密码返回 false")
        void should_returnFalse_when_passwordMismatches() {
            var user = fullUserWithPasswordAccount();
            assertThat(user.authenticate(AuthAccountType.P, "wrong", PASSWORD_HASHER)).isFalse();
        }

        @Test
        @DisplayName("不存在的账户类型返回 false")
        void should_returnFalse_when_noSuchAccountType() {
            var user = fullUserWithPasswordAccount();
            assertThat(user.authenticate(AuthAccountType.S, "any", PASSWORD_HASHER)).isFalse();
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
                    .build();
            user.assignId(userId);

            var events = user.flushEvents();
            var event = (UserCreatedEvent) events.getFirst();
            assertThat(event.entityId()).isEqualTo(userId);
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
            assertThat(restored.getStatus()).isEqualTo(original.getStatus());
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
                    .status(UserStatus.E)
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
