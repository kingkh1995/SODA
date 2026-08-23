package com.soda.user.infrastructure.gateway.persistence;

import com.soda.component.domain.types.Uuid;
import com.soda.user.domain.Verification;
import com.soda.user.domain.gateway.VerificationGateway;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import com.soda.user.infrastructure.convertor.VerificationConvertor;
import com.soda.user.infrastructure.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * {@link VerificationGateway} 的 JPA 实现 — 单表 {@code verification}。
 * <p>
 * 编排：查询 → {@link VerificationConvertor} 双向转换。领域↔持久化映射逻辑见 convertor。
 * <p>
 * 唯一性机制（见 ADR-0026）：槽位预检按 {@code active_key}（{@code uk_active_key} 索引）
 * 等值过滤 + expire_at 残余过滤；<b>过期 I/P 行惰性删除收敛进 {@code save}</b>（INSERT 前
 * 同事务腾槽——基础设施实现细节，非领域契约）；消费反查按 {@code (subject, scene, state, expire_at)}
 * 复合索引（{@code idx_subject_scene_state_expire_at}）。
 * <p>
 * 终态守卫（同 {@code UserGatewayImpl.save} 兜底，ADR-0023）：已终态（persisted state=U）行
 * 任何写入被拒绝——V→U 迁移合法写终态，行状态才是权威，拦截陈旧聚合/重复使用绕过路径。
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationGatewayImpl implements VerificationGateway {

    private final VerificationRepository verificationRepository;

    // ─── EntityGateway：save / findById / findAllById ───

    @Override
    @Transactional
    public Uuid save(Verification verification) {
        // 客户端生成 id 聚合 save 前置：必须已标识（ADR-0024；无 id 即契约违反，fail-fast）
        // 防御编程：Objects.requireNonNull → NPE（调用方按契约调用，异常类型即语义）
        var id = Objects.requireNonNull(verification.getId());
        // 终态守卫（同 UserGatewayImpl.save 基础设施兜底，ADR-0023）：findById 仅为读取持久化态
        // ——merge 不需要既有行基线（save(toPersistence(...)) 对 id 有值走 em.merge，路由与乐观
        // 锁由框架内建；Verification 无版本字段，跨加载竞态由状态机单调幂等兜底，见 ADR-0024 修订；
        // 审计列由 auditing + updatable=false 自动处理，见 VerificationConvertor javadoc）。
        // 与 User 不同不 orElseThrow：Verification 客户端生成 id、创建即 save（首存无行 → INSERT
        // 是合法路径，见 UserAuthServiceImpl 创建流），行缺失放行。
        var persisted = verificationRepository.findById(id.value()).orElse(null);
        // 终态（吸收态 U）行不可写——领域守卫之外的任何写路径在此被拒（ADR-0023 兜底语义）。
        // 检查持久化行状态（非聚合状态）：V→U 迁移合法地把聚合写成终态 U，行状态才是权威——
        // 拦截「域状态与行状态不一致」的绕过路径（陈旧聚合/重复使用）——防御编程兜底：
        // 调用方按契约调用，异常类型 + 栈帧即语义，不携消息
        if (persisted != null && VerificationState.of(persisted.getState()).terminal()) {
            throw new IllegalStateException();
        }
        // 惰性腾槽（基础设施实现细节，2026-08-16 见 ADR-0026）：INSERT 前同事务删除
        // 同活跃键过期未用（I/P）行——仅新占槽（I/P）需要腾槽；终态（U）迁移清 NULL 无槽可腾
        // （V 内存瞬态不落库，不涉槽位）。
        // 过期垃圾行物理清理（不同于 ADR-0017「终态由 save 持久化」——终态（U）行保留（V 内存瞬态不落库））。
        if (verification.isInitialized() || verification.isPending()) {
            verificationRepository.deleteExpiredByActiveKey(
                    verification.getSource().compositeKey(), Instant.now());
        }
        // 单一转换：save(toPersistence(verification))——id 恒有 → isNew=false → merge 按行
        // 存在性统一路由（无行 INSERT、有行 detached 状态全量拷贝），乐观锁/审计由框架
        // 自动处理（ADR-0024 决策 1：路由由 isNew + merge 内建，网关零判别逻辑）
        var po = VerificationConvertor.toPersistence(verification);
        // 终态清理（2026-08-16 检视修订，见 ADR-0026 修订注记）：convertor 恒设 active_key，
        // 终态（U）在此清 NULL——V 为内存瞬态（verify→use 同事务，永不落库，见
        // CredentialChangeDomainService）非终态、不涉槽位释放；terminal() 仅覆盖 U，
        // 不参与唯一（active_key 仅 I/P 行非空，uk_active_key 硬保证单活跃），
        // 状态语义收敛进网关。
        if (verification.isTerminal()) {
            po.setActiveKey(null);
        }
        verificationRepository.save(po);
        return id;
    }

    @Override
    public Optional<Verification> findById(Uuid id) {
        return verificationRepository.findById(id.value()).map(VerificationConvertor::toDomain);
    }

    @Override
    public List<Verification> findAllById(Iterable<Uuid> ids) {
        var values = StreamSupport.stream(ids.spliterator(), false)
                .map(Uuid::value)
                .toList();
        var entities = verificationRepository.findAllById(values);
        return entities.stream().map(VerificationConvertor::toDomain).toList();
    }

    // ─── VerificationGateway 查询 ───

    @Override
    public boolean existsBySource(VerificationSource source) {
        return verificationRepository.existsByActiveKeyAndExpireAtAfter(
                source.compositeKey(), Instant.now());
    }

    @Override
    public Optional<Verification> findLatestBySourceAndStateIn(
            VerificationSource source, Collection<VerificationState> states) {
        return verificationRepository
                .findFirstBySubjectAndSceneAndStateInAndExpireAtAfterOrderByExpireAtDesc(
                        source.subject(), source.scene(),
                        states.stream().map(VerificationState::name).toList(),
                        Instant.now())
                .map(VerificationConvertor::toDomain);
    }
}
