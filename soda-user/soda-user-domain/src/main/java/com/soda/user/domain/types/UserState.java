package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.StateEnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户状态枚举。取值：{@code E}（Enabled）、{@code D}（Disabled）、{@code R}（Removed / 注销，吸收态终态）。
 * <p>
 * 状态跃迁通过 User 的方法 {@code disable()} / {@code enable()} / {@code deregister()} 表达，
 * 不暴露泛化的 changeState（见 ADR-0017）。终态以覆写 {@code terminal()} 标记（仅 R 返回 true），
 * {@link #terminal()} 供基础设施兜底守卫消费（终态行不可写，ADR-0023）。
 *
 * @see StateEnumType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum UserState implements StateEnumType {

    E("enabled"),
    D("disabled"),
    R("deregistered") {
        @Override
        public boolean terminal() {
            return true;
        }
    };

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserState of(String name) {
        return ParseUtils.parseEnum(UserState.class, name);
    }
}