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
 *   <li>{@link #remove(Object)} — 同 {@code CrudRepository.delete(T)}</li>
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
     * 保存实体。
     * <p>
     * 自动判断 insert / update：若 entity 已标识 (isIdentified()) 执行更新，
     * 否则执行插入，并按生成策略返回 ID。
     * <p>
     * 客户端生成 ID（如 UUID）场景返回已有 ID。
     *
     * @param entity 待保存实体，非 null
     * @return 实体标识符，总为非 null
     */
    ID save(T entity);

    /**
     * 移除实体。
     * <p>
     * 需先通过 {@link #findById(Object)} 加载实体再进行移除，
     * 确保聚合不变量在删除前已被校验。
     *
     * @param entity 待移除实体，非 null
     */
    void remove(T entity);

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
