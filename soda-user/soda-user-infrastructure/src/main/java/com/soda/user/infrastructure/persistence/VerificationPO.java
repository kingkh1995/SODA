package com.soda.user.infrastructure.persistence;

import com.soda.component.infrastructure.persistence.AbstractAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * {@code verification} 表 JPA 实体 — 持久化模型，非领域对象。
 * <p>
 * 与领域 {@code Verification}（单类 + source/recipient 双概念，见 ADR-0026）双向
 * 转换见 {@link com.soda.user.infrastructure.convertor.VerificationConvertor}；restore 走
 * {@code channel} + {@code target} 两列（见 ADR-0026）——<b>channel 独立列</b>
 * （S/E 枚举短名）、target 为地址裸值，subject 为裸键（无类型前缀）。
 * <p>
 * {@code active_key} 为活跃槽位键（{@code source.compositeKey()} = scene:subject；I/P 且未过期占槽、
 * U 终态迁移清 NULL、过期 I/P 行惰性 DELETE 释放）——{@code uk_active_key} 唯一索引硬保证单活跃
 * （见 ADR-0025 活跃验证唯一性）。
 * <p>
 * {@code expire_at} 是绝对时间点（领域层为 {@link Instant}）：Hibernate 默认映射
 * {@code TIMESTAMP_UTC}，按 UTC 规范化读写（MySQL DATETIME 列无时区语义、存 UTC 字面值，
 * 见 MySQL 官方 13.2.2）。审计列（created_date/last_modified_date）经 {@link AbstractAuditable}
 * 由 Spring Data auditing 维护（@CreatedDate/@LastModifiedDate），应用恒填充，无 DB 默认值（见 ADR-0024 审计列）。
 */
@Entity
@Table(
        name = "verification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_active_key",
                columnNames = "active_key"),
        indexes = @Index(
                name = "idx_subject_scene_state_expire_at",
                columnList = "subject, scene, state, expire_at"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class VerificationPO extends AbstractAuditable<String> {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 120)
    private String subject;

    @Column(nullable = false, length = 4)
    private String scene;

    @Column(nullable = false, length = 1)
    private String state;

    @Column(nullable = false, length = 1)
    private String channel;

    @Column(nullable = false, length = 100)
    private String target;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "expire_at", nullable = false)
    private Instant expireAt;

    @Column(name = "active_key", length = 160)
    private @Nullable String activeKey;
}
