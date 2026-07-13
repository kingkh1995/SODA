/**
 * 定时任务 — 时间触发，通过 {@code api} 接口契约调用应用服务。
 * <p>
 * 只依赖 {@code api} 模块的接口，{@code app} 实现由 Spring DI 注入。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.job;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
