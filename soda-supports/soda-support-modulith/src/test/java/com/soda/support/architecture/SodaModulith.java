package com.soda.support.architecture;

import org.springframework.modulith.Modulithic;

/**
 * SODA 全仓 Modulith 校验根。
 *
 * <p>测试类路径纳入除本模块外的全部子项目，扫描范围由本注解的 {@code additionalPackages} 决定；本类型只声明合并扫描配置，不承载业务行为。 应用根清单的兜底断言见
 * {@code SodaModulithArchitectureTest}，治理见 ADR-0045。
 */
@Modulithic(
        systemName = "soda",
        additionalPackages = {"com.soda.component", "com.soda.user"},
        useFullyQualifiedModuleNames = true)
public final class SodaModulith {

    private SodaModulith() {}
}
