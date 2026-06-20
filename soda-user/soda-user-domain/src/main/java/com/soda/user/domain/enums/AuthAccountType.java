package com.soda.user.domain.enums;

import com.soda.component.domain.EnumType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 认证账户类型枚举 — 标识认证方式。
 * <p>
 * DTO/VO 不直接引用枚举类型，通过 {@link #name()} 字符串传递。
 * 数据库存储 {@link #name()} 值（{@code "P"} / {@code "S"} / {@code "E"} / {@code "O"}）。
 *
 * @see Sex
 * @see UserStatus
 * @see SocialType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum AuthAccountType implements EnumType {

    P("Password"),
    S("Sms"),
    E("Email"),
    O("OAuth");

    private final String desc;
}
