package com.soda.component.domain;

import java.util.List;
import java.util.Optional;

/**
 * 领域实体的持久化契约（防腐层接口）。
 * <p>
 * 所有持久化操作通过此接口抽象，实现层位于基础设施模块。
 * <p>
 * 方法取自 Spring Data JPA 命名约定：
 * <ul>
 *   <li>{@link #save(Object)} — 同 {@code CrudRepository.save(S)}</li>
 *   <li>{@link #findById(Object)} — 同 {@code CrudRepository.findById(ID)}</li>
 *   <li>{@link #findAllById(Iterable)} — 同 {@code CrudRepository.findAllById(Iterable)}</li>
 * </ul>
 *
 * @param <T>  实体类型，必须实现 {@link Entity}
 * @param <ID> 标识符类型，必须实现 {@link Identifier}
 * @see Gateway
 * @see Aggregate
 */
public interface EntityGateway<T extends Entity<ID>, ID extends Identifier<?>> extends Gateway {

    /**
     * 保存实体 — 统一入口（insert / put 全量更新 / 终态处理，见 ADR-0024）。
     * <p>
     * 全权委托 Spring Data JPA：PO 的 {@code isNew()} 为 {@code id == null} 判定——路由由
     * {@code repository.save(toPersistence(entity))} 内建：
     * <ul>
     *   <li>id==null（服务端生成 id 创建路径）→ persist：INSERT 新行，生成 ID 经
     *       {@code assignId} 回填并返回</li>
     *   <li>id 已标识（客户端生成 id 聚合、既有行更新）→ merge 按行存在性统一路由：
     *       无行 INSERT、有行 detached 状态全量拷贝（含 null——领域 null = 清空列）→ UPDATE</li>
     *   <li>领域终态 → 基础设施按聚合策略裁决（本框架：写终态保留行，无 DELETE 契约，
     *       见 ADR-0017/0023；remove 为演进路径）</li>
     * </ul>
     * 乐观锁：有 {@code @Version} 的聚合由 merge 自动校验（版本不一致 → 异常 → 事务整体回滚），
     * 网关不做手动比对。客户端生成 ID（UUID）的聚合：save 前置必须已标识（{@code isIdentified()}）。
     *
     * @param entity 待保存实体，非 null
     * @return 实体标识符，总为非 null
     */
    ID save(T entity);

    /**
     * 按 ID 查询实体。
     *
     * @param id 实体标识符，非 null
     * @return 包含实体的 Optional，不存在返回 {@link Optional#empty()}
     */
    Optional<T> findById(ID id);

    /**
     * 批量查询实体。
     *
     * @param ids 标识符集合，非 null，非 null 元素
     * @return 找到的实体列表；不存在结果时返回空列表，不返回 null
     */
    List<T> findAllById(Iterable<ID> ids);
}
