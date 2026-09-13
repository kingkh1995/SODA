package com.soda.component.domain;

/**
 * 所有领域原语（Domain Primitive）的根标记接口。
 * <p>
 * 领域原语是不可变的值，承载领域含义，例如：标识符、金额、数量、业务编码等。
 * 所有领域原语必须：
 * <ul>
 *   <li><b>不可变</b> — 所有字段均为 {@code final}</li>
 *   <li><b>自校验</b> — 构造函数中完成校验，非法状态不可表示</li>
 *   <li><b>value-based identity</b> — {@link Object#equals(Object)}/{@link Object#hashCode()} 基于规范值字段</li>
 * </ul>
 * <p>
 * DP 禁止实现 {@link java.io.Serializable}——JSON 是 DP 的唯一线上契约（见 dp-json-conventions §5）。
 * 是否实现 {@link Comparable} 按业务语义决定（只在有自然顺序时实现）。
 * <p>
 * 参考 kk-ddd 的 {@code Type} 接口设计。
 *
 * @see Identifier
 */
public interface Type {
}
