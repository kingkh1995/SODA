package com.soda.support.test;

import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.DependencyDepth;
import org.springframework.modulith.core.DependencyType;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Modulith 通用测试支持工具。
 * <p>
 * 提供可复用的模块依赖验证逻辑，避免在多个测试类中重复实现。
 */
public final class ModulithTestSupport {

    private ModulithTestSupport() {
    }

    /**
     * 验证 CLOSED 模块无未声明依赖——抓「实际使用但未声明在 {@code allowedDependencies} 白名单」的边界穿透。
     * <p>
     * <b>断言语义</b>：{@code allowedDependencies} 是「架构边界预留白名单」——声明的依赖可能
     * 当前尚未使用（设计前瞻、跨层引用的预备阶段），但「实际使用」必须落在白名单内（防止穿透边界）。
     * 这与 {@code modulith.verify()} 的「用错即拒」互补：
     * <ul>
     *   <li>{@code verify()} 捕获「import 超出白名单」——本方法反向校验，作为 fail-fast 双保险</li>
     *   <li>本方法捕获「实际使用未声明」——边界穿透的隐性违规</li>
     * </ul>
     * <b>不校验「声明未使用」</b>：允许预留依赖（如 {@code allowedDependencies = {"api"}}
     * 当前未直接 import，但为后续 DTO/Command 接入预留）。
     *
     * @param basePackage 要扫描的基础包名（如 {@code "com.soda.component"} 或 {@code "com.soda.user"}）
     */
    public static void assertNoUndeclaredDependenciesInClosedModules(String basePackage) {
        var modules = ApplicationModules.of(basePackage);
        var violations = new ArrayList<String>();
        modules.forEach(module -> {
            if (!module.isOpen()) {
                var actualDeps = module.getDependencies(modules, DependencyDepth.ALL,
                                DependencyType.USES_COMPONENT, DependencyType.ENTITY,
                                DependencyType.EVENT_LISTENER, DependencyType.DEFAULT)
                        .uniqueModules()
                        .map(ApplicationModule::getDisplayName)
                        .collect(Collectors.toSet());
                var declaredDeps = module.getAllowedDependencies(modules)
                        .stream()
                        .map(dep -> dep.getTargetModule().getDisplayName())
                        .collect(Collectors.toSet());
                // 反转：捕获「实际使用但未声明」的边界穿透
                var undeclared = actualDeps.stream()
                        .filter(dep -> !declaredDeps.contains(dep))
                        .collect(Collectors.toSet());
                if (!undeclared.isEmpty()) {
                    violations.add("Module " + module.getDisplayName() + " uses undeclared dependencies: " + undeclared);
                }
            }
        });
        assertThat(violations)
                .as("CLOSED modules must not use undeclared dependencies (allowedDependencies is the architectural whitelist; reserved deps without current usage are permitted): %s", violations)
                .isEmpty();
    }
}