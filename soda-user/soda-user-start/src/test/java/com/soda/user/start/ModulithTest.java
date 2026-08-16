package com.soda.user.start;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

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
 * │ api              │ OPEN     │ (none) — 2026-08-16 解除 domain 依赖（见 ADR-0026）│
 * │ domain           │ OPEN     │ (none)                               │
 * │ application      │ CLOSED   │ api, domain                          │
 * │ web              │ CLOSED   │ api — 2026-08-16 解除 domain 依赖（见 ADR-0026）  │
 * │ job              │ CLOSED   │ api                                  │
 * │ consumer         │ CLOSED   │ api                                  │
 * │ infrastructure   │ CLOSED   │ domain                               │
 * │ queryserver      │ CLOSED   │ api                          │
 * │ start            │ CLOSED   │ (none)                       │
 * </pre>
 */
class ModulithTest {

    @Test
    void verifyModuleStructure() {
        var modules = ApplicationModules.of("com.soda.user");
        modules.verify();
    }

    @Test
    void printModuleStructure() {
        var modules = ApplicationModules.of("com.soda.user");
        modules.forEach(System.out::println);
    }
}
