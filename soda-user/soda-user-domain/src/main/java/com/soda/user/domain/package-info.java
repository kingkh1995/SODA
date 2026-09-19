/**
 * 用户领域模块：用户聚合根、值对象、领域服务、领域事件与网关端口。
 *
 * <p>核心业务不变量（状态机、唯一性、凭证变更）在此定义；不依赖其他业务模块（见 ADR-0017、ADR-0026）。
 *
 * <p>模块面收窄为 CLOSED：仅 {@code types} / {@code gateway} / {@code service} / {@code event} 四个
 * {@code @NamedInterface} 子包可被其他模块引用，基包与其余子包属内部实现。
 */
@NullMarked
@ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        allowedDependencies = {"com.soda.component.domain"})
package com.soda.user.domain;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
