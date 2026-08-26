/**
 * 用户领域模块：用户聚合根、值对象、领域服务、领域事件与网关端口。
 * <p>
 * 核心业务不变量（状态机、唯一性、凭证变更）在此定义；不依赖其他业务模块（见 ADR-0017、ADR-0026）。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
package com.soda.user.domain;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
