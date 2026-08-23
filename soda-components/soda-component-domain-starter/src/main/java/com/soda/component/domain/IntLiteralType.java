package com.soda.component.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 整型字面量 DP 契约（2026-08-16，见 ADR-0028）— 单属性、包装一个不可变 {@code int} 字面量
 * （{@code Fen}/{@code Version}/{@code PositiveInt} 等），暴露 {@link #value()} 裸值（原语，免装箱/拆箱）。
 * <p>
 * 与 {@link StringLiteralType}/{@link LongLiteralType}/{@link BooleanLiteralType}/{@link DoubleLiteralType}
 * 平行（IntSupplier 式，互不关联、无共享根）。{@link JsonValue} 继承自本接口——实现类获得标量 JSON
 * 序列化与反序列化（Jackson 3.1.4 实证），record 实现类零 Jackson 代码。
 *
 * @see Type
 * @see StringLiteralType
 */
public interface IntLiteralType extends Type {

    /**
     * 底层字面量裸值（如 {@code Fen.value()} = {@code 1500}）。
     */
    @JsonValue
    int value();
}
