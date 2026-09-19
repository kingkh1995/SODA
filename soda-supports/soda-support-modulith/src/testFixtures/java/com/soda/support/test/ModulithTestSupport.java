package com.soda.support.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.modulith.Modulithic;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.DependencyDepth;
import org.springframework.modulith.core.DependencyType;
import org.springframework.modulith.core.JavaPackage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Modulith 通用测试支持工具。
 *
 * <p>提供可复用的模块依赖验证逻辑，避免在多个测试类中重复实现。
 */
public final class ModulithTestSupport {

    private ModulithTestSupport() {}

    /**
     * 验证 CLOSED 模块无未声明依赖——抓「实际使用但未声明在 {@code allowedDependencies} 白名单」的边界穿透。
     *
     * <p><b>断言语义</b>：{@code allowedDependencies} 是「架构边界预留白名单」——声明的依赖可能
     * 当前尚未使用（设计前瞻、跨层引用的预备阶段），但「实际使用」必须落在白名单内（防止穿透边界）。 这与 {@code modulith.verify()} 的「用错即拒」互补：
     *
     * <ul>
     *   <li>{@code verify()} 捕获「import 超出白名单」——本方法反向校验，作为 fail-fast 双保险
     *   <li>本方法捕获「实际使用未声明」——边界穿透的隐性违规
     * </ul>
     *
     * <b>不校验「声明未使用」</b>：允许预留依赖（如 {@code allowedDependencies = {"api"}} 当前未直接 import，但为后续
     * DTO/Command 接入预留）。
     *
     * <p>以 {@link CrossRootModules} 合并实例为扫描源——白名单为 FQN，只有合并实例的名空间能解析（见该内部类的说明）。 故本方法无参：调用方不必再传根包。
     */
    public static void assertNoUndeclaredDependenciesInClosedModules() {
        var modules = ApplicationModules.of(CrossRootModules.class);
        var violations = new ArrayList<String>();
        modules.forEach(
                module -> {
                    if (!module.isOpen()) {
                        var actualDeps =
                                module.getDependencies(
                                                modules,
                                                DependencyDepth.ALL,
                                                DependencyType.USES_COMPONENT,
                                                DependencyType.ENTITY,
                                                DependencyType.EVENT_LISTENER,
                                                DependencyType.DEFAULT)
                                        .uniqueModules()
                                        .map(ApplicationModule::getIdentifier)
                                        .collect(Collectors.toSet());
                        var declaredDeps =
                                module.getAllowedDependencies(modules).stream()
                                        .map(dep -> dep.getTargetModule().getIdentifier())
                                        .collect(Collectors.toSet());
                        // 反转：捕获「实际使用但未声明」的边界穿透
                        var undeclared =
                                actualDeps.stream()
                                        .filter(dep -> !declaredDeps.contains(dep))
                                        .collect(Collectors.toSet());
                        if (!undeclared.isEmpty()) {
                            violations.add(
                                    "Module "
                                            + module.getIdentifier()
                                            + " uses undeclared dependencies: "
                                            + undeclared);
                        }
                    }
                });
        assertThat(violations)
                .as(
                        "CLOSED modules must not use undeclared dependencies (allowedDependencies is the architectural whitelist; reserved deps without current usage are permitted): %s",
                        violations)
                .isEmpty();
    }

