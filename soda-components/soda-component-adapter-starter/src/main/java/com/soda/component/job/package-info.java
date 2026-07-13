/**
 * 定时任务基类包。
 * <p>
 * 包含 Scheduled Task / Quartz Job 基类、Cron 表达式注解等。
 * <p>
 * 通过 {@code api} 接口契约调用应用服务；{@code application} 实现由 Spring DI 注入。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.component.job;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
