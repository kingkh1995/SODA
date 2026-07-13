/**
 * 领域事件处理器 — 响应 {@code domain/event/} 的 DomainEvent。
 * <p>
 * 每个 Handler 处理一种 DomainEvent 类型，触发后续编排（发通知、更新读模型等）。
 */
@NullMarked
package com.soda.user.application.event;

import org.jspecify.annotations.NullMarked;
