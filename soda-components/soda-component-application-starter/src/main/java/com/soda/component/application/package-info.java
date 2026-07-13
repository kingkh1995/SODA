/**
 * Application Service 基类及编排基础设施定义包。
 * <p>
 * 默认所有类型、方法参数和返回值均为 {@code @NonNull}，
 * 仅在标记 {@code @Nullable} 处允许空值。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"domain", "api"})
package com.soda.component.application;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
