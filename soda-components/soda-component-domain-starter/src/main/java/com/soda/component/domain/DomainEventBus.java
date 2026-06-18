package com.soda.component.domain;

/**
 * 领域事件总线接口（依赖倒置：领域层定义契约，基础设施层实现）。
 * <p>
 * 实现在基础设施层，对接 Spring ApplicationEventPublisher、Spring Modulith 事件总线或 MQ。
 * <p>
 * 调用链：ApplicationService 通过 {@link EntityGateway} 持久化后，调用 {@link EventSource#flushEvents} 取出事件，再通过 {@link #fireAll} 发送。
 *
 * @see DomainEvent
 * @see Entity
 * @see EventSource
 */
public interface DomainEventBus extends Gateway {

    /**
     * 发布单个领域事件。
     *
     * @param event 领域事件，非 null
     */
    void fire(DomainEvent<?> event);

    /**
     * 批量发布领域事件。
     * <p>
     * 默认实现逐条委托 {@link #fire}，基础设施实现可按需重写（如批量入队）。
     *
     * @param events 领域事件集合，非 null
     */
    default void fireAll(Iterable<? extends DomainEvent<?>> events) {
        events.forEach(this::fire);
    }
}
