package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserStatus;

import java.time.Instant;

/**
 * 用户状态变更事件 — 当 User 的状态被修改时触发。
 *
 * @param entityId   状态变更的 User 标识符
 * @param occurredAt 事件发生时间
 * @param oldStatus  变更前的状态
 * @param newStatus  变更后的状态
 */
public record UserStatusChangedEvent(
        UserId entityId,
        Instant occurredAt,
        UserStatus oldStatus,
        UserStatus newStatus
) implements DomainEvent<UserId> {

    public UserStatusChangedEvent(UserId entityId, UserStatus oldStatus, UserStatus newStatus) {
        this(entityId, Instant.now(), oldStatus, newStatus);
    }
}
