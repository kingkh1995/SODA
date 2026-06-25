package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 用户昵称 DP — 最多 30 字符，禁止空白字符（空格、Tab、换行等），不可变、自校验、可比较。
 *
 * @see Type
 */
public record Nickname(@JsonValue String value) implements Type, Comparable<Nickname> {

    private static final int MAX_LENGTH = 30;
    private static final Pattern NO_WHITESPACE = Pattern.compile("^\\S+$");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Nickname {
        ValidateUtils.nonBlank(value);
        ValidateUtils.maxLength(MAX_LENGTH, value);
        ValidateUtils.matches(NO_WHITESPACE, value);
    }

    @Override
    public int compareTo(Nickname other) {
        return this.value.compareTo(other.value);
    }
}
