package com.soda.component.infrastructure.eventbus;

import com.soda.component.domain.DomainEventBus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配契约：组件 infrastructure-starter 位于运行时类路径即提供
 * {@link DomainEventBus}（经 AutoConfiguration.imports 注册），
 * 业务模块声明自定义 {@link DomainEventBus} bean 时自动退避。
 */
class SpringDomainEventBusConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringDomainEventBusConfiguration.class));

    @Test
    void autoConfiguresDomainEventBus() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(DomainEventBus.class)
                .hasSingleBean(SpringDomainEventBus.class));
    }

    @Test
    void backsOffWhenCustomDomainEventBusExists() {
        runner.withBean(DomainEventBus.class, () -> event -> {
                })
                .run(context -> assertThat(context)
                        .hasSingleBean(DomainEventBus.class)
                        .doesNotHaveBean(SpringDomainEventBus.class));
    }
}
