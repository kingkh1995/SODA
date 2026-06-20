package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;
import java.util.regex.Pattern;

/**
 * 验证码值 DP — 字母数字串，不可变、自校验、可比较。
 * <p>
 * 校验规则：非 blank，仅限字母数字。
 *
 * @see Type
 */
public record CodeValue(@JsonValue String value) implements Type, Comparable<CodeValue> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Pattern ALPHANUMERIC = Pattern.compile("^[A-Za-z0-9]+$");

    public CodeValue {
        ValidateUtils.nonBlank(value);
        ValidateUtils.matches(ALPHANUMERIC, value);
    }

    /** 从不可靠输入构造，null、blank 或包含非法字符时抛出 {@link IllegalArgumentException}。 */
    public static CodeValue valueOf(Object value) {
        return new CodeValue(ParseUtils.parseString(value));
    }

    @Override
    public int compareTo(CodeValue other) {
        return this.value.compareTo(other.value);
    }
}
