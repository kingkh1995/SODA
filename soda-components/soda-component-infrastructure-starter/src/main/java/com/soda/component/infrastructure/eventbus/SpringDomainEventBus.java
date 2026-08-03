package com.soda.component.infrastructure.eventbus;

import com.soda.component.domain.DomainEvent;
import com.soda.component.domain.DomainEventBus;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Objects;

/**
 * 基于 Spring {@link ApplicationEventPublisher} 的 {@link DomainEventBus} 默认实现。
 * <p>
 * 属于基础设施层，由 {@link SpringDomainEventBusConfiguration} 自动装配激活。
 * 业务模块可通过声明自己的 {@link DomainEventBus} {@code @Bean} 覆盖。
 */
public final class SpringDomainEventBus implements DomainEventBus {

    private final ApplicationEventPublisher eventPublisher;

    public SpringDomainEventBus(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public void publish(DomainEvent<?> event) {
        eventPublisher.publishEvent(Objects.requireNonNull(event));
    }

    @Override
    public void publishAll(Iterable<? extends DomainEvent<?>> events) {
        Objects.requireNonNull(events).forEach(e -> eventPublisher.publishEvent(Objects.requireNonNull(e)));
    }
}