    /**
     * 验证跨根绑定不变式——把「框架层复用 vs 业务隔离」判据从人工评审升为机检。
     *
     * <p>双根建模下 {@code verify()} 只见被传入的单个根包，「新增跨根引用」此前不受任何门禁 （单源见 {@code
     * docs/conventions/modulith.md} §3.2 R30a 与 §4 E1）。本方法以 {@link CrossRootModules}
     * 单实例枚举跨根依赖，断言两条不变式：
     *
     * <ol>
     *   <li><b>框架层不反向依赖业务层</b>——{@code component.*} / {@code support.*} 不得依赖 {@code
     *       user.*}（框架层是业务层的下行依赖，反向即成环）
     *   <li><b>业务层不互引</b>——{@code user.*} 的跨根依赖只允许指向 {@code component.*}； 指向 {@code support.*}
     *       或另一业务模块即为违规（新增业务模块时阻止其偷引既有业务模块内部）
     * </ol>
     *
     * <b>依赖口径</b>：{@link DependencyDepth#ALL}——含传递边与 bean / event 依赖。不可改用直接 import 计数：传递边会漏（如
     * {@code gateway → domain.types → domain.util}）。
     *
     * <p><b>覆盖缺口</b>：{@code queryserver} 两个模块不在本实例视野（独立部署单元，非本测试模块的 编译或运行期依赖，见 {@code
     * ADR-0019}）；其自身无跨根依赖，缺口不影响本断言强度。新增跨根 依赖若落在不可见模块，本方法**静默漏检**——须按 {@code modulith.md} §4 E1
     * 人工补核。
     */
    public static void assertNoCrossRootCoupling() {
        var modules = ApplicationModules.of(CrossRootModules.class);
        var violations = new ArrayList<String>();
        modules.forEach(
                module -> {
                    var base = module.getBasePackage().getName();
                    var sourceRoot = rootOf(base);
                    var targets =
                            module.getDependencies(
                                            modules,
                                            DependencyDepth.ALL,
                                            DependencyType.USES_COMPONENT,
                                            DependencyType.ENTITY,
                                            DependencyType.EVENT_LISTENER,
                                            DependencyType.DEFAULT)
                                    .uniqueModules()
                                    .map(ApplicationModule::getBasePackage)
                                    .map(JavaPackage::getName)
                                    .filter(target -> !rootOf(target).equals(sourceRoot))
                                    .sorted()
                                    .collect(Collectors.toList());
                    for (var target : targets) {
                        if (isFrameworkLayer(sourceRoot)) {
                            // 不变式 1：框架层不得反向依赖业务层
                            violations.add("框架层反向依赖业务层: " + base + " -> " + target);
                        } else if (!"component".equals(rootOf(target))) {
                            // 不变式 2：业务层的跨根依赖只允许指向框架层
                            violations.add("跨根业务耦合: " + base + " -> " + target);
                        }
                    }
                });
        assertThat(violations)
                .as(
                        "跨根依赖须为「业务层 → 框架层」单向（框架层复用合法；业务互引违规，见 modulith.md §3.2 R30a / §4 E1）: %s",
                        violations)
                .isEmpty();
    }

    /**
     * 校验合并实例的模块结构——白名单为 FQN，只有 {@link CrossRootModules} 的名空间能解析，故全仓唯一权威 {@code verify()} 落在此实例上。
     *
     * <p>取代原先「每根各一次 {@code ApplicationModules.of("com.soda.<root>").verify()}」：FQN 名在单根实例下被剥离根前缀，
     * 解析即抛 {@code No module found with name 'com.soda.user.api'}。本方法覆盖面更广——合并实例同时校验**跨根**边，
     * 而单根实例对跨根边完全不可见。
     */
    public static void verifyModuleStructure() {
        ApplicationModules.of(CrossRootModules.class).verify();
    }

    /** 打印合并实例的完整模块结构，供人工核对。 */
    public static void printModuleStructure() {
        ApplicationModules.of(CrossRootModules.class).forEach(System.out::println);
    }

    /** 取包所属的应用根标识（{@code com.soda.<root>} 的 {@code <root>} 段）。 */
    private static String rootOf(String pkg) {
        var parts = pkg.split("\\.");
        return parts.length > 2 ? parts[2] : pkg;
    }

    /** 是否为框架层根——{@code component}（分层 starter 族）与 {@code support}（可选支持模块）。 */
    private static boolean isFrameworkLayer(String root) {
        return List.of("component", "support").contains(root);
    }

    /**
     * 跨根绑定持有类 — 纯载体，无实例、无行为。
     *
     * <p>{@code additionalPackages} 把三个应用根（框架层 {@code com.soda.component}、业务层 {@code
     * com.soda.user}、支持层 {@code com.soda.support}）并入**同一个** {@link ApplicationModules}
     * 实例，使跨根依赖进入枚举视野；{@code useFullyQualifiedModuleNames} 是**必需项**——两根含 9 组同名子包（{@code api} /
     * {@code application} / {@code consumer} / {@code domain} / {@code infrastructure} / {@code
     * job} / {@code queryserver} / {@code start} / {@code web}），短名会在构造期抛 {@code
     * IllegalStateException: Duplicate key domain}。
     *
     * <p><b>本实例不可 {@code verify()}</b>：白名单项是根包相对逻辑名，合并实例的名空间下 {@code "domain"} / {@code "api"}
     * 等无法解析 → {@code IllegalArgumentException}（实测 14 个有非空白名单的模块全部抛错）。故本类只作**枚举源**——{@code verify()}
     * 仍由两个应用根 各自承担，二者职责互补。
     *
     * <p>已知不在视野：{@code queryserver} 两个模块（独立部署单元，非消费方测试模块的编译 / 运行期 依赖，见 {@code ADR-0019}）。
     */
    @Modulithic(
            systemName = "soda",
            additionalPackages = {"com.soda.component", "com.soda.user", "com.soda.support"},
            useFullyQualifiedModuleNames = true)
    public static class CrossRootModules {}
}
