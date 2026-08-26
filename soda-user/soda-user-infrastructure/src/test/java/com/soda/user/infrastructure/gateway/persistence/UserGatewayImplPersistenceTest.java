package com.soda.user.infrastructure.gateway.persistence;

import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.Sex;
import com.soda.user.domain.SmsAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.infrastructure.persistence.UserPO;
import com.soda.user.infrastructure.repository.UserArchiveRepository;
import com.soda.user.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link UserGatewayImpl} 真库行为（H2 MODE=MySQL + Flyway V1，ADR-0022）——
 * 创建路径 id 回填与审计列、更新乐观锁递增、D→R 注销迁移（归档快照 + 键释放）、
 * 终态行兜底守卫、登录开关列 → 账户 active 派生映射（ADR-0004/0023/0024）。
 * 边际用途：save 路由守卫与归档/键释放的真库行为——User 写路径行为矩阵单源。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserGatewayImpl.class)
@DisplayName("UserGatewayImpl 真库行为")
class UserGatewayImplPersistenceTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRST";

    @Autowired
    private UserGatewayImpl gateway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserArchiveRepository userArchiveRepository;

    @Autowired
    private TestEntityManager em;

    /**
     * 领域聚合内联 fixture：全字段创建态（E），mobile/email 均绑定。
     */
    private static User newUser() {
        return User.createBuilder()
                .username(new Username("alice"))
                .nickname(new Nickname("爱丽丝"))
                .mobile(Mobile.of("13800138000"))
                .email(Email.of("alice@example.com"))
                .sex(Sex.M)
                .passwordHash(PasswordHash.of(HASH))
                .build();
    }

    @Nested
    @DisplayName("save 路由")
    class SaveRouting {

        @Test
        @DisplayName("创建路径：INSERT 后生成 id 并回填聚合，审计列填充")
        void should_insertAndAssignGeneratedId_when_saveNewUser() {
            var user = newUser();

            var id = gateway.save(user);
            em.flush();
            em.clear();

            assertThat(user.getId()).isEqualTo(id);
            var row = userRepository.findById(id.value()).orElseThrow();
            assertThat(row.getState()).isEqualTo("E");
            assertThat(row.getUsername()).isEqualTo("alice");
            assertThat(row.isSmsLoginEnabled()).isTrue();
            assertThat(row.getVersion()).isZero();
            assertThat(row.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("更新路径：merge 全量拷贝落库并递增版本")
        void should_updateColumns_andBumpVersion_when_existingSaved() {
            var id = gateway.save(newUser());
            em.flush();
            em.clear();

            var loaded = gateway.findById(id).orElseThrow();
            loaded.changeNickname(new Nickname("新昵称"));
            gateway.save(loaded);
            em.flush();
            em.clear();

            var row = userRepository.findById(id.value()).orElseThrow();
            assertThat(row.getNickname()).isEqualTo("新昵称");
            assertThat(row.getVersion()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("注销迁移（D→R）")
    class DeregisterMigration {

        @Test
        @DisplayName("归档表写入释放前原键快照，archive_time 由 auditing 填充")
        void should_archiveOriginalKeys_when_deregisterSaved() {
            var id = gateway.save(newUser());
            var loaded = gateway.findById(id).orElseThrow();
            loaded.disable();
            loaded.deregister();
            gateway.save(loaded);
            em.flush();
            em.clear();

            var archive = userArchiveRepository.findById(id.value()).orElseThrow();
            assertThat(archive.getUsername()).isEqualTo("alice");
            assertThat(archive.getMobile()).isEqualTo("13800138000");
            assertThat(archive.getEmail()).isEqualTo("alice@example.com");
            assertThat(archive.getArchiveTime()).isNotNull();
        }

        @Test
        @DisplayName("user 行三键置空；恢复为 REMOVED 默认名且无派生账户，按原键查不到")
        void should_releaseKeys_onDeregisteredRow() {
            var id = gateway.save(newUser());
            var loaded = gateway.findById(id).orElseThrow();
            loaded.disable();
            loaded.deregister();
            gateway.save(loaded);
            em.flush();
            em.clear();

            var restored = gateway.findById(id).orElseThrow();
            assertThat(restored.getState()).isEqualTo(UserState.R);
            assertThat(restored.getUsername()).isEqualTo(Username.REMOVED);
            assertThat(restored.getAccounts()).isEmpty();
            assertThat(userRepository.findByUsername("alice")).isEmpty();
            assertThat(userRepository.findByMobile("13800138000")).isEmpty();
        }
    }

    @Nested
    @DisplayName("终态兜底守卫")
    class TerminalGuard {

        @Test
        @DisplayName("持久化行已为 R 时拒绝任何写入且行不被改写")
        void should_rejectSave_when_rowAlreadyTerminal() {
            var id = gateway.save(newUser());
            var stale = gateway.findById(id).orElseThrow();
            stale.changeNickname(new Nickname("改名"));

            var current = gateway.findById(id).orElseThrow();
            current.disable();
            current.deregister();
            gateway.save(current);
            em.flush();
            em.clear();

            assertThatThrownBy(() -> gateway.save(stale))
                    .isInstanceOf(IllegalStateException.class);

            var row = userRepository.findById(id.value()).orElseThrow();
            assertThat(row.getState()).isEqualTo("R");
            assertThat(row.getNickname()).isEqualTo("爱丽丝");
        }
    }

    @Nested
    @DisplayName("账户派生映射")
    class AccountDerivation {

        @Test
        @DisplayName("sms_login_enabled=false 行恢复出 inactive 的短信账户")
        void should_deriveInactiveSmsAccount_when_loginFlagOff() {
            var po = new UserPO();
            po.setUsername("bobby");
            po.setNickname("鲍勃");
            po.setState("E");
            po.setPasswordHash(HASH);
            po.setMobile("13800138000");
            po.setSmsLoginEnabled(false);
            po.setEmailLoginEnabled(true);
            var saved = userRepository.saveAndFlush(po);
            em.clear();

            var loaded = gateway.findById(new UserId(saved.getId())).orElseThrow();

            assertThat(loaded.getAccounts()).singleElement().isInstanceOf(SmsAuthAccount.class);
            var sms = (SmsAuthAccount) loaded.getAccounts().getFirst();
            assertThat(sms.isActive()).isFalse();
        }
    }
}
