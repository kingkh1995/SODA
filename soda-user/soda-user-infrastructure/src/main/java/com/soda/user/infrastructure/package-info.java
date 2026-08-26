/**
 * 用户基础设施层模块：实现领域网关契约（持久化、查询、外部适配）。
 * <p>
 * 包含 Gateway 实现、Repository 适配、PO 转换器与数据库迁移。
 * 仅依赖 domain 模块，实现细节不外泄（见 ADR-0023、ADR-0024）。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"domain"})
package com.soda.user.infrastructure;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;