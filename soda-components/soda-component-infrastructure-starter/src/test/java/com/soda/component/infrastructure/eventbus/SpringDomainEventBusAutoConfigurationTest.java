package com.soda.component.infrastructure.eventbus;

import com.soda.component.domain.DomainEventBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配契约：组件 infrastructure-starter 位于运行时类路径即提供
 * {@link DomainEventBus}（经 AutoConfiguration.imports 注册），
 * 业务模块声明自定义 {@link DomainEventBus} bean 时自动退避。
 */
@DisplayName("SpringDomainEventBus 自动装配")
class SpringDomainEventBusAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SpringDomainEventBusAutoConfiguration.class));

    @Test
    @DisplayName("类路径存在 starter 时自动装配 DomainEventBus 与 SpringDomainEventBus")
    void should_autoConfigure_when_starterOnClasspath() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(DomainEventBus.class)
                .hasSingleBean(SpringDomainEventBus.class));
    }

    @Test
    @DisplayName("业务模块已定义 DomainEventBus bean 时自动退避")
    void should_backOff_when_customDomainEventBusBeanExists() {
        runner.withBean(DomainEventBus.class, () -> event -> {
                })
                .run(context -> assertThat(context)
                        .hasSingleBean(DomainEventBus.class)
                        .doesNotHaveBean(SpringDomainEventBus.class));
    }
}
