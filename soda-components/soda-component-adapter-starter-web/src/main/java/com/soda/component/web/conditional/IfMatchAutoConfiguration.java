package com.soda.component.web.conditional;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * If-Match 入站装配 —— 注册 {@link IfMatchResolver}，并导入 {@link HttpValidatorHeadersAdvice} 出站头派生（见 ADR-0039）。
 * <p>
 * 经 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册：
 * 组件 adapter-starter-web 位于运行时类路径即生效。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(HttpValidatorHeadersAdvice.class)
public class IfMatchAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new IfMatchResolver());
    }
}
