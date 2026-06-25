package com.soda.component.domain;
/**
 *
 * 标识符是 <strong>不可变</strong> 的领域原语，在限界上下文内唯一标识一个实体。
 * 每个具体的标识符类型必须：
 * <ul>
 *   <li>不可变 — 优先使用 {@code record}</li>
 *   <li>value-based identity — 继承自 {@link Type}</li>
 *   <li>可比较 — 子类自行实现 {@link Comparable} 以提供类型安全</li>
 * </ul>
 * <p>
 * 由于 {@code Identifier<T>} 中 {@code T extends Comparable<T>}，
 * 使用示例（record 风格，JDK 16+）：
 * <pre>{@code
 * public record UserId(@JsonValue Long value) implements Identifier<Long>, Comparable<UserId> {
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
}
