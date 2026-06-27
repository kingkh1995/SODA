package com.soda.user.domain;

import com.soda.component.support.gateway.CredentialHasher;
import com.soda.component.support.types.Active;
import com.soda.component.support.types.CredentialHash;
import com.soda.component.support.types.Email;
import com.soda.component.support.types.Mobile;
import com.soda.component.support.types.RawCredential;
import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.Sex;
import com.soda.user.domain.enums.SocialType;
import com.soda.user.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

    // ——— construction ———

    @Test
    void createBuilder_createsUserWithDefaults() {
        var user = User.createBuilder()
                .username(USERNAME)
                .nickname(NICKNAME)
                .build();

        assertNull(user.getId());
        assertEquals(USERNAME, user.getUsername());
        assertEquals(NICKNAME, user.getNickname());
        assertEquals(UserStatus.E, user.getStatus());
        assertTrue(user.getSex().isEmpty());

        assertTrue(user.getAccounts().isEmpty());
    }

    @Test
    void createBuilder_registersUserCreatedEvent() {
        var user = User.createBuilder()
                .username(USERNAME)
                .nickname(NICKNAME)
                .build();
        var events = user.flushEvents();
        assertEquals(1, events.size());
        assertInstanceOf(UserCreatedEvent.class, events.getFirst());
        // event.entityId 在 flush 时 id 尚为 null，返回 null
        assertNull(events.getFirst().entityId());
    }

    @Test
    void createBuilder_withOptionalFields() {
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

        assertEquals(Optional.of(mobile), user.getMobile());
        assertEquals(Optional.of(email), user.getEmail());
        assertEquals(Optional.of(Sex.F), user.getSex());
        assertEquals(Optional.of(avatar), user.getAvatar());
    }

    @Test
    void restoreBuilder_restoresState() {
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

        assertEquals(USER_ID, user.getId());
        assertEquals(USERNAME, user.getUsername());
        assertEquals(NICKNAME, user.getNickname());
        assertEquals(Optional.of(mobile), user.getMobile());
        assertEquals(Optional.of(email), user.getEmail());
        assertEquals(Optional.of(Sex.F), user.getSex());
        assertEquals(Optional.of(avatar), user.getAvatar());
        assertEquals(UserStatus.D, user.getStatus());
        assertEquals(1, user.getAccounts().size());
        assertTrue(user.flushEvents().isEmpty()); // no new events from restore
    }

    @Test
    void restoreBuilder_withNullOptionals() {
        var user = User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .status(UserStatus.E)
                .accounts(List.of())
                .build();

        assertEquals(USER_ID, user.getId());
        assertTrue(user.getMobile().isEmpty());
        assertTrue(user.getEmail().isEmpty());
        assertTrue(user.getSex().isEmpty());
        assertTrue(user.getAvatar().isEmpty());
    }

    // ——— authenticate ———

    @Test
    void authenticate_passwordAccount_correctPassword_returnsTrue() {
        var user = fullUserWithPasswordAccount();
        assertTrue(user.authenticate(AuthAccountType.P, "password123", PASSWORD_HASHER));
    }

    @Test
    void authenticate_passwordAccount_wrongPassword_returnsFalse() {
        var user = fullUserWithPasswordAccount();
        assertFalse(user.authenticate(AuthAccountType.P, "wrong", PASSWORD_HASHER));
    }

    @Test
    void authenticate_noSuchAccount_returnsFalse() {
        var user = fullUserWithPasswordAccount();
        assertFalse(user.authenticate(AuthAccountType.S, "any", PASSWORD_HASHER));
    }

    // ——— events ———

    @Test
    void flushEvents_returnsUserCreatedEvent() {
        var user = User.createBuilder()
                .username(USERNAME)
                .nickname(NICKNAME)
                .build();
        var events = user.flushEvents();
        assertFalse(events.isEmpty());
        assertInstanceOf(UserCreatedEvent.class, events.getFirst());
    }

    @Test
    void userCreatedEvent_entityId_lazyResolved() {
        var userId = new UserId(42L);
        var user = User.createBuilder()
                .username(USERNAME)
                .nickname(NICKNAME)
                .build();
        user.assignId(userId);

        var events = user.flushEvents();
        var event = (UserCreatedEvent) events.getFirst();
        assertEquals(userId, event.entityId());
    }

    // ——— JSON ———

    @Test
    void jackson_serializeDeserialize() throws Exception {
        var original = fullUserWithPasswordAccount();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, User.class);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getUsername(), restored.getUsername());
        assertEquals(original.getNickname(), restored.getNickname());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(original.getAccounts().size(), restored.getAccounts().size());
        assertEquals(original.getAccounts().get(0).getId(), restored.getAccounts().get(0).getId());
        assertEquals(
                original.getAccounts().get(0).getAuthAccountType(),
                restored.getAccounts().get(0).getAuthAccountType()
        );
    }

    // ——— additional auth account types ———

    @Test
    void jackson_serializeDeserialize_withSmsAccount() throws Exception {
        var original = fullUserWithSmsAccount();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, User.class);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getAccounts().size(), restored.getAccounts().size());
        assertEquals(AuthAccountType.S, restored.getAccounts().get(0).getAuthAccountType());
    }

    @Test
    void jackson_serializeDeserialize_withEmailAccount() throws Exception {
        var original = fullUserWithEmailAccount();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, User.class);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getAccounts().size(), restored.getAccounts().size());
        assertEquals(AuthAccountType.E, restored.getAccounts().get(0).getAuthAccountType());
    }

    @Test
    void jackson_serializeDeserialize_withSocialAccount() throws Exception {
        var original = fullUserWithSocialAccount();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, User.class);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getAccounts().size(), restored.getAccounts().size());
        assertEquals(AuthAccountType.O, restored.getAccounts().get(0).getAuthAccountType());
    }

    @Test
    void jackson_serializeDeserialize_withMixedAccounts() throws Exception {
        var original = fullUserWithMixedAccounts();
        var json = MAPPER.writeValueAsString(original);
        var restored = MAPPER.readValue(json, User.class);
        assertEquals(original.getId(), restored.getId());
        assertEquals(4, restored.getAccounts().size());
        var restoredTypes = restored.getAccounts().stream()
                .map(a -> a.getAuthAccountType())
                .toList();
        assertTrue(restoredTypes.containsAll(List.of(
                AuthAccountType.P, AuthAccountType.S, AuthAccountType.E, AuthAccountType.O
        )));
    }

    // ——— identity ———

    @Test
    void notEqual_whenDifferentInstance() {
        // Entities use reference identity — same fields ≠ equal
        var a = fullUserWithPasswordAccount();
        var b = fullUserWithPasswordAccount();
        assertNotEquals(a, b);
    }

    @Test
    void toString_containsClassName() {
        var user = fullUserWithPasswordAccount();
        assertTrue(user.toString().contains("User@"));
    }
    // ——— helper ———

    /** 创建含 PasswordAccount 的完整 User（已有 ID 和账户，常用于认证测试）。 */
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
}
