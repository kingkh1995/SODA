/**
 * 用户应用层模块：编排用户聚合根的用例流程（加载 → 执行 → 持久化）。
 * <p>
 * 包含 ApplicationService 实现、命令/查询处理器、事件处理器、工厂与转换器。
 * 依赖 api 与 domain 模块，不暴露实现类给 adapter（见 ADR-0026、ADR-0019）。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api", "domain"})
package com.soda.user.application;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;