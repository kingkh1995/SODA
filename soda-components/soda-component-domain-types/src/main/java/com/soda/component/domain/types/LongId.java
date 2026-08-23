package com.soda.component.domain.types;

import com.soda.component.domain.Identifier;
import com.soda.component.domain.LongLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;


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
public record LongId(long value) implements Identifier<Long>, LongLiteralType, Comparable<LongId> {

    public LongId {
        ValidateUtils.minValue(value, 0, false);
    }

    public static LongId parse(String s) {
        return new LongId(ParseUtils.parseLong(s));
    }

    @Override
    public Long identifier() {
        return value;
    }

    @Override
    public int compareTo(LongId other) {
        return Long.compare(value, other.value);
    }
}
