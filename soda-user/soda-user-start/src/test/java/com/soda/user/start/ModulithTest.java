package com.soda.user.start;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith 模块依赖关系验证。
 * <p>
 * 扫描 {@code com.soda.user} 根包下所有 {@code @ApplicationModule} 注解的包，
 * 验证各子模块间依赖关系符合 package-info.java 中声明的白名单。
 * <p>
 * 全部模块为 OPEN 时 cycle check 无可验证的类，
 * 通过 {@code src/test/resources/archunit.properties} 设置
 * {@code archRule.failOnEmptyShould=false} 允许空 should 规则。
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
