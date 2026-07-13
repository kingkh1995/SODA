package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.types.UserId;

import java.time.Instant;

/**
 * 密码变更事件 — 当 User 的登录密码被修改时触发。
 *
 * @param entityId   密码变更的 User 标识符
 * @param occurredAt 事件发生时间
 */
public record PasswordChangedEvent(
        UserId entityId,
        Instant occurredAt
) implements DomainEvent<UserId> {

    public PasswordChangedEvent(UserId entityId) {
        this(entityId, Instant.now());
    }
}
