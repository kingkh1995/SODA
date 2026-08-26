package com.soda.user.infrastructure.repository;

import com.soda.user.infrastructure.persistence.VerificationPO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link VerificationRepository} 真库行为（H2 MODE=MySQL + Flyway V1，ADR-0022）——
 * {@code verification} 表列映射与审计列、{@code uk_active_key} 唯一索引、槽位预检 /
 * 惰性腾槽 / 消费反查三条查询契约（ADR-0026）。DATETIME 列秒级精度（对齐真 MySQL 默认），
 * 时刻 fixture 一律截断到秒。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("VerificationRepository 真库行为")
class VerificationRepositoryTest {

    private static final String KEY = "UCC:42";
    private static final String OTHER_KEY = "UCC:43";
    private static final Instant FUTURE = Instant.now().plusSeconds(600).truncatedTo(ChronoUnit.SECONDS);
    private static final Instant PAST = Instant.now().minusSeconds(600).truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private VerificationRepository verificationRepository;

    /**
     * 行内联 fixture：channel=S、target 固定手机号，其余由参数表达。
     * 注意：active_key 非空的行全表至多一行（uk_active_key），铺底数据须遵守。
     */
    private static VerificationPO row(String id, String scene, String subject, String state,
                                      String code, Instant expireAt, String activeKey) {
        var e = new VerificationPO();
        e.setId(id);
        e.setScene(scene);
        e.setSubject(subject);
        e.setState(state);
        e.setChannel("S");
        e.setTarget("13800138000");
        e.setCode(code);
        e.setExpireAt(expireAt);
        e.setActiveKey(activeKey);
        return e;
    }

    @Nested
    @DisplayName("映射正确性")
    class Mapping {

        @Test
        @DisplayName("全列 round-trip 一致；expire_at 按绝对时间点还原；审计列自动填充")
        void should_roundTripAllColumns_when_saveAndReload() {
            var po = row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "P", "123456", FUTURE, KEY);

            var saved = verificationRepository.saveAndFlush(po);
            em.clear();

