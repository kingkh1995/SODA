package com.soda.user.infrastructure.repository;

import com.soda.user.infrastructure.persistence.VerificationPO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * {@code verification} 表 Spring Data JPA Repository — 基础设施内部接口。
 * <p>
 * 查询契约（派生查询规范，见 ADR-0026）：
 * <ul>
 *   <li>{@link #existsByActiveKeyAndExpireAtAfter} — 槽位预检（{@code uk_active_key} 等值 +
 *       expire_at 残余过滤；active_key 仅 I/P 行非空，等值即活跃语义；键 = {@code source.compositeKey()}
 *       = scene:subject）</li>
 *   <li>{@link #deleteExpiredByActiveKey} — 惰性腾槽（过期 I/P 行物理删除；显式批量
 *       {@code @Modifying}——派生 deleteBy 的实体级 remove 语义不保证同事务 INSERT 前落库）</li>
 *   <li>{@link #findFirstBySubjectAndSceneAndStateInAndExpireAtAfterOrderByExpireAtDesc} —
 *       消费反查（{@code idx_subject_scene_state_expire_at} 复合索引，expire_at 倒序限 1；
 *       subject 为裸键字符串）</li>
 * </ul>
 */
public interface VerificationRepository extends JpaRepository<VerificationPO, String> {

    boolean existsByActiveKeyAndExpireAtAfter(String activeKey, Instant after);

    /**
     * 惰性腾槽：删除该活跃键下过期未用（I/P）行。
     * <p>
     * {@code clearAutomatically}：批量删除绕过持久化上下文，清空一级缓存防同事务后续
     * INSERT/merge 读到旧状态。代价：清空后 merge 前多一次 SELECT（save 路径唯一额外查询，
     * 与 ADR-0024「同事务加载 ctx 命中零额外 SQL」的用户路径不同——验证 save 是客户端生成
     * id + 恒 merge，此处为惰性腾槽的显式取舍）。
     */
    @Modifying(clearAutomatically = true)
    @Query("delete from VerificationPO v where v.activeKey = :activeKey and v.expireAt < :before")
    long deleteExpiredByActiveKey(@Param("activeKey") String activeKey, @Param("before") Instant before);

    Optional<VerificationPO> findFirstBySubjectAndSceneAndStateInAndExpireAtAfterOrderByExpireAtDesc(
            String subject, String scene, Collection<String> states, Instant after);
}
