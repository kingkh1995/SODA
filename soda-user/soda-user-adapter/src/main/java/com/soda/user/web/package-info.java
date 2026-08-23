/**
 * REST Controller — 接受 HTTP 请求，委托 api 层接口执行，返回结果。
 * <p>
 * 依赖 {@code api} 模块的接口和命令类型，{@code app} 实现由 Spring DI 注入。
 * 2026-08-16 解除对 {@code domain} 的临时依赖（见 ADR-0026）：assembler 不再构造携带领域
 * 枚举的命令——per-use-case 命令（{@code RequestChangeMobileCodeCommand}）为 primitive 形状，
 * {@code userId} 由路径参数传入（资源级端点，见 ADR-0026 检视修订②，非认证会话）。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.web;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
