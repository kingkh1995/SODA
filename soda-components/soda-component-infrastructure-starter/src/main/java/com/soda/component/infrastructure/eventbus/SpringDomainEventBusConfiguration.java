package com.soda.component.infrastructure.eventbus;

import com.soda.component.domain.DomainEventBus;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * 基于 Spring {@link ApplicationEventPublisher} 的 {@link DomainEventBus} 自动装配。
 * <p>
 * 经 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 注册：组件 infrastructure-starter 位于运行时类路径即生效，无需业务模块 {@code @Import}。
 * <p>
 * 仅在容器无 {@link DomainEventBus} bean 时生效，
 * 模块可提供自定义实现（如 Kafka/RocketMQ）覆盖。
 */
@AutoConfiguration
public class SpringDomainEventBusConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventBus.class)
    public DomainEventBus springDomainEventBus(ApplicationEventPublisher publisher) {
        return new SpringDomainEventBus(publisher);
    }
}
