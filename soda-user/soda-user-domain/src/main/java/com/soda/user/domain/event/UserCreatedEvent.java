package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.User;
import com.soda.user.domain.types.UserId;

import java.time.Instant;

/**
 * 用户创建事件 — 当 User 聚合根被创建时触发。
 * <p>
 * 在 {@link User#createBuilder()} 的 {@code build()} 中通过
 * {@link com.soda.component.domain.Aggregate#registerEvent} 注册。
 * <p>
 * {@code entityId} 通过 {@link #user()} 实体引用延迟求值——事件注册时 ID 可能尚未分配
 * （由 Repository 的 {@code save()} 调用 {@code assignId()} 填补）。
 * 调用方须在 {@code assignId()} 之后（如 ApplicationService 持久化后 flush）再取 {@code entityId()}；
 * 此前调用抛 {@link NullPointerException}（{@code User.getId()} 防御编程，异常类型即语义）。
 *
 * @param user       创建的用户实体
 * @param occurredAt 事件发生时间
 */
public record UserCreatedEvent(User user, Instant occurredAt)
        implements DomainEvent<UserId> {

    public UserCreatedEvent(User user) {
        this(user, Instant.now());
    }

    @Override
    public UserId entityId() {
        return user().getId();
    }
}
