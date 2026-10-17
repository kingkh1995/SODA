package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.component.domain.types.Uuid;
import com.soda.user.domain.Verification;

import java.time.Instant;

/**
 * 验证实体创建事件 — 请求发码用例创建 INITIALIZED 验证实体后注册。
 *
 * <p>在 {@link com.soda.user.domain.Verification} 的 create 工厂内注册（实体创建即需投递验证码）。由 ApplicationService
 * 在事务内持久化后经 {@link com.soda.component.domain.DomainEventBus} 发布；投递侧普通 {@code @EventListener} 收到后按
 * {@code recipient} 类型匹配 sender 投递，并在同一事务内落库 PENDING（channel 只在 VerificationRecipient，见 ADR-0026）。
 *
 * <p>实体引用为本地进程内事件载荷：创建路径 ID 由客户端生成（{@code Uuid.random()}），监听器直接调用实体行为，无需反查；本事件不承诺 outbox 或序列化（见
 * ADR-0044）。
 *
 * @param verification 已创建的验证实体（INITIALIZED）
 * @param occurredAt 事件发生时间
 */
public record VerificationCreatedEvent(Verification verification, Instant occurredAt)
        implements DomainEvent<Uuid> {

    public VerificationCreatedEvent(Verification verification) {
        this(verification, Instant.now());
    }

    @Override
    public Uuid entityId() {
        return verification.getId();
    }
}
