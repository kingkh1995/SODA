package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Identifier;
import com.soda.component.support.util.ValidateUtils;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * UUID 格式标识符 DP — 不可变、自校验、可比较。
 * <p>
 * 校验规则：格式匹配 {@code 8-4-4-4-12} 十六进制，归一化为小写。
 * 提供 {@link #random()} 工厂方法，等价于 {@code java.util.UUID.randomUUID()}。
 * <p>
 * 替换了 {@code StringId}：UUID 提供严格格式校验，而非仅非空字符串。
 *
 * @see Identifier
 */
public record UUId(String value) implements Identifier<String>, Comparable<UUId> {

    /**
     * 自动生成策略 — 用于客户端生成 {@code Entity(Supplier)} 构造器。
     */
    public static final Supplier<UUId> AUTO = UUId::random;
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");


    public UUId {
        ValidateUtils.hasText(value);
        value = value.toLowerCase(Locale.ROOT);
        ValidateUtils.matches(value, UUID_PATTERN);
    }

    /**
     * 生成随机 UUID，等价于 {@link UUID#randomUUID()}。
     */
    public static UUId random() {
        return new UUId(UUID.randomUUID().toString());
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @Override
    public String identifier() {
        return value;
    }

    @Override
    public int compareTo(UUId other) {
        return this.value.compareTo(other.value);
    }
}
