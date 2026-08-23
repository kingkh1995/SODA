package com.soda.component.infrastructure.eventbus;

import com.soda.component.domain.DomainEvent;
import com.soda.component.domain.DomainEventBus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 基于 Spring {@link ApplicationEventPublisher} 的 {@link DomainEventBus} 默认实现。
 * <p>
 * 属于基础设施层，由 {@link SpringDomainEventBusAutoConfiguration} 自动装配激活。
 * 业务模块可通过声明自己的 {@link DomainEventBus} {@code @Bean} 覆盖。
 */
@RequiredArgsConstructor
public final class SpringDomainEventBus implements DomainEventBus {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(DomainEvent<?> event) {
        eventPublisher.publishEvent(event);
    }
}
