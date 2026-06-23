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
 * 紧凑构造器为主入口（{@code new LongId(long)}），提供 {@code parse(String)} 字符串解析。
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

    /** 从字符串解析构造。格式同 {@link ParseUtils#parseLong}。 */
    public static LongId parse(String s) {
        return new LongId(ParseUtils.parseLong(s));
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
