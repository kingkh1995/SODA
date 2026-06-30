package com.soda.user.domain;

import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.util.regex.Pattern;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户名 DP — 4-30 位字母数字，不可变、自校验、可比较。
 * <p>
 * 唯一性由 {@link UserGateway#existsByUsername(Username)} 和 DB 唯一索引双重保证。
 *
 * @see Type
 */
public record Username(String value) implements Type, Comparable<Username> {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,30}$");

    public Username {
        ValidateUtils.hasText(value);
        ValidateUtils.matches(value, USERNAME_PATTERN);
    }

    @JsonValue
    public String value() { return this.value; }

    @Override
    public int compareTo(Username other) {
        return this.value.compareTo(other.value);
    }
}
