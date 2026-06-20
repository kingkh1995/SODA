package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * 头像 URL DP — URI 格式校验，不可变、自校验、可比较。
 *
 * @see Type
 */
public record Avatar(@JsonValue String value) implements Type, Comparable<Avatar> {

    @Serial
    private static final long serialVersionUID = 1L;

    public Avatar {
        ValidateUtils.nonBlank(value);
        value = value.trim();
        ValidateUtils.validUri(value);
    }

    /**
     * 从不可靠输入构造，null、blank 或 URI 格式不匹配时抛出 {@link IllegalArgumentException}.
     */
    public static Avatar valueOf(Object value) {
        return new Avatar(ParseUtils.parseString(value));
    }

    @Override
    public int compareTo(Avatar other) {
        return this.value.compareTo(other.value);
    }
}
