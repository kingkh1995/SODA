package com.soda.user.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.soda.component.support.types.Active;
import com.soda.component.support.types.Email;
import com.soda.component.support.types.Mobile;
import com.soda.component.support.types.PasswordHash;
import com.soda.component.support.types.RawPassword;
import com.soda.component.support.gateway.PasswordEncoder;
import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.Sex;
import com.soda.user.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link User} 聚合根单元测试。
 * <p>
 * 验证：
 * <ul>
 *   <li>创建时生成默认状态，无 ID，无 Account</li>
 *   <li>恢复后状态与持久化一致</li>
 *   <li>{@link User#authenticate(AuthAccountType, String, PasswordEncoder)} 分发到正确子类</li>
 *   <li>创建时注册 {@link UserCreatedEvent}，entityId 延迟求值</li>
 * </ul>
 */
class UserTest {

    private static final UserId USER_ID = new UserId(1L);
    private static final Username USERNAME = new Username("testuser");
    private static final Nickname NICKNAME = new Nickname("Test User");

    private static final PasswordEncoder PASSWORD_ENCODER = new PasswordEncoder() {
        @Override
        public PasswordHash encode(RawPassword rawPassword) {
            return new PasswordHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        }

        @Override
        public boolean matches(RawPassword rawPassword, PasswordHash hash) {
            return "password123".equals(rawPassword.value());
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
        assertNull(user.getSex());

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

        assertEquals(mobile, user.getMobile());
        assertEquals(email, user.getEmail());
        assertEquals(Sex.F, user.getSex());
        assertEquals(avatar, user.getAvatar());
    }

    @Test
    void restoreBuilder_restoresState() {
        var mobile = new Mobile("13800138000");
        var email = new Email("test@example.com");
        var avatar = new Avatar("https://example.com/avatar.png");
        var passwordAccount = new PasswordAuthAccount(PasswordAuthAccountId.from(USER_ID), Active.TRUE, PASSWORD_ENCODER.encode(new RawPassword("pwd")));
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
        assertEquals(mobile, user.getMobile());
        assertEquals(email, user.getEmail());
        assertEquals(Sex.F, user.getSex());
        assertEquals(avatar, user.getAvatar());
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
        assertNull(user.getMobile());
        assertNull(user.getEmail());
        assertNull(user.getSex());
        assertNull(user.getAvatar());
    }

    // ——— authenticate ———

    @Test
    void authenticate_passwordAccount_correctPassword_returnsTrue() {
        var user = fullUserWithPasswordAccount();
        assertTrue(user.authenticate(AuthAccountType.P, "password123", PASSWORD_ENCODER));
    }

    @Test
    void authenticate_passwordAccount_wrongPassword_returnsFalse() {
        var user = fullUserWithPasswordAccount();
        assertFalse(user.authenticate(AuthAccountType.P, "wrong", PASSWORD_ENCODER));
    }

    @Test
    void authenticate_noSuchAccount_returnsFalse() {
        var user = fullUserWithPasswordAccount();
        assertFalse(user.authenticate(AuthAccountType.S, "any", PASSWORD_ENCODER));
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

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

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

    // ——— helper ———

    /** 创建含 PasswordAccount 的完整 User（已有 ID 和账户，常用于认证测试）。 */
    private static User fullUserWithPasswordAccount() {
        var hash = PASSWORD_ENCODER.encode(new RawPassword("password123"));
        var passwordAccount = new PasswordAuthAccount(PasswordAuthAccountId.from(USER_ID), Active.TRUE, hash);
        return User.restoreBuilder()
                .id(USER_ID)
                .username(USERNAME)
                .nickname(NICKNAME)
                .status(UserStatus.E)
                .accounts(List.of(passwordAccount))
                .build();
    }
}
