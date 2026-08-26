package com.soda.component.domain.types;

import com.soda.support.test.ModulithTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 模块依赖关系验证。
 * <p>
 * <ul>
 *   <li>{@code type = CLOSED} 的模块：依赖方向被严格校验，只能引用 {@code allowedDependencies} 中声明的模块</li>
 *   <li>{@code type = OPEN} 的模块：根模块（无依赖），允许被任何人引用</li>
 * </ul>
 * <p>
 * <pre>
 * ┌──────────────────┬──────────┬──────────────────────────────────────┐
 * │ Module           │ Type     │ Allowed dependencies                 │
 * ├──────────────────┼──────────┼──────────────────────────────────────┤
 * │ domain           │ OPEN     │ (none)                               │
 * │ domain.util      │ CLOSED   │ (none)                               │
 * │ domain.types     │ CLOSED   │ domain, domain.util                  │
 * │ domain.gateway   │ CLOSED   │ domain, domain.types, domain.util     │
 * </pre>
 */
@DisplayName("Spring Modulith 模块结构")
class ModulithTest {

    @Test
    @DisplayName("模块依赖关系符合结构约定（CLOSED 模块依赖白名单校验）")
    void should_verifyModuleStructure() {
        var modules = ApplicationModules.of("com.soda.component");
        modules.verify();
    }

    @Test
    @DisplayName("打印模块结构（人工诊断输出）")
    void should_printModuleStructure() {
        var modules = ApplicationModules.of("com.soda.component");
        modules.forEach(System.out::println);
    }

    @Test
    @DisplayName("CLOSED 模块无未声明依赖")
    void shouldHaveNoUndeclaredDependenciesInClosedModules() {
        ModulithTestSupport.assertNoUndeclaredDependenciesInClosedModules("com.soda.component");
    }
}