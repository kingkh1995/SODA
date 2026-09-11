package com.soda.user.domain;

import com.soda.component.domain.gateway.PasswordHasher;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.SecretValue;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static com.soda.user.domain.DomainTestUtil.MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PasswordAuthAccount} 单元测试。
 */
@DisplayName("PasswordAuthAccount")
class PasswordAuthAccountTest {

    private static final PasswordAuthAccountId ID = PasswordAuthAccountId.from(new UserId(1L));
    private static final PasswordHash HASH = PasswordHash.of("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    /**
     * 升级产物 —— 与 HASH 同尾异前缀（成本 12），专供透明升级断言区分新旧哈希。
     */
    private static final PasswordHash REHASHED_HASH =
            PasswordHash.of("$2a$12$" + HASH.value().substring(7));

    private static final PasswordHasher STUB = new PasswordHasher() {
        @Override
        public PasswordHash hash(SecretValue credential) {
            return HASH;
        }

        @Override
        public boolean verify(PasswordHash stored, SecretValue candidate) {
            return "secret123".equals(candidate.rawValue());
        }

        @Override
        public boolean needsRehash(PasswordHash stored) {
            return false;
        }
    };

    /**
     * needsRehash 恒真桩 —— hash 产出 {@link #REHASHED_HASH}，驱动透明升级路径。
     */
    private static final PasswordHasher REHASHING_STUB = new PasswordHasher() {
        @Override
        public PasswordHash hash(SecretValue credential) {
            return REHASHED_HASH;
        }

        @Override
        public boolean verify(PasswordHash stored, SecretValue candidate) {
            return "secret123".equals(candidate.rawValue());
        }

        @Override
        public boolean needsRehash(PasswordHash stored) {
            return true;
        }
    };

    @Nested
    @DisplayName("构造")
    class Construction {

        @Test
        @DisplayName("构造时设置 ID 和密码哈希")
        void should_setIdAndHash_when_constructed() {
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            assertThat(a.getId()).isEqualTo(ID);
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }

        @Test
        @DisplayName("CreateBuilder 设置默认值")
        void should_setDefaults_when_usingCreateBuilder() {
            var a = PasswordAuthAccount.createBuilder()
                    .passwordHash(HASH)
                    .build();
            assertThat(a.isIdentified()).isFalse();
            assertThat(a.isActive()).isTrue();
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }

        @Test
        @DisplayName("builder 恢复所有字段")
        void should_restoreAllFields_when_usingBuilder() {
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            assertThat(a.getId()).isEqualTo(ID);
            assertThat(a.isActive()).isTrue();
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }
    }

    @Nested
    @DisplayName("认证")
    class Authentication {

        @Test
        @DisplayName("返回 P 类型")
        void should_returnTypeP_when_getAccountType() {
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            assertThat(a.getAccountType()).isEqualTo(AuthAccountType.P);
        }

        @Test
        @DisplayName("正确密码验证通过")
        void should_verifyTrue_when_correctPassword() {
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            assertThat(a.verify(new SecretValue("secret123"), STUB)).isTrue();
        }

        @Test
        @DisplayName("错误密码验证失败")
        void should_verifyFalse_when_wrongPassword() {
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            assertThat(a.verify(new SecretValue("wrong"), STUB)).isFalse();
        }

        @Test
        @DisplayName("更改密码更新哈希")
        void should_updateHash_when_changePassword() {
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            a.changePassword(new SecretValue("x"), STUB);
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }
    }

    @Nested
    @DisplayName("登录透明升级（ADR-0033 注记 7）")
    class RehashOnLogin {

        private PasswordAuthAccount account() {
            return PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
        }

        @Test
        @DisplayName("需升级且密码正确 —— 以新哈希替换并返回 true")
        void should_rehash_when_matchedAndNeedsUpgrade() {
            var a = account();
            assertThat(a.verifyAndRehash(new SecretValue("secret123"), REHASHING_STUB)).isTrue();
            assertThat(a.getPasswordHash()).isEqualTo(REHASHED_HASH);
        }

        @Test
        @DisplayName("无需升级且密码正确 —— 哈希保持不变")
        void should_keepHash_when_matchedButCurrentCost() {
            var a = account();
            assertThat(a.verifyAndRehash(new SecretValue("secret123"), STUB)).isTrue();
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }

        @Test
        @DisplayName("密码错误 —— 返回 false 且哈希不变（安全不变量）")
        void should_keepHash_when_wrongCandidate() {
            var a = account();
            assertThat(a.verifyAndRehash(new SecretValue("wrong"), REHASHING_STUB)).isFalse();
            assertThat(a.getPasswordHash()).isEqualTo(HASH);
        }
    }

    @Nested
    @DisplayName("恒启用不变量（ADR-0004）")
    class AlwaysEnabledInvariant {

        @Test
        @DisplayName("Active.TRUE 恢复时启用")
        void should_beActive_when_activeIsTrue() {
            assertThat(PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build().isActive()).isTrue();
        }

        @Test
        @DisplayName("恢复路径拒绝 Active.FALSE（恒启用不变量）")
        void should_rejectRestore_when_activeIsFalse() {
            assertThatThrownBy(() -> PasswordAuthAccount.builder().id(ID).active(Active.FALSE).passwordHash(HASH).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("must equal 'Active[value=true]', got: 'Active[value=false]'");
        }

        @Test
        @DisplayName("deactivate 拒绝：密码账户不允许设置为禁用态")
        void should_reject_when_deactivate() {
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            assertThatThrownBy(a::deactivate)
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("序列化")
    class Serialization {

        @Test
        @DisplayName("Jackson round-trip")
        void should_serializeDeserialize_when_jackson() throws Exception {
            var o = PasswordAuthAccount.builder()
                    .id(ID)
                    .active(Active.TRUE)
                    .passwordHash(HASH)
                    .build();
            var json = MAPPER.writeValueAsString(o);
            var r = MAPPER.readValue(json, PasswordAuthAccount.class);
            assertThat(r).isEqualTo(o);
        }

        @Test
        @DisplayName("缺少 id 的 JSON 拒绝")
        void should_reject_when_missingId() {
            var json = """
                    {"active":true,"passwordHash":"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"}
                    """;
            assertThatThrownBy(() -> MAPPER.readValue(json, PasswordAuthAccount.class))
                    .isInstanceOf(JacksonException.class);
        }
    }

    @Nested
    @DisplayName("相等性")
    class Equality {

        @Test
        @DisplayName("相同字段相等，不同字段不等")
        void should_beEqual_when_sameFields() {
            var same = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            var equal = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            var diffHash = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(PasswordHash.of("$2a$10$different")).build();
            var diffId = PasswordAuthAccount.builder().id(PasswordAuthAccountId.from(new UserId(2L))).active(Active.TRUE).passwordHash(HASH).build();
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
            var a = PasswordAuthAccount.builder().id(ID).active(Active.TRUE).passwordHash(HASH).build();
            assertThat(a.toString()).contains("PasswordAuthAccount@");
        }
    }
}
