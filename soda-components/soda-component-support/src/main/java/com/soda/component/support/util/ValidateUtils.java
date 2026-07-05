package com.soda.component.support.util;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.net.URI;
import java.util.regex.Pattern;

/**
 * 校验工具类 — 基础参数校验，校验失败抛出 {@link IllegalArgumentException}。
 * <p>
 * 参考 Spring {@code Assert} 设计，区别在于本工具类使用固定的默认错误消息，
 * 调用方无需每次传入消息字符串。参数顺序与 Spring Assert 一致：被校验值在前，
 * 辅助参数在后。
 * <p>
 * 仅负责校验，不做任何输入处理（trim、舍入等）。
 * 被校验值标注 {@link Nullable} 并校验，辅助参数视为可信。
 */
public final class ValidateUtils {

    private ValidateUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 非 null 校验。
     */
    public static void notNull(@Nullable Object value) {
        Assert.notNull(value, "must not be null");
    }

    /**
     * 非 blank 校验。
     */
    public static void hasText(@Nullable String value) {
        Assert.hasText(value, "must not be blank");
    }

    /**
     * 最小值校验（long 原始类型）。
     */
    public static void minValue(long value, long min, boolean inclusive) {
        var cmp = Long.compare(value, min);
        if ((inclusive && cmp < 0) || (!inclusive && cmp <= 0)) {
            throw minValueException(inclusive, min, value);
        }
    }

    /**
     * 最小值校验（Comparable 泛型）。
     */
    public static <T extends Comparable<? super T>> void minValue(@Nullable T value, T min, boolean inclusive) {
        notNull(value);
        var cmp = value.compareTo(min);
        if ((inclusive && cmp < 0) || (!inclusive && cmp <= 0)) {
            throw minValueException(inclusive, min, value);
        }
    }

    /**
     * 正则格式校验。
     */
    public static void matches(@Nullable String value, Pattern pattern) {
        hasText(value);
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid format: '" + value + "'");
        }
    }

    /**
     * BigDecimal 小数位数上限校验。
     */
    public static void maxScale(@Nullable BigDecimal value, int max) {
        notNull(value);
        if (value.scale() > max) {
            throw new IllegalArgumentException("scale must not exceed " + max + ", got: " + value.scale());
        }
    }

    /**
     * 字符串最大长度校验。
     */
    public static void maxLength(@Nullable String value, int max) {
        hasText(value);
        if (value.length() > max) {
            throw new IllegalArgumentException("length must not exceed " + max + ", got: " + value.length());
        }
    }

    /**
     * 区间校验（包含两端）。
     */
    public static void range(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("must be between " + min + " and " + max + " (inclusive), got: " + value);
        }
    }

    /**
     * 区间校验（包含两端），泛型 Comparable 版本。
     */
    public static <T extends Comparable<? super T>> void range(@Nullable T value, T min, T max) {
        notNull(value);
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException("must be between " + min + " and " + max + " (inclusive), got: " + value);
        }
    }

    /**
     * 校验字符串以指定前缀开头，不匹配时抛出 {@link IllegalArgumentException}。
     */
    public static void hasPrefix(@Nullable String value, String prefix) {
        hasText(value);
        if (!value.startsWith(prefix)) {
            throw new IllegalArgumentException("must start with '" + prefix + "', got: '" + value + "'");
        }
    }

    /**
     * 校验 URI 为合法 http/https URL。
     */
    public static void validUrl(@Nullable URI value) {
        notNull(value);
        var scheme = value.getScheme();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("URI scheme must be http/https, got: '" + scheme + "'");
        }
        if (!value.isAbsolute()) {
            throw new IllegalArgumentException("URI must be absolute: '" + value + "'");
        }
    }

    private static IllegalArgumentException minValueException(boolean inclusive, Object min, Object value) {
        var msg = (inclusive ? "must be greater than or equal to " : "must be greater than ")
                + min + ", got: " + value;
        return new IllegalArgumentException(msg);
    }
}
