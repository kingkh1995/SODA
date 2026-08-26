/**
 * web 入站通道公共类型包。
 * <p>
 * 包含统一响应信封 {@link Result}、错误结构 {@link ErrorInfo}、请求校验注解。
 * 不提供 WebMVC Controller 基类——Controller/Assembler 由业务模块 adapter 子模块自行实现。
 * <p>
 * 依赖 {@code api} 层获取 DTO 类型和接口契约；{@code application} 实现由 Spring DI 注入。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.component.web;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;