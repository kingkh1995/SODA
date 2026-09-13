package com.soda.user.infrastructure.gateway.persistence;

import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.Uuid;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import com.soda.user.infrastructure.persistence.VerificationPO;
import com.soda.user.infrastructure.repository.VerificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link VerificationGatewayImpl} 真库行为（H2 MODE=MySQL + Flyway V1，ADR-0022）——
 * save 路由（首存 INSERT / P 态更新 / U 终态清 active_key）、INSERT 前惰性腾槽真删行、
 * 终态行兜底守卫、槽位预检与消费反查的领域侧派发（ADR-0023/0026）。DATETIME 列秒级
 * 精度（对齐真 MySQL 默认），时刻 fixture 一律截断到秒。
 * 边际用途：VerificationGatewayImpl 的 save 行为矩阵单源（路由/守卫/腾槽机制在此一层钉死）。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(VerificationGatewayImpl.class)
@DisplayName("VerificationGatewayImpl 真库行为")
class VerificationGatewayImplPersistenceTest {

    private static final VerificationSource SOURCE = new VerificationSource("UCC", "42");
    private static final VerificationSource OTHER_SOURCE = new VerificationSource("UCC", "43");
    private static final String MOBILE = "13800138000";
    private static final Instant FUTURE = Instant.now().plusSeconds(600).truncatedTo(ChronoUnit.SECONDS);
    private static final Instant PAST = Instant.now().minusSeconds(600).truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private VerificationGatewayImpl gateway;

    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private TestEntityManager em;

    /**
     * 领域聚合内联 fixture：短信通道、固定码与未过期时刻。
     */
    private static Verification verification(String id, VerificationSource source, VerificationState state) {
        return Verification.builder()
                .id(new Uuid(id))
                .source(source)
                .state(state)
                .code(new VerificationCode("123456", FUTURE))
                .recipient(new SmsRecipient(Mobile.of(MOBILE)))
                .build();
    }

    /**
     * 持久化行内联 fixture（直接以行状态铺底，模拟既有数据）；终态 U 行清 NULL 活跃键
     * （终态不占槽，ADR-0026）。
     */
    private static VerificationPO seedRow(String id, VerificationSource source, VerificationState state,
                                          Instant expireAt) {
        var e = new VerificationPO();
        e.setId(id);
        e.setScene(source.scene());
        e.setSubject(source.subject());
        e.setState(state.name());
        e.setChannel("S");
        e.setTarget(MOBILE);
        e.setCode("999999");
        e.setExpireAt(expireAt);
        e.setActiveKey(state == VerificationState.U ? null : source.compositeKey());
        return e;
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }

    @Nested
    @DisplayName("save 路由")
    class SaveRouting {

