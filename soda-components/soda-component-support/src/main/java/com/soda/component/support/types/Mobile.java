package com.soda.component.support.types;

import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.util.regex.Pattern;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 手机号 DP — 中国手机号格式校验 + 归一化，不可变、自校验、可比较。
 * <p>
 * 校验规则：1 开头，11 位数字。
 *
 * @see Type
 */
public record Mobile(String value) implements Type {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    public Mobile {
        ValidateUtils.hasText(value);
        ValidateUtils.matches(value, MOBILE_PATTERN);
    }

    @JsonValue
    public String value() { return this.value; }

}
