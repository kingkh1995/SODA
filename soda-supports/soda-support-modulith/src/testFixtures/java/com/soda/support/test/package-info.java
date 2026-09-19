/** Modulith 测试支持模块 —— 供各应用根测试复用的模块结构校验工具（ModulithTestSupport）。 */
@ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        allowedDependencies = {})
package com.soda.support.test;

import org.springframework.modulith.ApplicationModule;
