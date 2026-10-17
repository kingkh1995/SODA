/**
 * 用户领域模块：用户聚合根、值对象、领域服务、领域事件与网关端口。
 *
 * <p>核心业务不变量（状态机、唯一性、凭证变更）在此定义；不依赖其他业务模块（见 ADR-0017、ADR-0026）。
 *
 * <p>模块类型刻意保持 OPEN：这是 User 侧共享领域 module，application 与 infrastructure 直接使用其 {@code types} / {@code
 * gateway} / {@code service} / {@code event} 子包；这是对通用 CLOSED 判据的明确例外，不引入 named interface。
 */
@NullMarked
@ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        allowedDependencies = {"com.soda.component.domain"})
package com.soda.user.domain;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
