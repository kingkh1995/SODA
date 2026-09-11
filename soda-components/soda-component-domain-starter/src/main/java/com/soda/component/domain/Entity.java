package com.soda.component.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 领域实体的抽象基类 — 身份标识载体。
 * <p>
 * 实体是具有连续身份标识（identity thread）的领域对象，直接持有 {@link Identifier} DP 作为身份标识。
 * 仅承载身份能力（id 分配/恢复）；状态机（{@link Stateful}）与领域事件源（{@link EventSource}）
 * 是<b>聚合根</b>能力，见 {@link Aggregate}——普通实体（如 {@code AuthAccount}，仅业务属性、
 * 无生命周期状态枚举、无领域事件）不实现。
 * <p>
 * 构造器按场景二选一：
 * <ul>
 *   <li><b>手动设置 &amp; 已有数据恢复</b> — {@link #Entity(Identifier)} 传入 ID</li>
 *   <li><b>服务端生成</b> — {@link #Entity()} 无 ID，由 Repository 调用 {@link #assignId(Identifier)}</li>
 * </ul>
 *
 * @param <ID> 标识符类型
 * @see Aggregate
 * @see Identifiable
 */

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@EqualsAndHashCode
public abstract class Entity<ID extends Identifier<?>> implements Identifiable<ID> {

    private @Nullable ID id;

    /**
     * 服务端生成：构造时无 ID，后续由 {@link #assignId(Identifier)} 填补。
     */
    protected Entity() {
    }

    /**
     * 手动设置 / 已有数据恢复（reconstitution）。
     */
    protected Entity(ID id) {
        Assert.notNull(id, "id must not be null");
        this.id = id;
    }

    @Override
    public final ID getId() {
        return Objects.requireNonNull(id, "id not assigned yet");
    }

    /**
     * 是否已分配标识符。
     * <p>
     * 创建瞬态（服务端生成路径）为 false，持久化后由 {@link #assignId} 填补为 true。
     */
    public boolean isIdentified() {
        return id != null;
    }

    /**
     * 持久化后由 Repository 填补 ID —— 基础设施回调，写一次。
     * <p>
     * 同值重复回填幂等（重试/多路径 save 安全）；异值即两个持久化身份被赋予同一实体，
     * 抛裸 {@link IllegalStateException}（防御编程不携消息，见 ADR-0015）。
     */
    public final void assignId(ID id) {
        Objects.requireNonNull(id, "id must not be null");
        if (this.id == null) {
            this.id = id;
        } else if (!this.id.equals(id)) {
            throw new IllegalStateException();
        }
    }
}
