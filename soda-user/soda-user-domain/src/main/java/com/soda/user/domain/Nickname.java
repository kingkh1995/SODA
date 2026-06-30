package com.soda.user.domain;

import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.util.regex.Pattern;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户昵称 DP — 最多 30 字符，禁止空白字符（空格、Tab、换行等），不可变、自校验、可比较。
 *
 * @see Type
 */
public record Nickname(String value) implements Type, Comparable<Nickname> {

    private static final int MAX_LENGTH = 30;

    private static final Pattern NO_WHITESPACE = Pattern.compile("^\\S+$");

    public Nickname {
        ValidateUtils.hasText(value);
        ValidateUtils.maxLength(value, MAX_LENGTH);
        ValidateUtils.matches(value, NO_WHITESPACE);
    }

    @JsonValue
    public String value() { return this.value; }

    @Override
    public int compareTo(Nickname other) {
        return this.value.compareTo(other.value);
    }
}
