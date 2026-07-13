package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.types.AuthAccountId;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.UserId;

import java.time.Instant;

/**
 * 认证账户绑定事件 — 当新的认证账户被绑定到 User 时触发。
 *
 * @param entityId    绑定账户的 User 标识符
 * @param occurredAt  事件发生时间
 * @param accountType 绑定的认证账户类型
 * @param accountId   绑定的认证账户标识符
 */
public record AccountBoundEvent(
        UserId entityId,
        Instant occurredAt,
        AuthAccountType accountType,
        AuthAccountId accountId
) implements DomainEvent<UserId> {

    public AccountBoundEvent(UserId entityId, AuthAccountType accountType, AuthAccountId accountId) {
        this(entityId, Instant.now(), accountType, accountId);
    }
}
