package com.soda.user.start;

import com.soda.support.test.ModulithTestSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Spring Modulith 模块依赖关系验证 — 业务层（{@code com.soda.user}）。
 *
 * <ul>
 *   <li>{@code type = CLOSED} 的模块：依赖方向被严格校验，只能引用 {@code allowedDependencies} 中声明的模块
 *   <li>{@code type = OPEN} 的模块：根模块（无依赖），允许被任何人引用
 * </ul>
 *
 * <p>白名单以 FQN 书写，只有 {@code CrossRootModules} 合并实例的名空间能解析（单根实例会剥离根前缀，解析即抛 {@code No module found
 * with name}）——故权威 {@code verify()} 落在合并实例上，本类只做转调。详见 {@code modulith.md} §3.1。
 *
 * <p>模块清单与白名单的事实源是各模块的 {@code package-info.java}（本类不复制——{@code
 * should_print_when_moduleStructureRendered} 打印真实结构）。
 */
@DisplayName("soda-user 模块结构")
class ModulithTest {

    @Test
    @DisplayName("模块依赖关系校验通过")
    void should_verify_when_moduleStructureFollowsConvention() {
        ModulithTestSupport.verifyModuleStructure();
    }

    @Test
    @DisplayName("打印模块结构供人工核对")
    void should_print_when_moduleStructureRendered() {
        ModulithTestSupport.printModuleStructure();
    }

    @Test
    @DisplayName("CLOSED 模块无未声明依赖")
    void shouldHaveNoUndeclaredDependenciesInClosedModules() {
        ModulithTestSupport.assertNoUndeclaredDependenciesInClosedModules();
    }

    @Test
    @DisplayName("跨根依赖为「业务层 → 框架层」单向")
    void shouldHaveNoCrossRootCoupling() {
        ModulithTestSupport.assertNoCrossRootCoupling();
    }
}
