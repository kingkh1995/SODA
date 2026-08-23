package com.soda.component.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 双精度字面量 DP 契约（2026-08-16，见 ADR-0028）— 单属性、包装一个不可变 {@code double} 字面量，
 * 暴露 {@link #value()} 裸值（原语，免装箱/拆箱）。当前无实现类，契约就位待业务需求。
 * <p>
 * 与 {@link StringLiteralType}/{@link LongLiteralType}/{@link IntLiteralType}/{@link BooleanLiteralType}
 * 平行（IntSupplier 式，互不关联、无共享根）。{@link JsonValue} 继承自本接口——实现类获得标量 JSON
 * 序列化与反序列化（Jackson 3.1.4 实证）。
 *
 * @see Type
 * @see StringLiteralType
 */
public interface DoubleLiteralType extends Type {

    /**
     * 底层字面量裸值。
     */
    @JsonValue
    double value();
}
