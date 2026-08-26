package com.soda.component.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 字符串字面量 DP 契约（见 ADR-0028）— 单属性、包装一个不可变 {@link String} 字面量
 * （手机号、邮箱、用户名、URL 等），暴露 {@link #value()} 裸值。
 * <p>
 * 与 {@link EnumType} 平行：枚举短名 DP 实现 {@code EnumType}（封闭常量集，常量自身即值，
 * 不包装字面量——非本接口子契约，见 ADR-0028）；单值字面量 DP
 * 实现本接口（{@code Mobile}/{@code Email}/{@code Uuid} 等）。字面量家族接口
 * （{@code StringLiteralType}/{@code LongLiteralType}/{@code IntLiteralType}/{@code BooleanLiteralType}/
 * {@code DoubleLiteralType}）互不关联（IntSupplier 式，无共享根），按字面量基本类型各归一族，
 * {@code value()} 返回原语以免装箱/拆箱。
 * <p>
 * Jackson 集成（ADR-0028）：{@link JsonValue} 声明在本接口的 {@code value()} 上，实现类继承后即获得
 * 标量 JSON 序列化与反序列化（Jackson 3.1.4 实证，双向）——record 实现类（组件即 {@code value}）与
 * 单 public 构造器 class 零 Jackson 代码；private 构造器 + 工厂（缓存/单例/解析）的 class 需自行保留
 * {@code @JsonCreator} 入口（构造入口不可继承，结构性必要）。
 * <p>
 * 用途：多态 DP 的类型参数约束（{@code VerificationRecipient<T extends StringLiteralType>}——
 * 泛型捕获后仍可直接调 {@code value()}，基础设施免密封 switch 判别具体类型）。
 *
 * @see Type
 * @see EnumType
 */
public interface StringLiteralType extends Type {

    /**
     * 底层字面量裸值（如 {@code Mobile.value()} = {@code "13800138000"}）。
     */
    @JsonValue
    String value();
}
