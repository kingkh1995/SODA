/**
 * REST Controller — 接受 HTTP 请求，委托 api 层接口执行，返回结果。
 * <p>
 * 只依赖 {@code api} 模块的接口和命令类型，{@code app} 实现由 Spring DI 注入。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.web;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
