package com.soda.component.infrastructure.persistence;

import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * 审计列读写契约 — 与 Spring Data {@code org.springframework.data.domain.Auditable} 对齐：
 * 接口名、字段/方法词表（{@code createdDate}/{@code lastModifiedDate}）
 * 均同 Spring；列名 {@code created_date}/{@code last_modified_date}（字段名 = 列名，
 * 仅形式差异）；返回类型 {@link Instant}（绝对时间点，仓库约定，见
 * {@link AbstractAuditable}）。
 * <p>
 * 与 Spring 接口的形态分歧（刻意）：Spring 的 {@code Auditable<U, ID, T>} 捆绑
 * {@code getCreatedBy/setCreatedBy/getLastModifiedBy/setLastModifiedBy}（操作人审计）与
 * {@code Optional} 访问器——本仓库未接入操作人身份（无 {@code @CreatedBy}/{@code @LastModifiedBy}），
 * 且审计列 DB 非空（值恒存在，无需 Optional），故只保留非 Optional 的
 * {@code getCreatedDate()/getLastModifiedDate()} 访问器。
 * <p>
 * 实现见 {@link AbstractAuditable}（{@code @CreatedDate}/{@code @LastModifiedDate}
 * 由 Spring Data auditing 填充，时间源为 {@code Instant.now()} 的 {@code DateTimeProvider}）。
 *
 * @param <ID> 实体标识符类型
 * @see AbstractAuditable
 */
public interface Auditable<ID> extends Persistable<ID> {

    Instant getCreatedDate();

    Instant getLastModifiedDate();
}
