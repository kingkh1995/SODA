package com.soda.user.domain;

import com.soda.component.domain.Aggregate;

/**
 * 用户聚合根 — （占位，待 Issue 04 实现完整实体）。
 * <p>
 * 当前仅提供使 {@link UserGateway} 编译通过的最小定义。
 *
 * @see Aggregate
 */
public class User extends Aggregate<UserId> {

    /** 用于持久化恢复的构造。 */
    public User(UserId id) {
        super(id);
    }

    /** 用于创建的构造 — 服务端生成 ID。 */
    public User() {
        super();
    }
}
