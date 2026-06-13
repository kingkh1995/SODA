package com.soda.component.domain;

/**
 * 领域标识符的标记接口。
 * <p>
 * 标识符是 <strong>不可变</strong> 的领域原语，在限界上下文内唯一标识一个实体。
 * 每个具体的标识符类型必须：
 * <ul>
 *   <li>不可变 — 优先使用 {@code record}（自动提供 {@code equals}/{@code hashCode}/{@code toString}）</li>
 *   <li>自校验 — 紧凑构造中完成值验证</li>
 *   <li>可序列化（继承自 {@link Type}）</li>
 *   <li>可比较，用于排序和有序集合</li>
 * </ul>
 * <p>
 * 由于 {@code Identifier<T>} 中 {@code T extends Comparable<T>}，
 * 使用示例（record 风格，JDK 16+）：
 * <pre>{@code
 * public record UserId(@JsonValue Long value) implements Identifier<Long> {
 *     public UserId {
 *         Objects.requireNonNull(value);
 *     }
 *     @Override
 *     public Long identifier() {
 *         return value;
 *     }
 * }
 * }</pre>
 *
 * @param <T> 底层值的类型，必须实现 {@link Comparable}
 * @see Type
 */
public interface Identifier<T extends Comparable<T>> extends Type {

    /**
     * 返回该标识符的底层值。
     */
    T identifier();

    /**
     * 委托给底层值的 {@code compareTo} 实现标识符的比较。
     */
    @Override
    @SuppressWarnings("unchecked")
    default int compareTo(Type other) {
        return identifier().compareTo(((Identifier<T>) other).identifier());
    }
}
