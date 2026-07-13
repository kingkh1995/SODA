/**
 * 消费者 — 跨服务消息队列监听，通过 {@code api} 接口契约处理消息。
 * <p>
 * 只依赖 {@code api} 模块的接口，{@code app} 实现由 Spring DI 注入。
 * <p>
 * 当前无具体消费者，预留骨架待跨服务 MQ 集成时补充。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.user.consumer;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
