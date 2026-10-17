package com.soda.support.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.DependencyDepth;
import org.springframework.modulith.core.DependencyType;
import org.springframework.modulith.core.JavaPackage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SODA 全仓模块结构与跨根依赖方向校验。
 *
 * <p>本测试是仓库唯一的 Modulith 架构门禁，只做最小兜底：官方 {@code verify()}、ADR-0042 的两条项目不变式（CLOSED
 * 传递依赖白名单、跨根方向），外加应用根存在断言。超出官方语义的检查不进测试，见 ADR-0045。
 */
@DisplayName("SODA 全仓 Modulith 架构")
class SodaModulithArchitectureTest {

    /**
     * 应用根清单 —— 与 {@link SodaModulith} 的 {@code additionalPackages} 同步维护。
     *
     * <p>此处刻意重复登记：把根从注解里删掉时本断言立刻变红（反射读注解则看不见被删的根）。新增根若漏改此处不损失覆盖（根只要登记进注解即被扫描）。
     */
    private static final String[] APPLICATION_ROOTS = {"com.soda.component", "com.soda.user"};

    private static final ApplicationModules MODULES = ApplicationModules.of(SodaModulith.class);

    @Test
    @DisplayName("模块依赖关系符合白名单与 API 边界")
    void should_verify_when_moduleStructureFollowsConvention() {
        MODULES.verify();
    }

    @Test
    @DisplayName("CLOSED 模块的传递依赖均已声明")
    void should_declareAllTransitiveDependencies_when_closedModulesFollowConvention() {
        var violations = new ArrayList<String>();

        MODULES.forEach(
                module -> {
                    if (module.isOpen()) {
                        return;
                    }

                    var actualDependencies =
                            actualDependenciesOf(module)
                                    .map(ApplicationModule::getIdentifier)
                                    .collect(Collectors.toSet());
                    var declaredDependencies =
                            module.getAllowedDependencies(MODULES).stream()
                                    .map(dependency -> dependency.getTargetModule().getIdentifier())
                                    .collect(Collectors.toSet());
                    var undeclaredDependencies =
                            actualDependencies.stream()
                                    .filter(
                                            dependency ->
                                                    !declaredDependencies.contains(dependency))
                                    .collect(Collectors.toSet());

                    if (!undeclaredDependencies.isEmpty()) {
                        violations.add(
                                "Module "
                                        + module.getIdentifier()
                                        + " uses undeclared dependencies: "
                                        + undeclaredDependencies);
                    }
                });

        assertThat(violations)
                .as("CLOSED modules must not use undeclared dependencies: %s", violations)
                .isEmpty();
    }

    @Test
    @DisplayName("跨根依赖保持业务层到框架层单向")
    void should_keepCrossRootDependenciesOneWay() {
        var violations = new ArrayList<String>();

        MODULES.forEach(
                module -> {
                    var source = module.getBasePackage().getName();
                    var sourceRoot = rootOf(source);
                    actualDependenciesOf(module)
                            .map(ApplicationModule::getBasePackage)
                            .map(JavaPackage::getName)
                            .filter(target -> !rootOf(target).equals(sourceRoot))
                            .sorted()
                            .forEach(
                                    target -> {
                                        var targetRoot = rootOf(target);
                                        if (isFrameworkLayer(sourceRoot)
                                                && !isFrameworkLayer(targetRoot)) {
                                            violations.add(
                                                    "框架层反向依赖业务层: " + source + " -> " + target);
                                        } else if (!isFrameworkLayer(sourceRoot)
                                                && !isFrameworkLayer(targetRoot)) {
                                            violations.add("业务层跨根耦合: " + source + " -> " + target);
                                        }
                                    });
                });

        assertThat(violations).as("跨根依赖须为「业务层 → 框架层」单向: %s", violations).isEmpty();
    }

    @Test
    @DisplayName("两个应用根均在模块图中")
    void should_coverEachApplicationRoot_when_rootsAreRegistered() {
        var basePackages = new ArrayList<String>();
        MODULES.forEach(module -> basePackages.add(module.getBasePackage().getName()));

        var missing =
                Arrays.stream(APPLICATION_ROOTS)
                        .filter(
                                root ->
                                        basePackages.stream()
                                                .noneMatch(
                                                        pkg ->
                                                                pkg.equals(root)
                                                                        || pkg.startsWith(
                                                                                root + ".")))
                        .toList();

        assertThat(missing)
                .as("应用根必须登记在 SodaModulith.additionalPackages，否则门禁看不见它: %s", basePackages)
                .isEmpty();
    }

    /** 模块的全部实际依赖（含传递）唯一化后的模块流；口径只在本测试内维护，见 ADR-0045。 */
    private static Stream<ApplicationModule> actualDependenciesOf(ApplicationModule module) {
        return module.getDependencies(
                        MODULES,
                        DependencyDepth.ALL,
                        DependencyType.USES_COMPONENT,
                        DependencyType.ENTITY,
                        DependencyType.EVENT_LISTENER,
                        DependencyType.DEFAULT)
                .uniqueModules();
    }

    private static String rootOf(String packageName) {
        var parts = packageName.split("\\.");
        return parts.length > 2 ? parts[2] : packageName;
    }

    /** 框架层只认 {@code com.soda.component}；其余应用根一律业务层（新增业务根无需改此判定）。 */
    private static boolean isFrameworkLayer(String root) {
        return "component".equals(root);
    }
}
