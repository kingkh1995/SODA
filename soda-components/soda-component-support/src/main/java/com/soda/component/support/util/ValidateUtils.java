package com.soda.component.support.util;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * 校验工具类 — 基础参数校验，校验失败抛出 {@link IllegalArgumentException}。
 * <p>
 * 参考 kk-ddd 的 {@code ValidateUtils} 设计。
 * <p>校验方法参数惯例：辅助参数在前，被校验值（命名均为 {@code value}）在最后。
 * 只有被校验值标注 {@link Nullable} 并校验，辅助参数视为可信。
 */
public final class ValidateUtils {

    private ValidateUtils() {
        throw new UnsupportedOperationException();
    }

    public static void notNull(@Nullable Object value) {
        if (value == null) {
            throw IllegalArgumentExceptions.forIsNull();
        }
    }

    public static void nonBlank(@Nullable String value) {
        if (value == null || value.isBlank()) {
            throw IllegalArgumentExceptions.forIsBlank();
        }
    }

    public static void minValue(long min, boolean minInclusive, long value) {
        var cmp = Long.compare(value, min);
        if ((minInclusive && cmp < 0) || (!minInclusive && cmp <= 0)) {
            throw IllegalArgumentExceptions.forMinValue(value, min, minInclusive);
        }
    }

    public static <T extends Comparable<? super T>> void minValue(T min, boolean minInclusive, @Nullable T value) {
        notNull(value);
        var cmp = value.compareTo(min);
        if ((minInclusive && cmp < 0) || (!minInclusive && cmp <= 0)) {
            throw IllegalArgumentExceptions.forMinValue(value, min, minInclusive);
        }
    }

    public static void matches(Pattern pattern, @Nullable String value) {
        nonBlank(value);
        if (!pattern.matcher(value).matches()) {
            throw IllegalArgumentExceptions.forInvalidFormat(value);
        }
    }

    public static void maxScale(int max, @Nullable BigDecimal value) {
        notNull(value);
        if (value.scale() > max) {
            throw IllegalArgumentExceptions.forMaxScale(value.scale(), max);
        }
    }

    public static void maxLength(int max, @Nullable String value) {
        nonBlank(value);
        if (value.length() > max) {
            throw IllegalArgumentExceptions.forMaxLength(value.length(), max);
        }
    }

    public static void maxValue(int max, boolean inclusive, int value) {
        var cmp = Integer.compare(value, max);
        if ((inclusive && cmp > 0) || (!inclusive && cmp >= 0)) {
            throw IllegalArgumentExceptions.forMaxValue(value, max, inclusive);
        }
    }

    public static void range(int min, int max, int value) {
        if (value < min || value > max) {
            throw IllegalArgumentExceptions.forOutOfRange(value, min, max);
        }
    }

    public static void validUri(@Nullable String value) {
        nonBlank(value);
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw IllegalArgumentExceptions.forInvalidUriFormat(value);
        }
        if (!uri.isAbsolute()) {
            throw IllegalArgumentExceptions.forRelativeUri(value);
        }
        var scheme = uri.getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw IllegalArgumentExceptions.forInvalidUriScheme(value, scheme);
        }
    }
    /** 校验字符串以指定前缀开头，不匹配时抛出 {@link IllegalArgumentException}。 */
    public static void hasPrefix(String prefix, @Nullable String value) {
        nonBlank(value);
        if (!value.startsWith(prefix)) {
            throw IllegalArgumentExceptions.forMissingPrefix(prefix, value);
        }
    }
}
