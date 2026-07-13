package com.soda.component.domain.util;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 解析工具类 — 将不可靠 {@link Object} 输入解析为指定基础类型。
 * <p>
 * 可调用 {@link ValidateUtils} 对输入做校验。
 */
public final class ParseUtils {

    private ParseUtils() {
        throw new UnsupportedOperationException();
    }

    private static IllegalArgumentException typeError(String expected, Object o) {
        return new IllegalArgumentException("expected " + expected + " but got: " + o.getClass().getName());
    }

    private static IllegalArgumentException invalidFormat(Object value) {
        return new IllegalArgumentException("invalid format: '" + value + "'");
    }

    /**
     * Object → int
     */
    public static int parseInt(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return switch (o) {
            case Number n -> n.intValue();
            case String s -> {
                try {
                    yield Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    throw invalidFormat(s);
                }
            }
            default -> throw typeError("Number or String", o);
        };
    }

    /**
     * Object → long
     */
    public static long parseLong(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return switch (o) {
            case Number n -> n.longValue();
            case String s -> {
                try {
                    yield Long.parseLong(s.trim());
                } catch (NumberFormatException e) {
                    throw invalidFormat(s);
                }
            }
            default -> throw typeError("Number or String", o);
        };
    }

    /**
     * Object → boolean
     */
    public static boolean parseBoolean(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return switch (o) {
            case Boolean b -> b;
            case String s -> {
                var trimmed = s.trim();
                if ("true".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) {
                    yield true;
                } else if ("false".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
                    yield false;
                }
                throw invalidFormat(trimmed);
            }
            default -> throw typeError("Boolean or String", o);
        };
    }

    /**
     * Object → BigDecimal
     */
    public static BigDecimal parseBigDecimal(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return switch (o) {
            case BigDecimal bd -> bd;
            case Number n -> {
                try {
                    yield new BigDecimal(n.toString());
                } catch (NumberFormatException e) {
                    throw invalidFormat(n);
                }
            }
            case String s -> {
                try {
                    yield new BigDecimal(s.trim());
                } catch (NumberFormatException e) {
                    throw invalidFormat(s);
                }
            }
            default -> throw typeError("Number or String", o);
        };
    }

    /**
     * String → Enum
     */
    public static <T extends Enum<T>> T parseEnum(Class<T> enumClass, @Nullable String value) {
        ValidateUtils.hasText(value);
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown " + enumClass.getSimpleName() + ": '" + value + "'");
        }
    }

    /**
     * String → {@link URI}。仅校验非空和格式，不校验 scheme/absolute 等业务语义（由 {@link ValidateUtils#validUrl} 负责）。
     */
    public static URI parseUri(@Nullable String value) {
        ValidateUtils.hasText(value);
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw invalidFormat(value);
        }
    }

    /**
     * 查找分隔符 {@code delimiter} 在 {@code value} 中的索引。未找到时抛出 {@link IllegalArgumentException}（vs {@link String#indexOf} 返回 -1）。
     */
    public static int indexOf(@Nullable String value, String delimiter) {
        ValidateUtils.hasText(value);
        var idx = value.indexOf(delimiter);
        if (idx < 0) {
            throw new IllegalArgumentException("delimiter '" + delimiter + "' not found in: '" + value + "'");
        }
        return idx;
    }

    /**
     * 按 {@code delimiter} 拆分 {@code value} 为两个子串（仅拆分第一次出现）。分隔符必须存在。
     *
     * @return {@code [first, second]}
     */
    public static String[] splitPair(@Nullable String value, String delimiter) {
        var idx = indexOf(value, delimiter);
        return new String[]{value.substring(0, idx), value.substring(idx + delimiter.length())};
    }

    /**
     * 校验字符串以 {@code prefix} 开头并返回去掉前缀后的剩余部分。
     */
    public static String cutPrefix(@Nullable String value, String prefix) {
        ValidateUtils.hasPrefix(value, prefix);
        return value.substring(prefix.length());
    }
}
