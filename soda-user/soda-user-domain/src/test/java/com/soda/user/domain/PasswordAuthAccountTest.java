package com.soda.user.domain;

import com.soda.component.support.gateway.CredentialHasher;
import com.soda.component.support.types.Active;
import com.soda.component.support.types.CredentialHash;
import com.soda.component.support.types.RawCredential;
import com.soda.user.domain.enums.AuthAccountType;
import org.junit.jupiter.api.Test;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PasswordAuthAccount} 单元测试。
 */
class PasswordAuthAccountTest {

    private static final PasswordAuthAccountId ID = PasswordAuthAccountId.from(new UserId(1L));
    private static final CredentialHash HASH = new CredentialHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    private static final CredentialHasher STUB = new CredentialHasher() {
        @Override
        public CredentialHash hash(RawCredential credential) { return HASH; }
        @Override
        public boolean matches(RawCredential credential, CredentialHash hash) {
            return "secret123".equals(credential.internalValue());
        }
    };

    @Test void constructor_setsIdAndHash() {
        var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
        assertEquals(ID, a.getId()); assertEquals(HASH, a.getPasswordHash());
    }
    @Test void getAuthAccountType_returnsP() {
        var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
        assertEquals(AuthAccountType.P, a.getAuthAccountType());
    }
    @Test void verify_correctPassword_returnsTrue() {
        var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
        assertTrue(a.verify(new RawCredential("secret123"), STUB));
    }
    @Test void verify_wrongPassword_returnsFalse() {
        var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
        assertFalse(a.verify(new RawCredential("wrong"), STUB));
    }
    @Test void changePassword_updatesHash() {
        var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
        a.changePassword(new RawCredential("x"), STUB);
        assertEquals(HASH, a.getPasswordHash());
    }
    @Test void activeTrue_isActive() {
        assertTrue(new PasswordAuthAccount(ID, Active.TRUE, HASH).isActive());
    }
    @Test void activeFalse_isInactive() {
        assertFalse(new PasswordAuthAccount(ID, Active.FALSE, HASH).isActive());
    }
    @Test void deactivate_setsInactive() {
        var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
        a.deactivate(); assertFalse(a.isActive());
    }
    @Test void activate_restoresActive() {
        var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
        a.deactivate(); a.activate(); assertTrue(a.isActive());
    }

    // ——— factories ———

    @Test void createBuilder_setsDefaults() {
        var a = PasswordAuthAccount.createBuilder().userId(new UserId(1L)).passwordHash(HASH).build();
        assertEquals(PasswordAuthAccountId.of("P:1"), a.getId());
        assertTrue(a.isActive());
        assertEquals(HASH, a.getPasswordHash());
    }
    @Test void restoreBuilder_restoresAllFields() {
        var a = PasswordAuthAccount.restoreBuilder().id(ID).active(Active.FALSE).passwordHash(HASH).build();
        assertEquals(ID, a.getId()); assertFalse(a.isActive()); assertEquals(HASH, a.getPasswordHash());
    }

    // ——— JSON ———

    @Test void jackson_serializeDeserialize() throws Exception {
        var o = PasswordAuthAccount.createBuilder().userId(new UserId(1L)).passwordHash(HASH).build();
        var json = MAPPER.writeValueAsString(o);
        var r = MAPPER.readValue(json, PasswordAuthAccount.class);
        assertEquals(o.getId(), r.getId());
        assertEquals(o.getAuthAccountType(), r.getAuthAccountType());
        assertEquals(o.isActive(), r.isActive());
        assertEquals(o.getPasswordHash(), r.getPasswordHash());
    }
}
