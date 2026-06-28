package com.soda.component.domain;

import java.util.List;

/**
 * 领域事件来源标记接口。
 * <p>
 * {@link Entity} 实现此接口表明自身可作为领域事件来源。
 * 调用方可通过 {@link #flushEvents()} 取出已注册但未发送的事件。
 * <p>
 * 对应 {@link Identifiable} 的设计模式：将 Entity 的某一能力抽象为接口，
 * 避免调用方直接依赖 Entity 具体类型。
 *
 * @see DomainEventBus
 */
public interface EventSource<ID extends Identifier<?>> {

    /**
     * 取出当前所有未发送的领域事件并清空内部列表。
     * <p>
     * 取出后由 ApplicationService 通过 {@link DomainEventBus#fireAll} 发送。
     *
     * @return 未发送的领域事件列表；无事件时返回空列表
     */
    List<DomainEvent<ID>> flushEvents();
}
