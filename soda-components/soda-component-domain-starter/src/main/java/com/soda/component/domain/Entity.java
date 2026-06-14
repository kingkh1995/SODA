package com.soda.component.domain;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 领域实体的抽象基类。
 * <p>
 * 实体是具有连续身份标识（identity thread）的领域对象。直接持有 {@link Identifier} DP 作为身份标识。
 * <p>
 * 构造器按场景三选一：
 * <ul>
 *   <li><b>手动设置 &amp; 已有数据恢复</b> — {@link #Entity(Identifier)} 传入 ID</li>
 *   <li><b>客户端生成</b> — {@link #Entity(Supplier)} 构造器内部调用 {@code generator.get()}</li>
 *   <li><b>服务端生成</b> — {@link #Entity()} 无 ID，由 Repository 调用 {@link #assignId(Identifier)}</li>
 * </ul>
 * <p>
 * 不覆写 {@code equals} / {@code hashCode}（保持 Object 引用相等）。
 *
 * @param <ID> 标识符类型
 * @see Aggregate
 */
public abstract class Entity<ID extends Identifier<?>> implements Identifiable<ID> {

    private @Nullable ID id;

    /** 手动设置 / 已有数据恢复（reconstitution）。 */
    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "id must not be null");
    }
    /** 客户端生成：构造时由 {@code generator} 产生 ID，发生在构造器内部。 */
    protected Entity(Supplier<ID> generator) {
        this.id = Objects.requireNonNull(Objects.requireNonNull(generator, "generator must not be null").get());
    }

    /** 服务端生成：构造时无 ID，后续由 {@link #assignId(Identifier)} 填补。 */
    protected Entity() {
    }

    @Override
    public final @Nullable ID getId() {
        return id;
    }

    /**
     * 持久化后由 Repository 填补 ID。
     * <p>
     * 仅限服务端生成场景调用（{@link #Entity()} 构造），已有 ID 时抛出异常确保只赋值一次。
     *
     * @throws IllegalStateException Entity 已有 ID
     */
    public final void assignId(ID id) {
        if (this.isIdentified()) {
            throw new IllegalStateException("entity already identified");
        }
        this.id = Objects.requireNonNull(id);
    }
}
