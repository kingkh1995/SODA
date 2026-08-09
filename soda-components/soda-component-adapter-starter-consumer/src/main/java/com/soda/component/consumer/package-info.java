/**
 * 消费者基类包 — 待跨服务 MQ 集成时补充通用类型。
 * <p>
 * consumer 用于接收其他服务发布的消息（MQ），
 * 模块内部的领域解耦应使用 {@link com.soda.component.domain.DomainEvent}。
 */
@NullMarked
@ApplicationModule(type = ApplicationModule.Type.CLOSED, allowedDependencies = {"api"})
package com.soda.component.consumer;

import org.jspecify.annotations.NullMarked;

import org.springframework.modulith.ApplicationModule;
