package com.soda.component.support;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 模块依赖关系验证。
 * <p>
 * 验证规则：
 * <ul>
 *   <li>所有模块默认为 {@code type = OPEN}，允许被任何模块引用</li>
 *   <li>基础模块（domain、support、support.spi）不引用其他模块</li>
 *   <li>有依赖的模块（support.util、support.types、support.gateway）通过 {@code allowedDependencies} 白名单声明</li>
 * </ul>
 * <p>
 * 全部模块为 OPEN 时 cycle check 无可验证的类，
 * 通过 {@code src/test/resources/archunit.properties} 设置
 * {@code archRule.failOnEmptyShould=false} 允许空 should 规则。
 * <p>
 * <pre>
 * ┌────────────────┬──────────┬──────────────────────────┐
 * │ Module         │ Type     │ Allowed dependencies     │
 * ├────────────────┼──────────┼──────────────────────────┤
 * │ domain         │ OPEN     │ (none)                   │
 * │ support        │ OPEN     │ (none)                   │
 * │ support.util   │ OPEN     │ support.spi              │
 * │ support.types  │ OPEN     │ domain, support.util, support.spi │
 * │ support.spi    │ OPEN     │ (none)                   │
 * │ support.gateway│ OPEN     │ domain, support.types    │
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
