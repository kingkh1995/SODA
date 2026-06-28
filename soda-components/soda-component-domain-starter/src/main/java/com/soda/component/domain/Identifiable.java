package com.soda.component.domain;

import org.jspecify.annotations.Nullable;

/**
 * 可标识的领域对象标记接口。
 * <p>
 * 提供身份标识的查询契约。所有 Entity 和 Aggregate 必须实现此接口。
 *
 * @param <ID> 标识符类型
 * @see Identifier
 * @see Entity
 */
public interface Identifiable<ID extends Identifier<?>> {

    /**
     * 返回该领域对象的标识符。
     * <p>
     * 可能为 {@code null}（DB 自增场景，未持久化前）。
     */
    @Nullable ID getId();

    /**
     * 是否已分配标识符。
     */
    default boolean isIdentified() {
        return getId() != null;
    }
}
