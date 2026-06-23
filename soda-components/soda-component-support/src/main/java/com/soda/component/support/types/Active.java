package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 激活状态 DP — 通用 boolean 值封装。
 * <p>
 * 不可变，缓存 TRUE / FALSE 单例。
 * 提供 {@link #negate()} 用于取反。
 *
 * @see Type
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public final class Active implements Type, Comparable<Active> {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final Active TRUE = new Active(true);
    public static final Active FALSE = new Active(false);

    @Getter
    @JsonValue
    private final boolean value;

    private Active(boolean value) {
        this.value = value;
    }

    /** 从 boolean 构造（含 {@link JsonCreator} 入口）。 */
    @JsonCreator
    public static Active of(boolean value) {
        return value ? TRUE : FALSE;
    }


    /** 从字符串解析构造。格式同 {@link ParseUtils#parseBoolean}。 */
    public static Active parse(String s) {
        return of(ParseUtils.parseBoolean(s));
    }

    /** 取反。 */
    public Active negate() {
        return value ? FALSE : TRUE;
    }

    @Override
    public int compareTo(Active other) {
        return Boolean.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        return "Active[" + value + "]";
    }
}
