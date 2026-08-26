package com.soda.component.start;

import com.soda.support.test.ModulithTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 模块依赖关系验证 — 组件层。
 * <p>
 * <ul>
 *   <li>{@code type = CLOSED} 的模块：依赖方向被严格校验，只能引用 {@code allowedDependencies} 中声明的模块</li>
 *   <li>{@code type = OPEN} 的模块：根模块（无依赖），允许被任何人引用</li>
 * </ul>
 * <p>
 * <pre>
 * ┌──────────────────┬──────────┬──────────────────────────────┐
 * │ Module           │ Type     │ Allowed dependencies         │
 * ├──────────────────┼──────────┼──────────────────────────────┤
 * │ api              │ OPEN     │ (none)                       │
 * │ domain           │ OPEN     │ (none)                       │
 * │ domain.types     │ CLOSED   │ domain, domain.util          │
 * │ domain.gateway   │ CLOSED   │ domain, domain.types, domain.util │
 * │ domain.util      │ CLOSED   │ (none)                       │
 * │ application      │ CLOSED   │ domain, api                  │
 * │ web              │ CLOSED   │ api                          │
 * │ consumer         │ CLOSED   │ api                          │
 * │ job              │ CLOSED   │ api                          │
 * │ infrastructure   │ CLOSED   │ domain                       │
 * │ queryserver      │ CLOSED   │ api                          │
 * │ start            │ CLOSED   │ (none)                       │
 * </pre>
 */
@DisplayName("Spring Modulith 组件层模块结构")
class ModulithTest {

    @Test
    @DisplayName("模块依赖符合声明约定时校验通过")
    void should_verify_when_moduleStructureFollowsConvention() {
        var modules = ApplicationModules.of("com.soda.component");
        modules.verify();
    }

    @Test
    @DisplayName("打印组件层模块结构供人工核对")
    void should_print_when_moduleStructureRendered() {
        var modules = ApplicationModules.of("com.soda.component");
        modules.forEach(System.out::println);
    }

    @Test
    @DisplayName("CLOSED 模块无未声明依赖")
    void shouldHaveNoUndeclaredDependenciesInClosedModules() {
        ModulithTestSupport.assertNoUndeclaredDependenciesInClosedModules("com.soda.component");
    }
}