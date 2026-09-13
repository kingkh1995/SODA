package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.IntLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;


/**
 * 正整数 DP — 不可变、自校验、可比较。
 * <p>
 * 校验规则：值 >= 1。通用类型，可用于长度、数量、序号等场景。
 * <p>
 * 无实例缓存：生产调用点为策略常量级（每次构造一个实例），缓存不产生可测收益（见 dp-conventions §7）。
 *
 * @see IntLiteralType
 */
@EqualsAndHashCode
public final class PositiveInt implements IntLiteralType, Comparable<PositiveInt> {

    /**
     * 单位值常量（1）。
     */
    public static final PositiveInt ONE = new PositiveInt(1);

    private final int value;

    private PositiveInt(int value) {
        ValidateUtils.minValue(value, 1, true);
        this.value = value;
    }

    /**
     * 从可靠输入构造。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PositiveInt of(int value) {
        return new PositiveInt(value);
    }

    /**
     * 从字符串解析构造。格式同 {@link ParseUtils#parseInt}。
     */
    public static PositiveInt parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

    /**
     * 规范值访问器 — {@code @JsonValue} 继承自 {@link IntLiteralType}（ADR-0028）。
     */
    public int value() {
        return value;
    }

    @Override
    public int compareTo(PositiveInt other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return "PositiveInt[value=" + value + "]";
    }
}
