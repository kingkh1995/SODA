package com.soda.component.domain;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
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
 * <p>
 * 使用 Lombok {@code @EqualsAndHashCode} 生成基于字段的相等判断，排除领域事件列表（{@code domainEvents}）。
 *
 * @param <ID> 标识符类型
 * @see Aggregate
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
@EqualsAndHashCode(exclude = {"domainEvents"})
public abstract class Entity<ID extends Identifier<?>> implements Identifiable<ID>, EventSource<ID> {

    private @Nullable ID id;

    @JsonIgnore
    private transient List<DomainEvent<ID>> domainEvents = new ArrayList<>();

    /**
     * 手动设置 / 已有数据恢复（reconstitution）。
     */
    protected Entity(ID id) {
        Assert.notNull(id, "id must not be null");
        this.id = id;
    }

    /**
     * 客户端生成：构造时由 {@code generator} 产生 ID，发生在构造器内部。
     */
    protected Entity(Supplier<ID> generator) {
        Assert.notNull(generator, "generator must not be null");
        var id = generator.get();
        Assert.notNull(id, "id must not be null");
        this.id = id;
    }

    /**
     * 服务端生成：构造时无 ID，后续由 {@link #assignId(Identifier)} 填补。
     */
    protected Entity() {
    }

    @Override
    public final @Nullable ID getId() {
        return id;
    }

    /**
     * 持久化后由 Repository 填补 ID。
     * <p>
     * 仅限服务端生成场景调用（{@link #Entity()} 构造），已有 ID 时忽略。
     *
     * @throws NullPointerException id 为 null
     */
    public final void assignId(ID id) {
        if (this.isIdentified()) {
            return;
        }
        Assert.notNull(id, "id must not be null");
        this.id = id;
    }

    /**
     * 注册领域事件，在 {@link #flushEvents} 时被获取并发送。
     * <p>
     * 在业务方法中调用，一个业务方法可注册多个事件。
     *
     * @param event 领域事件，非 null
     */
    protected void registerEvent(DomainEvent<ID> event) {
        Assert.notNull(event, "event must not be null");
        domainEvents.add(event);
    }

    /**
     * 取出当前所有未发送的领域事件并清空内部列表。
     * <p>
     * 供 ApplicationService 在持久化后取出事件并通过 {@link DomainEventBus#fireAll} 发送。
     *
     * @return 未发送的领域事件列表；无事件时返回空列表
     */
    @Override
    public List<DomainEvent<ID>> flushEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents = new ArrayList<>();
        return events;
    }
}
