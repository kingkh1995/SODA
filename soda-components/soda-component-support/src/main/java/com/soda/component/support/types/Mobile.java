package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;
import java.util.regex.Pattern;

/**
 * 手机号 DP — 中国手机号格式校验 + 归一化，不可变、自校验、可比较。
 * <p>
 * 校验规则：1 开头，11 位数字。
 * 归一化：去除空白字符。
 *
 * @see Type
 */
public record Mobile(@JsonValue String value) implements Type, Comparable<Mobile> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    public Mobile {
        ValidateUtils.nonBlank(value);
        value = value.trim();
        ValidateUtils.matches(MOBILE_PATTERN, value);
    }


    @Override
    public int compareTo(Mobile other) {
        return this.value.compareTo(other.value);
    }
}
