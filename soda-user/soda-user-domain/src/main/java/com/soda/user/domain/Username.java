package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 用户名 DP — 4-30 位字母数字，不可变、自校验、可比较。
 * <p>
 * 唯一性由 {@link UserGateway#existsByUsername(Username)} 和 DB 唯一索引双重保证。
 *
 * @see Type
 */
public record Username(@JsonValue String value) implements Type, Comparable<Username> {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,30}$");
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Username {
        ValidateUtils.nonBlank(value);
        ValidateUtils.matches(USERNAME_PATTERN, value);
    }

    @Override
    public int compareTo(Username other) {
        return this.value.compareTo(other.value);
    }
}
