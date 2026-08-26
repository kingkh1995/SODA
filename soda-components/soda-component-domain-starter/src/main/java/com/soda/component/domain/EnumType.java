package com.soda.component.domain;

/**
 * 枚举类型的契约接口。
 * <p>
 * 业务模块中所有业务枚举需实现此接口，统一：
 * <ul>
 *   <li><b>英文描述</b> — {@link #desc()} 返回辅助解释的英文描述字符串</li>
 *   <li><b>领域原语</b> — 继承 {@link Type}，所有业务枚举同时也是 Domain Primitive（ADR-0005）</li>
 * </ul>
 * 序列化：Jackson 对枚举原生输出 {@code name()} 短名（与持久化短名一致，见 ADR-0005）；
 * 反序列化入口由各 enum 自行提供 {@code @JsonCreator of(String)}（显式声明，不依赖推断）。
 * <p>
 * 与字面量家族（{@link StringLiteralType} 等）平行：枚举是封闭常量集（常量自身即值），
 * 不包装字面量、不参与家族契约（见 ADR-0028）。
 *
 * @see Type
 * @see Identifier
 * @see StringLiteralType
 */
public interface EnumType extends Type {

    /**
     * 返回英文描述，辅助解释枚举含义。
     *
     * @return 描述字符串
     */
    String desc();
}
