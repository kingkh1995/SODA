package com.soda.component.web.conditional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * If-Match 入站装配：注册 {@link IfMatchResolver}；非 Servlet 环境自动退避（见 ADR-0039）。
 * <p>
 * 出站 {@code ETag} 头与入站 If-Match 的线上行为由 {@code ConditionalRequestMvcTest} 以完整 MVC 覆盖。
 */
@DisplayName("IfMatchAutoConfiguration 装配")
class IfMatchAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IfMatchAutoConfiguration.class));

    @Test
    @DisplayName("注册 IfMatchResolver 参数解析器")
    void should_registerIfMatchResolver_when_addArgumentResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        new IfMatchAutoConfiguration().addArgumentResolvers(resolvers);

        assertThat(resolvers).hasSize(1);
        assertThat(resolvers.getFirst()).isInstanceOf(IfMatchResolver.class);
    }

    @Test
    @DisplayName("非 Web 环境下自动退避")
    void should_backOff_when_nonWebEnvironment() {
        runner.run(context -> assertThat(context).doesNotHaveBean(IfMatchAutoConfiguration.class));
    }
}
