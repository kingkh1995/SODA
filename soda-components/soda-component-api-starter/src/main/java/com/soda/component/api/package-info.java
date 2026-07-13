/**
 * API 层共享 DTO / Command / Query 基类定义包。
 * <p>
 * 默认所有类型、方法参数和返回值均为 {@code @NonNull}，
 * 仅在标记 {@code @Nullable} 处允许空值。
 * <p>
 * 业务模块的 {@code -api} 模块以本模块为框架依赖，建议在 {@code com.soda.xxx.api} 根包下采用如下子包分类：
 * <ul>
 *   <li>{@code command/} — Command record（增删改请求参数）</li>
 *   <li>{@code dto/} — 返回型 DTO / ClientObject</li>
 *   <li>{@code query/} — Query record（查询请求参数）</li>
 *   <li>{@code context/} — 请求上下文（用户身份、租户等）</li>
 * </ul>
 * Service 接口直接放在 {@code com.soda.xxx.api} 根包。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
package com.soda.component.api;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
