package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.types.UserId;

import java.time.Instant;

/**
 * 用户删除事件 — 当 User 被删除时触发。
 *
 * @param entityId   被删除的 User 标识符
 * @param occurredAt 事件发生时间
 */
public record UserRemovedEvent(
        UserId entityId,
        Instant occurredAt
) implements DomainEvent<UserId> {

    public UserRemovedEvent(UserId entityId) {
        this(entityId, Instant.now());
    }
}
