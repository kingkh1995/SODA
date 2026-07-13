package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ParseUtils;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

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
public final class Active implements Type {

    public static final Active TRUE = new Active(true);
    public static final Active FALSE = new Active(false);

    private final boolean value;

    private Active(boolean value) {
        this.value = value;
    }

    /**
     * 从 boolean 构造（含 {@link JsonCreator} 入口）。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Active of(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * 从字符串解析构造。格式同 {@link ParseUtils#parseBoolean}。
     */
    public static Active parse(String s) {
        return of(ParseUtils.parseBoolean(s));
    }

    @JsonValue
    public boolean value() {
        return this.value;
    }

    /**
     * 取反。
     */
    public Active negate() {
        return value ? FALSE : TRUE;
    }


    @Override
    public String toString() {
        return "Active[value=" + value + "]";
    }
}
