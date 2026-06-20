package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * 原始密码 DP — 未经哈希的密码明文，不可变、自校验、可比较。
 * <p>
 * 校验规则：非 blank。不校验格式（密码策略在上层决定）。
 *
 * @see Type
 */
public record RawPassword(@JsonValue String value) implements Type, Comparable<RawPassword> {

    @Serial
    private static final long serialVersionUID = 1L;

    public RawPassword {
        ValidateUtils.nonBlank(value);
    }

    /** 从不可靠输入构造，null 或 blank 时抛出 {@link IllegalArgumentException}。 */
    public static RawPassword valueOf(Object value) {
        return new RawPassword(ParseUtils.parseString(value));
    }

    @Override
    public int compareTo(RawPassword other) {
        return this.value.compareTo(other.value);
    }
}
