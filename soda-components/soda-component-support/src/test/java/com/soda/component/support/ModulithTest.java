package com.soda.component.support;

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
 * ┌────────────────┬──────────┬──────────────────────────────────────┐
 * │ Module         │ Type     │ Allowed dependencies                 │
 * ├────────────────┼──────────┼──────────────────────────────────────┤
 * │ domain         │ OPEN     │ (none)                               │
 * │ support        │ OPEN     │ (none)                               │
 * │ support.spi    │ OPEN     │ (none)                               │
 * │ support.util   │ CLOSED   │ support.spi                          │
 * │ support.types  │ CLOSED   │ domain, support.util, support.spi    │
 * │ support.gateway│ CLOSED   │ domain, support.types                │
 * </pre>
 */
class ModulithTest {

    @Test
    void verifyModuleStructure() {
        var modules = ApplicationModules.of("com.soda.component");
        // verify() throws Violations (extends RuntimeException) on failure
        modules.verify();
    }

    @Test
    void printModuleStructure() {
        var modules = ApplicationModules.of("com.soda.component");
        modules.forEach(System.out::println);
    }
}