            var reloaded = verificationRepository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getId()).isEqualTo(saved.getId());
            assertThat(reloaded.getScene()).isEqualTo("UCC");
            assertThat(reloaded.getSubject()).isEqualTo("42");
            assertThat(reloaded.getState()).isEqualTo("P");
            assertThat(reloaded.getChannel()).isEqualTo("S");
            assertThat(reloaded.getTarget()).isEqualTo("13800138000");
            assertThat(reloaded.getCode()).isEqualTo("123456");
            assertThat(reloaded.getExpireAt()).isEqualTo(FUTURE);
            assertThat(reloaded.getActiveKey()).isEqualTo(KEY);
            assertThat(reloaded.getCreatedDate()).isNotNull();
            assertThat(reloaded.getLastModifiedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("uk_active_key 唯一索引")
    class UniqueActiveKey {

        @Test
        @DisplayName("同活跃键第二行被拒——单活跃硬保证")
        void should_rejectSecondRow_when_sameActiveKey() {
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "P", "111111", FUTURE, KEY));

            assertThatThrownBy(() -> verificationRepository.saveAndFlush(
                    row("0b9e6c5e-2222-4a22-9f22-bbbbbbbbbb2", "UCC", "42", "I", "222222", PAST, KEY)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("active_key 为 NULL 的多行共存——终态不参与唯一")
        void should_allowManyRows_when_activeKeyNull() {
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "U", "111111", PAST, null));
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-2222-4a22-9f22-bbbbbbbbbb2", "UCC", "42", "U", "222222", PAST, null));

            assertThat(verificationRepository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("槽位预检 existsByActiveKeyAndExpireAtAfter")
    class SlotProbe {

        @Test
        @DisplayName("未过期活跃行占槽")
        void should_reportOccupied_when_liveRowExists() {
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "P", "111111", FUTURE, KEY));

            assertThat(verificationRepository.existsByActiveKeyAndExpireAtAfter(KEY, Instant.now())).isTrue();
        }

        @Test
        @DisplayName("仅剩过期行视为空闲——expire_at 残余过滤")
        void should_reportFree_when_onlyExpiredRowRemains() {
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "P", "111111", PAST, KEY));

            assertThat(verificationRepository.existsByActiveKeyAndExpireAtAfter(KEY, Instant.now())).isFalse();
        }
    }

    @Nested
    @DisplayName("惰性腾槽 deleteExpiredByActiveKey")
    class LazyEvict {

        @Test
        @DisplayName("只删同键过期行——其他键的过期行与存活行不受影响，返回删除数")
        void should_deleteOnlyExpiredRowsOfSameKey() {
            var expiredSameKey =
                    row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "P", "111111", PAST, KEY);
            var liveOtherKey =
                    row("0b9e6c5e-2222-4a22-9f22-bbbbbbbbbb2", "UCC", "43", "P", "222222", FUTURE, OTHER_KEY);
            var expiredNullKey =
                    row("0b9e6c5e-3333-4a33-9f33-cccccccccc3", "UCC", "44", "U", "333333", PAST, null);
            verificationRepository.saveAndFlush(expiredSameKey);
            verificationRepository.saveAndFlush(liveOtherKey);
            verificationRepository.saveAndFlush(expiredNullKey);
            em.clear();

            var deleted = verificationRepository.deleteExpiredByActiveKey(KEY, Instant.now());
            em.clear();

            assertThat(deleted).isEqualTo(1);
            assertThat(verificationRepository.findById(expiredSameKey.getId())).isEmpty();
            assertThat(verificationRepository.findById(liveOtherKey.getId())).isPresent();
            assertThat(verificationRepository.findById(expiredNullKey.getId())).isPresent();
        }

        @Test
        @DisplayName("无匹配行时删除数为 0")
        void should_deleteNothing_when_keyUnknown() {
            var deleted = verificationRepository.deleteExpiredByActiveKey(KEY, Instant.now());

            assertThat(deleted).isZero();
        }
    }

    @Nested
    @DisplayName("消费反查 findFirst…OrderByExpireAtDesc")
    class LatestLookup {

        /**
         * 现实约束：同一 (subject, scene) 下非空 active_key 至多一行（uk_active_key），
         * 故候选集内合法共存的多行只能来自终态 U（NULL 键）或其他主体。
         */
        @Test
        @DisplayName("状态过滤下取未过期一行——更晚的终态行与其他主体不干扰")
        void should_returnLiveRow_ignoringLaterTerminal_andOtherSubjects() {
            var livePending =
                    row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "P", "111111", FUTURE, KEY);
            verificationRepository.saveAndFlush(livePending);
            // 同主体更晚的终态行（NULL 键合法共存）：不在候选状态 [I,P] 内
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-2222-4a22-9f22-bbbbbbbbbb2", "UCC", "42", "U", "222222",
                            FUTURE.plusSeconds(1800), null));
            // 其他主体的更晚 P 行：subject 不匹配
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-3333-4a33-9f33-cccccccccc3", "UCC", "43", "P", "333333",
                            FUTURE.plusSeconds(3600), OTHER_KEY));
            em.clear();

            var found = verificationRepository.findFirstBySubjectAndSceneAndStateInAndExpireAtAfterOrderByExpireAtDesc(
                    "42", "UCC", List.of("I", "P"), Instant.now());

            assertThat(found).hasValueSatisfying(row ->
                    assertThat(row.getId()).isEqualTo(livePending.getId()));
        }

        @Test
        @DisplayName("无匹配行返回 empty")
        void should_returnEmpty_when_noMatchingLiveRow() {
            verificationRepository.saveAndFlush(
                    row("0b9e6c5e-1111-4a11-9f11-aaaaaaaaaaa1", "UCC", "42", "U", "111111", PAST, null));

            assertThat(verificationRepository.findFirstBySubjectAndSceneAndStateInAndExpireAtAfterOrderByExpireAtDesc(
                    "42", "UCC", List.of("I", "P"), Instant.now())).isEmpty();
        }
    }
}
