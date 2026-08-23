package com.soda.component.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根的抽象基类 — 聚合根能力：身份（{@link Entity}）+ 状态机对象（{@link Stateful}）+
 * 领域事件源（{@link EventSource}）。
 * <p>
 * 聚合根是聚合一致性边界内的顶层实体，负责保证聚合内部的所有不变量不被破坏。
 * 对聚合的所有操作必须通过聚合根进行。
 * <p>
 * 状态机契约（{@link Stateful}）：{@code getState()} 由具体聚合经 Lombok {@code @Getter}
 * 生成满足（协变返回各自状态枚举，如 {@code UserState}/{@code VerificationState}），
 * 终态判定 {@code isTerminal()} 委托 {@link StateEnumType#terminal()}——应用层经
 * {@code AbstractAppService.requireNotTerminal} 统一守卫终态禁写。普通实体（{@code AuthAccount}）
 * 无生命周期状态枚举，不实现本契约（恒非终态无意义）。
 * <p>
 * 领域事件（{@link EventSource}）：业务方法内 {@link #registerEvent} 注册，
 * ApplicationService 持久化后 {@link #flushEvents} 取出并经 {@link DomainEventBus#publishAll} 发送。
 *
 * @param <ID> 聚合根标识符类型
 * @see Entity
 * @see Stateful
 * @see EventSource
 */
public abstract class Aggregate<ID extends Identifier<?>> extends Entity<ID>
        implements Stateful, EventSource<ID> {

    @JsonIgnore
    private transient List<DomainEvent<ID>> domainEvents = new ArrayList<>();

    /**
     * 服务端生成。
     */
    protected Aggregate() {
        super();
    }

    /**
     * 手动设置 / 已有数据恢复。
     */
    protected Aggregate(ID id) {
        super(id);
    }

    /**
     * 注册领域事件，在 {@link #flushEvents} 时被获取并发送。
     * <p>
     * 在业务方法中调用，一个业务方法可注册多个事件。
     *
     * @param event 领域事件
     */
    protected void registerEvent(DomainEvent<ID> event) {
        this.domainEvents.add(event);
    }

    /**
     * 取出当前所有未发送的领域事件并清空内部列表。
     * <p>
     * 供 ApplicationService 在持久化后取出事件并通过 {@link DomainEventBus#publishAll} 发送。
     *
     * @return 未发送的领域事件列表；无事件时返回空列表
     */
    @Override
    public List<DomainEvent<ID>> flushEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
