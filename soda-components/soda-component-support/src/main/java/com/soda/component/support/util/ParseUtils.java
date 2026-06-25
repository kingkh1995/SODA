package com.soda.component.support.util;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

import static com.soda.component.support.util.IllegalArgumentExceptions.forInvalidFormat;
import static com.soda.component.support.util.IllegalArgumentExceptions.forWrongType;

/**
 * 解析工具类 — 将不可靠 {@link Object} 输入解析为指定基础类型。
 * <p>
 * 参考 kk-ddd 的 {@code ParseUtils} 设计。
 */
public final class ParseUtils {

    private static final String EXPECTED_NUM = "Number or String";
    private static final String EXPECTED_BOOL = "Boolean or String";

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
                    throw forInvalidFormat(s);
                }
            }
            default -> throw forWrongType(EXPECTED_NUM, o.getClass());
        };
    }

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
                throw forInvalidFormat(trimmed);
            }
            default -> throw forWrongType(EXPECTED_BOOL, o.getClass());
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
                    throw forInvalidFormat(s);
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
                    throw forInvalidFormat(n.toString());
                }
            }
            case String s -> {
                try {
                    yield new BigDecimal(s.trim());
                } catch (NumberFormatException e) {
                    throw forInvalidFormat(s);
                }
            }
            default -> throw forWrongType(EXPECTED_NUM, o.getClass());
        };
    }

    public static <T extends Enum<T>> T parseEnum(Class<T> enumClass, @Nullable Object value) {
        var name = parseString(value);  // null → IAE（notNull）
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            throw IllegalArgumentExceptions.forUnknownEnum(name, enumClass.getSimpleName());
        }
    }

}
