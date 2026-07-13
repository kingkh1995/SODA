package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.User;
import com.soda.user.domain.types.UserId;

import java.time.Instant;

/**
 * 用户创建事件 — 当 User 聚合根被创建时触发。
 * <p>
 * 在 {@link User#createBuilder()} 的 {@code build()} 中通过
 * {@link com.soda.component.domain.Entity#registerEvent} 注册。
 * <p>
 * {@code entityId} 通过 {@link #user()} 实体引用延迟求值——事件注册时 ID 可能尚未分配
 * （由 Repository 的 {@code save()} 调用 {@code assignId()} 填补），
 * 但事件持有实体引用，任何时候 {@code user().getId()} 都能取到最新 ID。
 *
 * @param user       创建的用户实体
 * @param occurredAt 事件发生时间
 */
public record UserCreatedEvent(User user, Instant occurredAt)
        implements DomainEvent<UserId> {

    /**
     * 默认使用当前时间。
     */
    public UserCreatedEvent(User user) {
        this(user, Instant.now());
    }

    @Override
    public UserId entityId() {
        return user().getId();
    }
}
