package com.soda.component.domain.util;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;

/**
 * 解析工具类 — 将不可靠 {@link Object} 输入解析为指定基础类型。
 * <p>
 * 输入不可靠（可能 null / 类型不符 / 格式非法），失败统一抛 {@link IllegalArgumentException}。
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
     * 解析为 int：接受 {@code Number} 或 {@code String}（自动 trim），null、类型不符或格式非法抛 IAE。
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
     * 解析为 long：接受 {@code Number} 或 {@code String}（自动 trim），null、类型不符或格式非法抛 IAE。
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
     * 解析为 boolean：接受 {@code Boolean} 或 {@code String}（true/false/1/0，大小写不敏感）。
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
     * 解析为 {@link BigDecimal}：接受 {@code BigDecimal}、{@code Number}（经 toString）或 {@code String}（自动 trim）。
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
     * 解析为枚举成员：按 {@code name} 精确匹配（大小写敏感），null 或未知值抛 IAE。
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

    /**
     * String → byte[]（标准 Base64，RFC 4648 §4）。格式非法时抛出 {@link IllegalArgumentException}。
     */
    public static byte[] parseBase64(@Nullable String value) {
        ValidateUtils.hasText(value);
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw invalidFormat(value);
        }
    }

    /**
     * String → byte[]（URL-safe Base64，RFC 4648 §5）。格式非法时抛出 {@link IllegalArgumentException}。
     */
    public static byte[] parseBase64Url(@Nullable String value) {
        ValidateUtils.hasText(value);
        try {
            return Base64.getUrlDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw invalidFormat(value);
        }
    }
}
