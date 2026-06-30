/**
 * 适配层（Controller / Assembler）基类定义包。
 * <p>
 * 默认所有类型、方法参数和返回值均为 {@code @NonNull}，
 * 仅在标记 {@code @Nullable} 处允许空值。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"app"})
package com.soda.component.adapter;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
