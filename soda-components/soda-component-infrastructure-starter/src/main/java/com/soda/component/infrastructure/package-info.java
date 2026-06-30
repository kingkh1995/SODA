/**
 * 基础设施层持久化基类及 Repository 骨架定义包。
 * <p>
 * 默认所有类型、方法参数和返回值均为 {@code @NonNull}，
 * 仅在标记 {@code @Nullable} 处允许空值。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"domain"})
package com.soda.component.infrastructure;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
