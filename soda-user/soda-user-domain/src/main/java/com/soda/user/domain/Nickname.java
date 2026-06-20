package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * 用户昵称 DP — 最多 30 字符，不可变、自校验、可比较。
 *
 * @see Type
 */
public record Nickname(@JsonValue String value) implements Type, Comparable<Nickname> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MAX_LENGTH = 30;

    public Nickname {
        ValidateUtils.nonBlank(value);
        value = value.trim();
        ValidateUtils.maxLength(MAX_LENGTH, value);
    }

    /** 从不可靠输入构造，null、blank 或超长时抛出 {@link IllegalArgumentException}。 */
    public static Nickname valueOf(Object value) {
        return new Nickname(ParseUtils.parseString(value));
    }

    @Override
    public int compareTo(Nickname other) {
        return this.value.compareTo(other.value);
    }
}
