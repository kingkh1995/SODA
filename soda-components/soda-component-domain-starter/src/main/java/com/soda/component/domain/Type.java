package com.soda.component.domain;

import java.io.Serializable;

/**
 * 所有领域原语（Domain Primitive）的根标记接口。
 * <p>
 * 领域原语是不可变的值，承载领域含义，例如：标识符、金额、数量、业务编码等。
 * 所有领域原语必须：
 * <ul>
 *   <li><b>不可变</b> — 所有字段均为 {@code final}</li>
 *   <li><b>自校验</b> — 构造函数中完成校验，非法状态不可表示</li>
 *   <li><b>可序列化</b> — 实现 {@link Serializable}，用于分布式 / CQRS 场景</li>
 *   <li><b>可比较</b> — 每个 DP 自行实现 {@link Comparable}{@code <Self>} 提供类型安全比较</li>
 * </ul>
 * <p>
 * 参考 kk-ddd 的 {@code Type} 接口设计。
 *
 * @see Identifier
 */
public interface Type extends Serializable {
}
