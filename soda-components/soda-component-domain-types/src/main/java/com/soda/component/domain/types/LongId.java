package com.soda.component.domain.types;

import com.soda.component.domain.Identifier;
import com.soda.component.domain.LongLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;


/**
 * 通用长整型标识符 DP — 不可变、自校验、可比较。
 * <p>
 * 值域 value ≥ 1：0 与负数非法。
 * 紧凑构造器为主入口，提供 {@code parse(String)} 字符串解析。
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
