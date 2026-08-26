package com.soda.component.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;

/**
 * 审计列 JPA 实体基类（{@code isNew} 判定见 {@link AbstractPersistable}）。
 * <p>
 * {@code isNew} 机制（ADR-0024 merge 全权委托）：基类 {@code isNew() = id == null}——
 * 服务端生成 id 的实体（{@link com.soda.user.infrastructure.persistence.UserPO}）创建路径
 * id==null → persist；客户端分配 id 的实体（{@link com.soda.user.infrastructure.persistence.VerificationPO}）
 * 恒已标识 → merge——insert/update 由 merge 按行存在性统一路由，乐观锁由 {@code @Version}
 * 自动校验。
 * <p>
 * 审计列：Spring Data 官方审计机制
 * ——{@code @CreatedDate}/{@code @LastModifiedDate} + {@code AuditingEntityListener}
 * （{@code @EnableJpaAuditing} 自动配置启用，见 {@link JpaAuditingAutoConfiguration}），
 * persist/update 时由 Spring Data 填充；字段/列名与 Spring {@link Auditable} 词表对齐
 * （{@code createdDate}/{@code lastModifiedDate} ↔ {@code created_date}/{@code last_modified_date}）。
 * 类型为 {@link Instant}（绝对时间点，仓库约定，与 VerificationPO.expireAt 同——
 * Hibernate 默认映射 {@code TIMESTAMP_UTC}，DATETIME 列存 UTC 字面值），时间源由
 * {@code DateTimeProvider} 提供（{@code Instant.now()}）。
 * {@code created_date} 不可更新（{@code updatable=false}）；列无 DB 默认值（{@code CURRENT_TIMESTAMP}
 * 按会话时区生成会与 UTC 字面值约定漂移，值由应用恒填充）。
 * setter 存在以供 JPA 与测试使用；convertor 更新路径<b>不</b>搬运审计列（{@code toPersistence}
 * 不构造，null 即可）——merge 全量拷贝不会覆盖审计列（见 ADR-0024；
 * {@code created_date} 由 {@code updatable=false} 保护、{@code last_modified_date} 由
 * {@code @PreUpdate} 刷新），setter 不破坏「值由 auditing 填充」的契约（监听器仍覆盖写）。
 * <p>
 * 命名：具体数据库模型类统一 {@code XxxPO}；本基类按所实现接口命名
 * （AbstractXxx implements Xxx 模式，见 ADR-0023）。
 * PO 字段名与 DB 列名一致（仅下划线/驼峰形式差异，由命名策略或显式 {@code @Column} 表达）；
 * 需履行框架契约（如 {@code Persistable.getId()}）时显式实现方法，而非为契约改字段名。
 *
 * @param <ID> 实体标识符类型
 * @see AbstractPersistable
 * @see Auditable
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractAuditable<ID extends Serializable> extends AbstractPersistable<ID>
        implements Auditable<ID> {

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    private Instant lastModifiedDate;
}
