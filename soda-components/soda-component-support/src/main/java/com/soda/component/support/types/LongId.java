package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Identifier;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * {@code Long} 类型标识符 — 通用 DP，项目中最基础的长整型 ID 类型。
 * <p>
 * 遵循 DP 规范：不可变、自校验、可比较。
 * 作为标识符 DP 的样板代码，所有 ID 类型按此模式实现：
 * <pre>{@code
 * public record XxxId(@JsonValue T value) implements Identifier<T> {
 *     public XxxId { … }                                       // 紧凑校验
 *     public static XxxId valueOf(Object value) { … }          // 工厂方法
 * }
 * }</pre>
 * <p>
 * 参考 kk-ddd 的 {@code LongId} 设计。
 *
 * @see Identifier
 */
public record LongId(@JsonValue long value) implements Identifier<Long>, Comparable<LongId> {

    @Serial
    private static final long serialVersionUID = 1L;

    public LongId {
        ValidateUtils.minValue(0, false, value);
    }

    /** 从不可靠输入构造，null 或非法值时抛出 {@link IllegalArgumentException}。 */
    public static LongId valueOf(Object value) {
        return new LongId(ParseUtils.parseLong(value));
    }

    @Override
    public Long identifier() {
        return value;
    }

    @Override
    public int compareTo(LongId other) {
        return Long.compare(this.value, other.value);
    }
}
