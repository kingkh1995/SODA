package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 电子邮箱 DP — 不可变、自校验、可比较。
 * <p>
 * 校验规则：非空、格式匹配 {@code local@domain}，域名至少包含一个 {@code .}。
 * 值归一化为小写。{@link #localPart()} 和 {@link #domain()} 在构造时预计算并缓存。
 * <p>
 * 使用 class+Lombok 实现（非 record）：{@link JsonValue} 在字段上，{@link JsonCreator} 在构造器上，
 * 与项目内基于 {@code Type} 的 DP（如 {@link Mobile}）保持一致的 Jackson 集成方式。
 *
 * @see Type
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Accessors(fluent = true)
public final class Email implements Type {


    // 基础邮箱格式校验：local@domain.tld
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @EqualsAndHashCode.Include
    private final String value;

    private final String localPart;
    private final String domain;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Email(String value) {
        ValidateUtils.hasText(value);
        ValidateUtils.matches(value, EMAIL_PATTERN);
        value = value.toLowerCase(Locale.ROOT);
        var at = value.indexOf('@');
        this.value = value;
        this.localPart = value.substring(0, at);
        this.domain = value.substring(at + 1);
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @Override
    public String toString() {
        return "Email[value=" + value + "]";
    }
}