        @Test
        @DisplayName("首存 INSERT：active_key 占槽、列映射一致、审计列填充")
        void should_insertRow_when_firstSave() {
            var aggregate = verification(uuid(), SOURCE, VerificationState.I);

            var returned = gateway.save(aggregate);
            em.flush();
            em.clear();

            assertThat(returned).isEqualTo(aggregate.getId());
            var row = verificationRepository.findById(returned.value()).orElseThrow();
            assertThat(row.getActiveKey()).isEqualTo(SOURCE.compositeKey());
            assertThat(row.getState()).isEqualTo("I");
            assertThat(row.getChannel()).isEqualTo("S");
            assertThat(row.getTarget()).isEqualTo(MOBILE);
            assertThat(row.getCode()).isEqualTo("123456");
            assertThat(row.getExpireAt()).isEqualTo(FUTURE);
            assertThat(row.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("markSent 后再存：P 态落库且活跃键保留")
        void should_persistPending_when_markSentAndSaved() {
            var aggregate = verification(uuid(), SOURCE, VerificationState.I);
            gateway.save(aggregate);
            aggregate.markSent();
            gateway.save(aggregate);
            em.flush();
            em.clear();

            var row = verificationRepository.findById(aggregate.getId().value()).orElseThrow();
            assertThat(row.getState()).isEqualTo("P");
            assertThat(row.getActiveKey()).isEqualTo(SOURCE.compositeKey());
        }

        @Test
        @DisplayName("U 终态迁移：active_key 在库中清 NULL")
        void should_clearActiveKeyInDb_when_usedSaved() {
            var aggregate = verification(uuid(), SOURCE, VerificationState.V);
            aggregate.use();
            gateway.save(aggregate);
            em.flush();
            em.clear();

            var row = verificationRepository.findById(aggregate.getId().value()).orElseThrow();
            assertThat(row.getState()).isEqualTo("U");
            assertThat(row.getActiveKey()).isNull();
        }

        @Test
        @DisplayName("INSERT 前惰性腾槽：同键过期行物理删除，其他键存活行不受影响")
        void should_evictExpiredSlotRows_beforeInsert() {
            var expiredSameKey = seedRow(uuid(), SOURCE, VerificationState.P, PAST);
            var liveOtherKey = seedRow(uuid(), OTHER_SOURCE, VerificationState.P, FUTURE);
            verificationRepository.saveAndFlush(expiredSameKey);
            verificationRepository.saveAndFlush(liveOtherKey);
            em.clear();

            gateway.save(verification(uuid(), SOURCE, VerificationState.I));
            em.flush();
            em.clear();

            assertThat(verificationRepository.findById(expiredSameKey.getId())).isEmpty();
            assertThat(verificationRepository.findById(liveOtherKey.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("终态兜底守卫")
    class TerminalGuard {

        @Test
        @DisplayName("持久化行已为 U 时拒绝写入且行保持原状")
        void should_rejectSave_when_rowAlreadyTerminal() {
            var terminalId = uuid();
            verificationRepository.saveAndFlush(
                    seedRow(terminalId, SOURCE, VerificationState.U, FUTURE));
            em.clear();

            assertThatThrownBy(() -> gateway.save(verification(terminalId, SOURCE, VerificationState.P)))
                    .isInstanceOf(IllegalStateException.class);

            var row = verificationRepository.findById(terminalId).orElseThrow();
            assertThat(row.getState()).isEqualTo("U");
            assertThat(row.getCode()).isEqualTo("999999");
        }
    }

    @Nested
    @DisplayName("查询路径")
    class QueryPaths {

        @Test
        @DisplayName("存在未过期活跃行时槽位被占用")
        void should_reportOccupied_when_liveRowExists() {
            verificationRepository.saveAndFlush(seedRow(uuid(), SOURCE, VerificationState.P, FUTURE));

            assertThat(gateway.existsBySource(SOURCE)).isTrue();
        }

        @Test
        @DisplayName("仅剩过期行时槽位空闲")
        void should_reportFree_when_onlyExpiredRow() {
            verificationRepository.saveAndFlush(seedRow(uuid(), SOURCE, VerificationState.P, PAST));

            assertThat(gateway.existsBySource(SOURCE)).isFalse();
        }

        /**
         * 同一 source 下非空活跃键至多一行（uk_active_key），候选集内合法共存的更晚行
         * 只能是终态 U（NULL 键）或其他主体。
         */
        @Test
        @DisplayName("消费反查命中未过期 P 行并还原领域聚合——更晚的 U 行与其他主体不干扰")
        void should_loadLatestPendingMapped_ignoringLaterTerminal_andOtherSubjects() {
            var livePending = seedRow(uuid(), SOURCE, VerificationState.P, FUTURE);
            verificationRepository.saveAndFlush(livePending);
            // 同主体更晚的终态行：不在候选状态 [P] 内
            verificationRepository.saveAndFlush(
                    seedRow(uuid(), SOURCE, VerificationState.U, FUTURE.plusSeconds(1800)));
            // 其他主体的更晚 P 行：source 不匹配
            verificationRepository.saveAndFlush(
                    seedRow(uuid(), OTHER_SOURCE, VerificationState.P, FUTURE.plusSeconds(3600)));
            em.clear();

            var loaded = gateway.findLatestBySourceAndStateIn(SOURCE, List.of(VerificationState.P));

            assertThat(loaded).hasValueSatisfying(v -> {
                assertThat(v.getId()).isEqualTo(new Uuid(livePending.getId()));
                assertThat(v.getSource()).isEqualTo(SOURCE);
                assertThat(v.getState()).isEqualTo(VerificationState.P);
                assertThat(v.getRecipient()).isEqualTo(new SmsRecipient(Mobile.of(MOBILE)));
            });
        }
    }
}
