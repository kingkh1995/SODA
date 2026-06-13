package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 电子邮箱 DP — 不可变、自校验、可比较。
 * <p>
 * 校验规则：非空、格式匹配 {@code local@domain}，域名至少包含一个 {@code .}。
 * 值归一化为小写。
 * <p>
 * 非标识符 DP，直接实现 {@link Type}。
 *
 * @see Type
 */
public record Email(@JsonValue String value) implements Type {

    @Serial
    private static final long serialVersionUID = 1L;

    // 基础邮箱格式校验：local@domain.tld
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public Email {
        ValidateUtils.nonBlank(value);
        value = value.trim();
        ValidateUtils.matches(EMAIL_PATTERN, value);
        value = value.toLowerCase(Locale.ENGLISH);
    }

    /** 从不可靠输入构造，null、blank 或格式不匹配时抛出 {@link IllegalArgumentException}。 */
    public static Email valueOf(Object value) {
        return new Email(ParseUtils.parseString(value));
    }

    /** 返回 {@code @} 前的本地部分。 */
    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    /** 返回 {@code @} 后的域名部分。 */
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    @Override
    public int compareTo(Type other) {
        return value.compareTo(((Email) other).value);
    }
}
