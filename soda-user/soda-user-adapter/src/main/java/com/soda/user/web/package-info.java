/**
 * REST Controller — 接受 HTTP 请求，委托 api 层接口执行，返回 {@code Result} 信封。
 * <p>
 * 依赖 {@code api} 模块的接口和命令类型，{@code app} 实现由 Spring DI 注入。
 * 命令为 primitive 形状（per-use-case），{@code userId} 由路径参数传入（资源级端点，见 ADR-0026）。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.web;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;