package com.soda.component.support.util;

import static com.soda.component.support.util.IllegalArgumentExceptions.forWrongFormat;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

import static com.soda.component.support.util.IllegalArgumentExceptions.forWrongType;

/**
 * 解析工具类 — 将不可靠 {@link Object} 输入解析为指定基础类型。
 * <p>
 * 参考 kk-ddd 的 {@code ParseUtils} 设计。
 */
public final class ParseUtils {

    private static final String EXPECTED_NUM = "Number or String";

    private ParseUtils() {
        throw new UnsupportedOperationException();
    }

    public static int parseInt(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return switch (o) {
            case Number n -> n.intValue();
            case String s -> {
                try {
                    yield Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    throw forWrongFormat(s);
                }
            }
            default -> throw forWrongType(EXPECTED_NUM, o.getClass());
        };
    }

    public static long parseLong(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return switch (o) {
            case Number n -> n.longValue();
            case String s -> {
                try {
                    yield Long.parseLong(s.trim());
                } catch (NumberFormatException e) {
                    throw forWrongFormat(s);
                }
            }
            default -> throw forWrongType(EXPECTED_NUM, o.getClass());
        };
    }

    public static String parseString(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return o.toString();
    }

    public static BigDecimal parseBigDecimal(@Nullable Object o) {
        ValidateUtils.notNull(o);
        return switch (o) {
            case BigDecimal bd -> bd;
            case Number n -> {
                try {
                    yield new BigDecimal(n.toString());
                } catch (NumberFormatException e) {
                    throw forWrongFormat(n.toString());
                }
            }
            case String s -> {
                try {
                    yield new BigDecimal(s.trim());
                } catch (NumberFormatException e) {
                    throw forWrongFormat(s);
                }
            }
            default -> throw forWrongType(EXPECTED_NUM, o.getClass());
        };
    }
}
