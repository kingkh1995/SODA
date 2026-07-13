/**
 * REST Controller 基类包。
 * <p>
 * 包含 WebMVC Controller 基类、请求/响应 DTO 转换基类等。
 * <p>
 * 依赖 {@code api} 层获取 DTO 类型和接口契约；{@code application} 实现由 Spring DI 注入。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.component.web;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
