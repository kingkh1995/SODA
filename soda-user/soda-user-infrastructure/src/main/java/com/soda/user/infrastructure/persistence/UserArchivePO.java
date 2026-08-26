package com.soda.user.infrastructure.persistence;

import com.soda.component.infrastructure.persistence.AbstractPersistable;
import com.soda.user.infrastructure.gateway.persistence.UserGatewayImpl;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * {@code user_archive} 表 JPA 持久化模型 — 用户注销归档审计表（ADR-0023）。
 * <p>
 * D→R 注销迁移时由 {@link UserGatewayImpl#save} 同事务写入：捕获释放前的原键值
 * （username/mobile/email）快照，供审计/合规查询；{@code user} 表 R 行此后三键置空。
 * 纯审计存储（insert-only，不参与业务读写路径）；保存期限届满后由清除 job 处理
 * （个保法 19 条最短期限，见 ADR-0023）。
 * <p>
 * 时间戳语义：insert-only 表无 update 概念——单一 {@code archive_time}（{@code @CreatedDate}
 * 由 Spring Data auditing 填充；归档时刻 = 行创建时刻），不复用审计列对
 * （created_date/last_modified_date），故继承 {@link AbstractPersistable}（isNew=id==null 判定，
 * ADR-0024）而非审计基类。
 * 主键 = 原用户 ID（表名 {@code user_archive} 已含 user 语义，列名即 {@code id}；
 * 1:1——注销是吸收态终态，每用户至多一条归档记录）。
 * id 恒有 → isNew=false → save 恒走 merge（insert 多一次 PK SELECT，1:1 行低频可接受，ADR-0024）。
 */
@Entity
@Table(name = "`user_archive`")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@EntityListeners(AuditingEntityListener.class)
public class UserArchivePO extends AbstractPersistable<Long> {

    @Id
    private Long id;

    @Column(nullable = false, length = 30)
    private String username;

    @Column(length = 20)
    private @Nullable String mobile;

    @Column(length = 100)
    private @Nullable String email;

    /**
     * 归档时刻（绝对时间点，{@link Instant} + Hibernate TIMESTAMP_UTC，仓库约定；
     * 由 Spring Data auditing 填充（@CreatedDate），时间源 Instant.now()）。
     */
    @CreatedDate
    @Column(name = "archive_time", nullable = false, updatable = false)
    private Instant archiveTime;
}
