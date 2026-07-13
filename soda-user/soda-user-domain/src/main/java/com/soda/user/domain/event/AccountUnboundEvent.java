package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.types.AuthAccountId;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.UserId;

import java.time.Instant;

/**
 * 认证账户解绑事件 — 当认证账户从 User 被移除时触发。
 *
 * @param entityId    解绑账户的 User 标识符
 * @param occurredAt  事件发生时间
 * @param accountType 解绑的认证账户类型
 * @param accountId   解绑的认证账户标识符
 */
public record AccountUnboundEvent(
        UserId entityId,
        Instant occurredAt,
        AuthAccountType accountType,
        AuthAccountId accountId
) implements DomainEvent<UserId> {

    public AccountUnboundEvent(UserId entityId, AuthAccountType accountType, AuthAccountId accountId) {
        this(entityId, Instant.now(), accountType, accountId);
    }
}
