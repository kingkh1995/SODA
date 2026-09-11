/**
 * web 入站通道公共类型包 —— 公开入口：{@code IfMatch}（If-Match 头注入）与 {@code HttpValidatorSource}（条件请求 validator 源）。
 * <p>
 * 内部实现沉于子包（由 Spring 自动发现，业务模块不直接引用）：{@code conditional}（条件请求出站头派生 + 入站参数解析，见 ADR-0039）、
 * {@code error}（异常 → ProblemDetail 翻译，见 ADR-0013）；{@code validation} 提供 Bean Validation 约束。
 * 不提供 WebMVC Controller 基类——Controller/Assembler 由业务模块 adapter 子模块自行实现。
 * <p>
 * 依赖 {@code api} 层获取 DTO 类型和接口契约；{@code application} 实现由 Spring DI 注入。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.component.web;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
