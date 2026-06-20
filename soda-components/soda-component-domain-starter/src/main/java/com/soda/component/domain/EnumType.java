package com.soda.component.domain;

import java.io.Serializable;

/**
 * 枚举类型的根标记接口。
 * <p>
 * 业务模块中所有业务枚举需实现此接口，统一：
 * <ul>
 *   <li><b>英文描述</b> — {@link #desc()} 返回辅助解释的英文描述字符串</li>
 *   <li><b>可序列化</b> — 实现 {@link Serializable}，用于分布式 / 领域事件场景</li>
 * </ul>
 * <p>
 * 枚举不实现 {@link Type} 接口 — 枚举是业务分类而非领域原语，使用独立的 {@link EnumType} 接口提供 {@link #desc()} 契约。
 * 所有业务模块枚举需一致实现此接口，确保统一提供 {@link #desc()}。
 *
 * @see Type
 * @see Identifier
 */
public interface EnumType extends Serializable {

    /**
     * 返回英文描述，辅助解释枚举含义。
     *
     * @return 描述字符串
     */
    String desc();
}
