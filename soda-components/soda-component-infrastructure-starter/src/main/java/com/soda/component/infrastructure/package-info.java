/**
 * 基础设施层公共类型包 —— 公开入口（根包即 API）：持久化载体骨架 {@code AbstractPersistable} / {@code AbstractAuditable} /
 * {@code Auditable}。
 *
 * <p>内部实现沉于子包（由 Spring 自动发现，业务模块不直接引用）：{@code persistence}（JPA 审计自动配置）、 {@code
 * eventbus}（领域事件总线实现与自动配置）。
 *
 * <p>默认所有类型、方法参数和返回值均为 {@code @NonNull}， 仅在标记 {@code @Nullable} 处允许空值。
 */
@NullMarked
@ApplicationModule(
        type = ApplicationModule.Type.CLOSED,
        allowedDependencies = {"com.soda.component.domain"})
package com.soda.component.infrastructure;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
