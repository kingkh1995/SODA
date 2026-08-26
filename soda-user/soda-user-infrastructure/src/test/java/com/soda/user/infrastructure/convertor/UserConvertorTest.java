package com.soda.user.infrastructure.convertor;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.Version;
import com.soda.user.domain.EmailAuthAccount;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.SmsAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link UserConvertor} 双向转换单测（COLA convertor 独立类后转换逻辑可独立验证）。
 * <p>边际用途：PO↔领域双向映射语义（纯函数层）；持久化现实归 repository 切片。
 */
@DisplayName("UserConvertor 双向转换")
class UserConvertorTest {

    @Test
    @DisplayName("toPersistence → toDomain 往返：字段、派生账户、登录开关、version 完整")
    void should_roundTrip() {
        var user = User.createBuilder()
                .username(new Username("alice"))
                .nickname(new Nickname("Alice"))
                .mobile(Mobile.of("13900139000"))
                .passwordHash(PasswordHash.of("$2a$10$hash"))
                .build();
        user.assignId(new com.soda.user.domain.types.UserId(42L));

        var entity = UserConvertor.toPersistence(user);
        assertThat(entity.getId()).isEqualTo(42L);
        assertThat(entity.getUsername()).isEqualTo("alice");
        assertThat(entity.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(entity.isSmsLoginEnabled()).isTrue();
        assertThat(entity.isEmailLoginEnabled()).isTrue();

        var restored = UserConvertor.toDomain(entity);
        assertThat(restored.getId()).isEqualTo(user.getId());
        assertThat(restored.getUsername()).isEqualTo(user.getUsername());
        assertThat(restored.getNickname()).isEqualTo(user.getNickname());
        assertThat(restored.getMobile()).contains(Mobile.of("13900139000"));
        assertThat(restored.getAccounts()).hasSize(1);
        assertThat(restored.getAccounts().get(0)).isInstanceOf(SmsAuthAccount.class);
        assertThat(restored.getPasswordAccount().getPasswordHash())
                .isEqualTo(PasswordHash.of("$2a$10$hash"));
        assertThat(restored.getState()).isEqualTo(UserState.E);
        assertThat(restored.getVersion().value()).isZero();
    }

    @Test
    @DisplayName("登录开关：账户 active=false → 列 false → 恢复 active=false")
    void should_preserveLoginSwitch() {
        var user = User.createBuilder()
                .username(new Username("bobby"))
                .nickname(new Nickname("Bobby"))
                .mobile(Mobile.of("13900139001"))
                .passwordHash(PasswordHash.of("$2a$10$hash"))
                .build();
        user.assignId(new com.soda.user.domain.types.UserId(43L));
        var sms = (SmsAuthAccount) user.getAccounts().get(0);
        sms.deactivate();

        var entity = UserConvertor.toPersistence(user);
        assertThat(entity.isSmsLoginEnabled()).isFalse();

        var restored = UserConvertor.toDomain(entity);
        assertThat(((SmsAuthAccount) restored.getAccounts().get(0)).isActive()).isFalse();
    }

    @Test
    @DisplayName("邮箱账户：email 非空派生 EmailAuthAccount，开关独立")
    void should_deriveEmailAccount() {
        var user = User.createBuilder()
                .username(new Username("carol"))
                .nickname(new Nickname("Carol"))
                .email(Email.of("carol@test.com"))
                .passwordHash(PasswordHash.of("$2a$10$hash"))
                .build();
        user.assignId(new com.soda.user.domain.types.UserId(44L));

        var entity = UserConvertor.toPersistence(user);
        assertThat(entity.getEmail()).isEqualTo("carol@test.com");

        var restored = UserConvertor.toDomain(entity);
        assertThat(restored.getAccounts()).hasSize(1);
        assertThat(restored.getAccounts().get(0)).isInstanceOf(EmailAuthAccount.class);
        assertThat(((EmailAuthAccount) restored.getAccounts().get(0)).isActive()).isTrue();
    }

    @Test
    @DisplayName("version 随持久化数据流转：领域 version 原样带入列")
    void should_carryVersion() {
        var user = User.createBuilder()
                .username(new Username("dave"))
                .nickname(new Nickname("Dave"))
                .passwordHash(PasswordHash.of("$2a$10$hash"))
                .build();
        // 恢复路径 version 由基础设施层递增后回填（JPA @Version 更新 entity），
        // convertor 只负责原样搬运——这里验证创建路径 INITIAL=0
        var entity = UserConvertor.toPersistence(user);
        assertThat(entity.getVersion()).isZero();
    }

    @Test
    @DisplayName("注销终态恢复：username 列空 → 领域 Username.REMOVED（键释放，ADR-0023）")
    void should_restoreRemovedUsername_when_keyReleased() {
        var user = User.builder()
                .id(new UserId(43L))
                .version(Version.of(1))
                .username(Username.REMOVED)
                .nickname(new Nickname("Removed_User"))
                .state(UserState.R)
                .passwordAccount(PasswordAuthAccount.builder()
                        .id(PasswordAuthAccountId.from(new UserId(43L)))
                        .active(Active.TRUE)
                        .passwordHash(PasswordHash.of("$2a$10$hash"))
                        .build())
                .build();

        var entity = UserConvertor.toPersistence(user);
        // 模拟注销迁移后键置空（实际由 UserGatewayImpl.save 的 D→R 分支处理）
        entity.setUsername(null);
        entity.setMobile(null);
        entity.setEmail(null);

        var restored = UserConvertor.toDomain(entity);
        assertThat(restored.getState()).isEqualTo(UserState.R);
        assertThat(restored.getUsername()).isEqualTo(Username.REMOVED);
        assertThat(restored.getMobile()).isEmpty();
    }
}
