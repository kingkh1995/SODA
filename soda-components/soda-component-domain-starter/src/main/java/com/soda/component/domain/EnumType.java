package com.soda.component.domain;

/**
 * 枚举类型的根标记接口。
 * <p>
 * 业务模块中所有业务枚举需实现此接口，统一：
 * <ul>
 *   <li><b>英文描述</b> — {@link #desc()} 返回辅助解释的英文描述字符串</li>
 *   <li><b>领域原语</b> — 继承 {@link Type}，所有业务枚举同时也是 Domain Primitive</li>
 * </ul>
 * 枚举自带 {@code name()} 作为序列化值，Jackson 默认序列化为 {@code name()}。
 * 各 enum 还需自行提供 {@code @JsonCreator of(String)} 入口。
 *
 * @see Type
 * @see Identifier
 */
public interface EnumType extends Type {

    /**
     * 返回英文描述，辅助解释枚举含义。
     *
     * @return 描述字符串
     */
    String desc();
}
