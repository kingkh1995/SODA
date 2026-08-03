package com.soda.user.domain.event;

import com.soda.component.domain.DomainEvent;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;

import java.time.Instant;

/**
 * 用户状态变更事件 — 当 User 的状态被修改时触发。
 *
 * @param entityId   状态变更的 User 标识符
 * @param occurredAt 事件发生时间
 * @param oldState   变更前的状态
 * @param newState   变更后的状态
 */
public record UserStateChangedEvent(
        UserId entityId,
        Instant occurredAt,
        UserState oldState,
        UserState newState
) implements DomainEvent<UserId> {

    public UserStateChangedEvent(UserId entityId, UserState oldState, UserState newState) {
        this(entityId, Instant.now(), oldState, newState);
    }
}
