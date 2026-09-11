package com.soda.component.domain;

/**
 * 可标识的领域对象标记接口。
 * <p>
 * 提供身份标识的查询契约。所有 Entity 和 Aggregate 必须实现此接口。
 * <p>
 * {@link #getId()} 返回 {@code @NonNull}；未标识时（仅创建瞬态）抛 {@link NullPointerException}。
 * 调用方须保证在持久化后使用，或通过 {@link #isIdentified()} 先做窄化守卫。
 *
 * @param <ID> 标识符类型
 * @see Identifier
 * @see Entity
 */
public interface Identifiable<ID extends Identifier<?>> {

    /**
     * 返回该领域对象的标识符。
     * <p>
     * 未标识时（仅创建瞬态）抛 {@link NullPointerException}（防御编程：调用方 bug，异常类型即语义）。
     * 持久化后由 {@code assignId} 填补，此后恒非空。
     */
    ID getId();

    /**
     * 是否已分配标识符。
     * <p>
     * 创建瞬态（服务端生成路径）为 false，持久化后由 {@code assignId} 填补为 true。
     */
    boolean isIdentified();
}
