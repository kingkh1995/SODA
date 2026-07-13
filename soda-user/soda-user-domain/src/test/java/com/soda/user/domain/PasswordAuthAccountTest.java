package com.soda.user.domain;

import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.RawCredential;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PasswordAuthAccount} 单元测试。
 */
@DisplayName("PasswordAuthAccount")
class PasswordAuthAccountTest {

    private static final PasswordAuthAccountId ID = PasswordAuthAccountId.from(new UserId(1L));
    private static final CredentialHash HASH = new CredentialHash("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    private static final CredentialHasher STUB = new CredentialHasher() {
        @Override
        public CredentialHash hash(RawCredential credential) {
            return HASH;
        }

        @Override
        public boolean matches(RawCredential credential, CredentialHash hash) {
            return "secret123".equals(credential.internalValue());
        }
    };

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("构造时设置 ID 和密码哈希")
        void should_setIdAndHash_when_constructed() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            assertThat(a.getId()).isEqualTo(ID);
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }

        @Test
        @DisplayName("CreateBuilder 设置默认值")
        void should_setDefaults_when_usingCreateBuilder() {
            var a = PasswordAuthAccount.createBuilder().userId(new UserId(1L)).passwordHash(HASH).build();
            assertThat(a.getId()).isEqualTo(PasswordAuthAccountId.of("P:1"));
            assertThat(a.isActive()).isTrue();
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }

        @Test
        @DisplayName("RestoreBuilder 恢复所有字段")
        void should_restoreAllFields_when_usingRestoreBuilder() {
            var a = PasswordAuthAccount.restoreBuilder().id(ID).active(Active.FALSE).passwordHash(HASH).build();
            assertThat(a.getId()).isEqualTo(ID);
            assertThat(a.isActive()).isFalse();
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }
    }

    @Nested
    @DisplayName("认证")
    class Authentication {

        @Test
        @DisplayName("返回 P 类型")
        void should_returnTypeP_when_getAuthAccountType() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            assertThat(a.getAuthAccountType()).isEqualTo(AuthAccountType.P);
        }

        @Test
        @DisplayName("正确密码验证通过")
        void should_verifyTrue_when_correctPassword() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            assertThat(a.verify(new RawCredential("secret123"), STUB)).isTrue();
        }

        @Test
        @DisplayName("错误密码验证失败")
        void should_verifyFalse_when_wrongPassword() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            assertThat(a.verify(new RawCredential("wrong"), STUB)).isFalse();
        }

        @Test
        @DisplayName("更改密码更新哈希")
        void should_updateHash_when_changePassword() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            a.changePassword(new RawCredential("x"), STUB);
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }
    }

    @Nested
    @DisplayName("状态")
    class Status {

        @Test
        @DisplayName("Active.TRUE 时启用")
        void should_beActive_when_activeIsTrue() {
            assertThat(new PasswordAuthAccount(ID, Active.TRUE, HASH).isActive()).isTrue();
        }

        @Test
        @DisplayName("Active.FALSE 时禁用")
        void should_beInactive_when_activeIsFalse() {
            assertThat(new PasswordAuthAccount(ID, Active.FALSE, HASH).isActive()).isFalse();
        }

        @Test
        @DisplayName("停用后设置未激活")
        void should_setInactive_when_deactivate() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            a.deactivate();
            assertThat(a.isActive()).isFalse();
        }

        @Test
        @DisplayName("重新激活恢复启用状态")
        void should_restoreActive_when_reactivate() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            a.deactivate();
            a.activate();
            assertThat(a.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip")
        void should_serializeDeserialize_when_jackson() throws Exception {
            var o = PasswordAuthAccount.createBuilder().userId(new UserId(1L)).passwordHash(HASH).build();
            var json = MAPPER.writeValueAsString(o);
            var r = MAPPER.readValue(json, PasswordAuthAccount.class);
            assertThat(r.getId()).isEqualTo(o.getId());
            assertThat(r.getAuthAccountType()).isEqualTo(o.getAuthAccountType());
            assertThat(r.isActive()).isEqualTo(o.isActive());
            assertThat(r.getPasswordHash()).isEqualTo(o.getPasswordHash());
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同字段相等，不同字段不等")
        void should_beEqual_when_sameFields() {
            var same = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            var equal = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            var diffHash = new PasswordAuthAccount(ID, Active.TRUE, new CredentialHash("$2a$10$different"));
            var diffId = new PasswordAuthAccount(PasswordAuthAccountId.from(new UserId(2L)), Active.TRUE, HASH);
            assertThat(same).isEqualTo(equal);
            assertThat(same).isNotEqualTo(diffHash);
            assertThat(same).isNotEqualTo(diffId);
        }
    }

    @Nested
    @DisplayName("调试")
    class Debug {

        @Test
        @DisplayName("toString 包含类名")
        void should_containClassName_when_toString() {
            var a = new PasswordAuthAccount(ID, Active.TRUE, HASH);
            assertThat(a.toString()).contains("PasswordAuthAccount@");
        }
    }
}
