package com.soda.component.domain;

/**
 * 单属性字面量 DP 契约（2026-08-16，见 ADR-0026 修订六）— 包装一个不可变字符串字面量
 * （手机号、邮箱、用户名、URL 等），暴露 {@link #value()} 裸值。
 * <p>
 * 与 {@link EnumType} 平行：枚举短名 DP 实现 {@code EnumType}，单值字面量 DP 实现本接口
 * （{@code Mobile}/{@code Email} 等）。
 * <p>
 * 用途：多态 DP 的类型参数约束（{@code VerificationRecipient<T extends LiteralType>}——
 * 泛型捕获后仍可直接调 {@code value()}，基础设施免密封 switch 判别具体类型）。
 *
 * @see Type
 * @see EnumType
 */
public interface LiteralType extends Type {

    /**
     * 底层字面量裸值（如 {@code Mobile.value()} = {@code "13800138000"}）。
     */
    String value();
}
