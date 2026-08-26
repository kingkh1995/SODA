package com.soda.component.domain.types;

import com.soda.component.domain.Identifier;
import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * UUID 格式标识符 DP — 不可变、自校验、可比较。
 * <p>
 * 校验规则：格式匹配 {@code 8-4-4-4-12} 十六进制，归一化为小写。
 * 提供 {@link #random()} 工厂方法，等价于 {@code java.util.UUID.randomUUID()}。
 *
 * @see Identifier
 */
public record Uuid(String value) implements StringLiteralType, Identifier<String>, Comparable<Uuid> {

    private static final Pattern PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    public Uuid {
        var normalized = value == null ? null : value.toLowerCase(Locale.ROOT);
        ValidateUtils.matches(normalized, PATTERN);
        value = normalized;
    }

    /**
     * 生成随机 UUID，等价于 {@link UUID#randomUUID()}。
     */
    public static Uuid random() {
        return new Uuid(UUID.randomUUID().toString());
    }

    @Override
    public String identifier() {
        return value;
    }

    @Override
    public int compareTo(Uuid other) {
        return value.compareTo(other.value);
    }
}