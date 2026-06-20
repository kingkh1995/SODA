package com.soda.component.support.util;


/**
 * 参数异常工厂 — 生成通用格式的 {@link IllegalArgumentException}。
 * <p>
 * 统一错误消息模板，保证 type 类间一致性。
 */
public final class IllegalArgumentExceptions {

    private IllegalArgumentExceptions() {
        throw new UnsupportedOperationException();
    }

    public static IllegalArgumentException forIsNull() {
        return new IllegalArgumentException("must not be null");
    }

    public static IllegalArgumentException forIsBlank() {
        return new IllegalArgumentException("must not be blank");
    }

    public static IllegalArgumentException forWrongType(String expected, Class<?> actual) {
        return new IllegalArgumentException("expected " + expected + " but got: " + actual.getName());
    }

    public static IllegalArgumentException forWrongFormat(String value) {
        return new IllegalArgumentException("invalid number format: '" + value + "'");
    }

    public static IllegalArgumentException forInvalidFormat(String value) {
        return new IllegalArgumentException("invalid format: '" + value + "'");
    }

    public static IllegalArgumentException forMinValue(Object value, Object min, boolean inclusive) {
        var sb = new StringBuilder("must be greater than ");
        if (inclusive) {
            sb.append("or equal to ");
        }
        sb.append(min).append(", got: ").append(value);
        return new IllegalArgumentException(sb.toString());
    }

    public static IllegalArgumentException forMaxScale(int scale, int max) {
        return new IllegalArgumentException("scale must not exceed " + max + ", got: " + scale);
    }

    public static IllegalArgumentException forMaxLength(int actual, int max) {
        return new IllegalArgumentException("length must not exceed " + max + ", got: " + actual);
    }

    public static IllegalArgumentException forMaxValue(Object value, Object max, boolean inclusive) {
        var sb = new StringBuilder("must be less than ");
        if (inclusive) {
            sb.append("or equal to ");
        }
        sb.append(max).append(", got: ").append(value);
        return new IllegalArgumentException(sb.toString());
    }

    public static IllegalArgumentException forOutOfRange(int value, int min, int max) {
        return new IllegalArgumentException("must be between " + min + " and " + max + " (inclusive), got: " + value);
    }

    public static IllegalArgumentException forInvalidUriFormat(String value) {
        return new IllegalArgumentException("invalid URI format: '" + value + "'");
    }

    public static IllegalArgumentException forRelativeUri(String value) {
        return new IllegalArgumentException("URI must be absolute: '" + value + "'");
    }

    public static IllegalArgumentException forInvalidUriScheme(String value, String scheme) {
        return new IllegalArgumentException("URI scheme must be http/https, got: '" + scheme + "' for: '" + value + "'");
    }

    public static IllegalArgumentException forUnknownEnum(String value, String enumName) {
        return new IllegalArgumentException("unknown " + enumName + ": '" + value + "'");
    }

    public static IllegalArgumentException forMissingPrefix(String prefix, String value) {
        return new IllegalArgumentException("must start with '" + prefix + "', got: '" + value + "'");
    }
}
