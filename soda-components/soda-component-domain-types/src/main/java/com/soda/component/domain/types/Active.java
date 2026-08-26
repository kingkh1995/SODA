package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.BooleanLiteralType;
import com.soda.component.domain.util.ParseUtils;
import lombok.EqualsAndHashCode;

/**
 * 激活状态 DP — 通用 boolean 值封装。
 * <p>
 * 不可变，缓存 TRUE / FALSE 单例。
 * 提供 {@link #negate()} 用于取反。
 *
 * @see BooleanLiteralType
 */
@EqualsAndHashCode
public final class Active implements BooleanLiteralType {

    /**
     * 真值单例（激活）。
     */
    public static final Active TRUE = new Active(true);

    /**
     * 假值单例（未激活）。
     */
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

    /**
     * 规范值访问器 — {@code @JsonValue} 继承自 {@link BooleanLiteralType}（ADR-0028）。
     */
    public boolean value() {
        return value;
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
