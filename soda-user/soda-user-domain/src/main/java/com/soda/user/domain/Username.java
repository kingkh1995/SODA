package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;
import java.util.regex.Pattern;

/**
 * 用户名 DP — 4-30 位字母数字，不可变、自校验、可比较。
 * <p>
 * 唯一性由 {@link UserGateway#existsByUsername(Username)} 和 DB 唯一索引双重保证。
 *
 * @see Type
 */
public record Username(@JsonValue String value) implements Type, Comparable<Username> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]{4,30}$");

    public Username {
        ValidateUtils.nonBlank(value);
        value = value.trim();
        ValidateUtils.matches(USERNAME_PATTERN, value);
    }

    /** 从不可靠输入构造，null、blank 或格式不匹配时抛出 {@link IllegalArgumentException}。 */
    public static Username valueOf(Object value) {
        return new Username(ParseUtils.parseString(value));
    }

    @Override
    public int compareTo(Username other) {
        return this.value.compareTo(other.value);
    }
}
