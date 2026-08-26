package com.soda.user.infrastructure.repository;

import com.soda.user.infrastructure.persistence.UserPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link UserRepository} 真库行为（H2 MODE=MySQL + Flyway V1，ADR-0022）——
 * {@code user} 表列映射与审计列、派生查询、唯一索引（uk_username/uk_mobile/uk_email）、
 * 乐观锁版本（{@code @Version} 冲突检测）。
 * 边际用途：SQL/方言/约束与派生查询的真库现实；映射语义归 convertor 包 UserConvertorTest。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserRepository 真库行为")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    /**
     * 必填列内联 fixture（username/mobile/email/sex/avatar/开关由用例覆写）。
     */
    private static UserPO user() {
        var po = new UserPO();
        po.setNickname("爱丽丝");
        po.setState("E");
        po.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRST");
        return po;
    }

    @Nested
    @DisplayName("映射正确性")
    class Mapping {

        @Test
        @DisplayName("全列 round-trip 一致；审计列由 auditing 填充、version 播种为 0")
        void should_roundTripAllColumns_when_saveAndReload() {
            var po = user();
            po.setUsername("alice");
            po.setMobile("13800138000");
            po.setEmail("alice@example.com");
            po.setSex("M");
            po.setAvatar("https://cdn.example.com/a.png");
            po.setSmsLoginEnabled(false);
            po.setEmailLoginEnabled(false);

            var saved = userRepository.saveAndFlush(po);
            em.clear();

            var reloaded = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getUsername()).isEqualTo("alice");
            assertThat(reloaded.getNickname()).isEqualTo("爱丽丝");
            assertThat(reloaded.getMobile()).isEqualTo("13800138000");
            assertThat(reloaded.getEmail()).isEqualTo("alice@example.com");
            assertThat(reloaded.getSex()).isEqualTo("M");
            assertThat(reloaded.getAvatar()).isEqualTo("https://cdn.example.com/a.png");
            assertThat(reloaded.getState()).isEqualTo("E");
            assertThat(reloaded.getPasswordHash()).isEqualTo(po.getPasswordHash());
            assertThat(reloaded.isSmsLoginEnabled()).isFalse();
            assertThat(reloaded.isEmailLoginEnabled()).isFalse();
            assertThat(reloaded.getVersion()).isZero();
            assertThat(reloaded.getCreatedDate()).isNotNull();
            assertThat(reloaded.getLastModifiedDate()).isNotNull();
        }

        @Test
        @DisplayName("更新保留 created_date、auditing 刷新 last_modified_date 并递增 version")
        void should_keepCreatedDate_andRefreshLastModified_when_update() {
            var saved = userRepository.saveAndFlush(user());
            em.clear();
            var createdAt = userRepository.findById(saved.getId()).orElseThrow().getCreatedDate();

            var managed = userRepository.findById(saved.getId()).orElseThrow();
            managed.setNickname("新昵称");
            managed.setLastModifiedDate(Instant.EPOCH);
            userRepository.saveAndFlush(managed);
            em.clear();

            var reloaded = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getNickname()).isEqualTo("新昵称");
            assertThat(reloaded.getCreatedDate()).isEqualTo(createdAt);
            assertThat(reloaded.getLastModifiedDate()).isNotEqualTo(Instant.EPOCH).isAfterOrEqualTo(createdAt);
            assertThat(reloaded.getVersion()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("派生查询")
    class DerivedQueries {

        @Test
        @DisplayName("findByUsername 命中且不串行其他用户")
        void should_returnRow_when_findByUsernameHits() {
            var alice = userRepository.saveAndFlush(user());
            alice.setUsername("alice");
            userRepository.saveAndFlush(alice);
            var bob = userRepository.saveAndFlush(user());
            bob.setUsername("bob");
            userRepository.saveAndFlush(bob);
            em.clear();

            assertThat(userRepository.findByUsername("bob")).hasValueSatisfying(
                    row -> assertThat(row.getId()).isEqualTo(bob.getId()));
        }

        @Test
        @DisplayName("findByUsername 未命中返回 empty")
        void should_returnEmpty_when_usernameAbsent() {
            userRepository.saveAndFlush(user());

            assertThat(userRepository.findByUsername("nobody")).isEmpty();
        }

        @Test
        @DisplayName("findByMobile 命中")
        void should_returnRow_when_findByMobileHits() {
            var saved = userRepository.saveAndFlush(user());
            saved.setMobile("13900139000");
            userRepository.saveAndFlush(saved);
            em.clear();

            assertThat(userRepository.findByMobile("13900139000")).hasValueSatisfying(
                    row -> assertThat(row.getId()).isEqualTo(saved.getId()));
        }

        @Test
        @DisplayName("findByEmail 命中")
        void should_returnRow_when_findByEmailHits() {
            var saved = userRepository.saveAndFlush(user());
            saved.setEmail("alice@example.com");
            userRepository.saveAndFlush(saved);
            em.clear();

            assertThat(userRepository.findByEmail("alice@example.com")).hasValueSatisfying(
                    row -> assertThat(row.getId()).isEqualTo(saved.getId()));
        }
    }

    @Nested
    @DisplayName("唯一约束")
    class UniqueConstraints {

        @Test
        @DisplayName("uk_username 拒绝重复用户名")
        void should_rejectDuplicateUsername() {
            var first = userRepository.saveAndFlush(user());
            first.setUsername("dup");
            userRepository.saveAndFlush(first);

            var second = user();
            second.setUsername("dup");
            assertThatThrownBy(() -> userRepository.saveAndFlush(second))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("uk_mobile 拒绝重复手机号")
        void should_rejectDuplicateMobile() {
            var first = userRepository.saveAndFlush(user());
            first.setMobile("13800138000");
            userRepository.saveAndFlush(first);

            var second = user();
            second.setMobile("13800138000");
            assertThatThrownBy(() -> userRepository.saveAndFlush(second))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("uk_email 拒绝重复邮箱")
        void should_rejectDuplicateEmail() {
            var first = userRepository.saveAndFlush(user());
            first.setEmail("alice@example.com");
            userRepository.saveAndFlush(first);

            var second = user();
            second.setEmail("alice@example.com");
            assertThatThrownBy(() -> userRepository.saveAndFlush(second))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("乐观锁")
    class OptimisticLock {

        @Test
        @DisplayName("陈旧版本写入被拒（version 不一致即失败），行保持权威状态")
        void should_rejectStaleWrite_when_versionMismatch() {
            var saved = userRepository.saveAndFlush(user());

            var winner = userRepository.findById(saved.getId()).orElseThrow();
            winner.setNickname("winner");
            userRepository.saveAndFlush(winner);
            em.clear();

            var stale = user();
            stale.setId(saved.getId());
            stale.setVersion(0);
            assertThatThrownBy(() -> userRepository.saveAndFlush(stale))
                    .isInstanceOf(ObjectOptimisticLockingFailureException.class);

            var row = userRepository.findById(saved.getId()).orElseThrow();
            assertThat(row.getVersion()).isEqualTo(1);
            assertThat(row.getNickname()).isEqualTo("winner");
        }
    }
}
