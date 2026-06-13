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

    public static IllegalArgumentException forMinValue(long value, long min, boolean inclusive) {
        var sb = new StringBuilder("must be greater than ");
        if (inclusive) sb.append("or equal to ");
        sb.append(min).append(", got: ").append(value);
        return new IllegalArgumentException(sb.toString());
    }
}
