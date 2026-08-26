package com.soda.user.start;

import com.soda.support.test.ModulithTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Modulith 模块依赖关系验证。
 * <ul>
 *   <li>{@code type = CLOSED} 的模块：依赖方向被严格校验，只能引用 {@code allowedDependencies} 中声明的模块</li>
 *   <li>{@code type = OPEN} 的模块：根模块（无依赖），允许被任何人引用</li>
 * </ul>
 * <p>
 * <pre>
 * ┌──────────────────┬──────────┬──────────────────────────────────────┐
 * │ Module           │ Type     │ Allowed dependencies                 │
 * ├──────────────────┼──────────┼──────────────────────────────────────┤
 * │ api              │ OPEN     │ (none)，见 ADR-0026                  │
 * │ domain           │ OPEN     │ (none)                               │
 * │ application      │ CLOSED   │ api, domain                          │
 * │ web              │ CLOSED   │ api，见 ADR-0026                     │
 * │ job              │ CLOSED   │ api                                  │
 * │ consumer         │ CLOSED   │ api                                  │
 * │ infrastructure   │ CLOSED   │ domain                               │
 * │ queryserver      │ CLOSED   │ api                                  │
 * │ start            │ CLOSED   │ (none)                               │
 * </pre>
 */
@DisplayName("soda-user 模块结构")
class ModulithTest {

    @Test
    @DisplayName("模块依赖关系校验通过")
    void should_verifyModuleStructure() {
        ApplicationModules.of("com.soda.user").verify();
    }

    @Test
    @DisplayName("模块结构可解析且非空")
    void should_resolveNonEmptyModules() {
        var modules = ApplicationModules.of("com.soda.user");
        modules.forEach(System.out::println);
        assertThat(modules).isNotEmpty();
    }

    @Test
    @DisplayName("CLOSED 模块无未声明依赖")
    void shouldHaveNoUndeclaredDependenciesInClosedModules() {
        ModulithTestSupport.assertNoUndeclaredDependenciesInClosedModules("com.soda.user");
    }
}