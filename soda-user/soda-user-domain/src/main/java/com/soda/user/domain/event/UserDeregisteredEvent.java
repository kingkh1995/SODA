package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.types.UserId;

import java.time.Instant;

/**
 * 用户注销事件 — 当 User 被注销（进入终态 R）时触发（ADR-0017）。
 * <p>
 * 注销是终态迁移（D→R），不是物理删除——R 的持久化表示（状态列 / 软删 / 删行）由
 * 基础设施层决定，领域不感知擦除。
 *
 * @param entityId   被注销的 User 标识符
 * @param occurredAt 事件发生时间
 */
public record UserDeregisteredEvent(
        UserId entityId,
        Instant occurredAt
) implements DomainEvent<UserId> {

    public UserDeregisteredEvent(UserId entityId) {
        this(entityId, Instant.now());
    }
}
