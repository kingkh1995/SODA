package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.component.domain.types.UUId;
import com.soda.user.domain.Verification;

import java.time.Instant;

/**
 * 验证实体创建事件 — 请求发码用例创建 INITIALIZED 验证实体后注册。
 * <p>
 * 在 {@link com.soda.user.domain.Verification} 的 create 工厂内注册（实体创建即需投递验证码）。
 * 由 ApplicationService 在持久化后经 {@link com.soda.component.domain.DomainEventBus} 发布；
 * 投递侧监听器（AFTER_COMMIT，见 ADR-0011）收到后按 {@code recipient} 类型匹配 sender 投递
 * 并落库 PENDING（channel 只在 VerificationRecipient，2026-08-16 见 ADR-0026）——记录先于发送持久化，
 * 且 DB 事务不跨外部投递通道持有。
 * <p>
 * 实体引用为单一载荷：创建路径 ID 由客户端生成（{@code UUId.random()}），发布时已可用，
 * 监听器直接调用实体行为，无需反查。
 *
 * @param verification 已创建的验证实体（INITIALIZED）
 * @param occurredAt   事件发生时间
 */
public record VerificationCreatedEvent(Verification verification, Instant occurredAt)
        implements DomainEvent<UUId> {

    /**
     * 默认使用当前时间。
     */
    public VerificationCreatedEvent(Verification verification) {
        this(verification, Instant.now());
    }

    @Override
    public UUId entityId() {
        return verification.getId();
    }
}
