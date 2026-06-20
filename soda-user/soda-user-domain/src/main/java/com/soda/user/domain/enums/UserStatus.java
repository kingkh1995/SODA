package com.soda.user.domain.enums;

import com.soda.component.domain.EnumType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户状态枚举 — 启用 / 禁用。
 * <p>
 * DTO/VO 不直接引用枚举类型，通过 {@link #name()} 字符串传递。
 * 数据库存储 {@link #name()} 值（{@code "E"} / {@code "D"}）。
 *
 * @see Sex
 * @see AuthAccountType
 * @see SocialType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum UserStatus implements EnumType {

    E("Enabled"),
    D("Disabled");

    private final String desc;
}
