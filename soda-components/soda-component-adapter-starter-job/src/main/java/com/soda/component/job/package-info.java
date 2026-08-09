/**
 * 定时任务上下文包。
 * <p>
 * 包含调度上下文 {@link JobContext}。刻意不绑定具体调度框架（Quartz/XXL-JOB 由外部适配构造），
 * 业务 Job 以 JobContext 为入参。
 * <p>
 * 通过 {@code api} 接口契约调用应用服务；{@code application} 实现由 Spring DI 注入。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.component.job;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
