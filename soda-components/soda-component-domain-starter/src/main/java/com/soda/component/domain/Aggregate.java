package com.soda.component.domain;

/**
 * 聚合根的抽象基类。
 * <p>
 * 聚合根是聚合一致性边界内的顶层实体，负责保证聚合内部的所有不变量不被破坏。
 * 对聚合的所有操作都必须通过聚合根进行。
 *
 * @param <ID> 聚合根标识符类型
 */
public abstract class Aggregate<ID extends Identifier<?>> extends Entity<ID> {

    protected Aggregate(ID id) {
        super(id);
    }
}
