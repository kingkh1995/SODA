package com.soda.component.support.util;

import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * 校验工具类 — 基础参数校验，校验失败抛出 {@link IllegalArgumentException}。
 * <p>
 * 参考 kk-ddd 的 {@code ValidateUtils} 设计。
 */
public final class ValidateUtils {

    private ValidateUtils() {
        throw new UnsupportedOperationException();
    }

    public static void nonBlank(@Nullable String value) {
        if (value == null || value.isBlank()) {
            throw IllegalArgumentExceptions.forIsBlank();
        }
    }

    public static void minValue(long value, long min, boolean minInclusive) {
        var cmp = Long.compare(value, min);
        if ((minInclusive && cmp < 0) || (!minInclusive && cmp <= 0)) {
            throw IllegalArgumentExceptions.forMinValue(value, min, minInclusive);
        }
    }

    public static void notNull(@Nullable Object value) {
        if (value == null) {
            throw IllegalArgumentExceptions.forIsNull();
        }
    }

    public static void matches(Pattern pattern, String value) {
        if (!pattern.matcher(value).matches()) {
            throw IllegalArgumentExceptions.forInvalidFormat(value);
        }
    }
}
