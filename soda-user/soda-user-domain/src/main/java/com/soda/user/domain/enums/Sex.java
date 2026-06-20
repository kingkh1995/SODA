package com.soda.user.domain.enums;

import com.soda.component.domain.EnumType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 性别枚举 — 短名标识持久化到数据库 {@link #name()}。
 * <p>
 * DTO/VO 不直接引用枚举类型，通过 {@link #name()} 字符串传递。
 *
 * @see UserStatus
 * @see AuthAccountType
 * @see SocialType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Sex implements EnumType {

    M("Male"),
    F("Female");

    private final String desc;
}
