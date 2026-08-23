package com.soda.user.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 用户昵称 DP — 最多 30 字符，禁止空白字符（空格、Tab、换行等），不可变、自校验、可比较。
 *
 * @see StringLiteralType
 */
public record Nickname(String value) implements StringLiteralType, Comparable<Nickname> {

    private static final int MAX_LENGTH = 30;

    private static final Pattern NO_WHITESPACE = Pattern.compile("^\\S+$");

    public Nickname {
        ValidateUtils.hasText(value);
        ValidateUtils.maxLength(value, MAX_LENGTH);
        ValidateUtils.matches(value, NO_WHITESPACE);
    }

    @Override
    public int compareTo(Nickname other) {
        return value.compareTo(other.value);
    }
}
