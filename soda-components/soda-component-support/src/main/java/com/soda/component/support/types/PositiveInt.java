package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.TypeConfig;
import com.soda.component.support.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 正整数 DP — 不可变、自校验、可比较、带缓存。
 * <p>
 * 校验规则：值 >= 1。通用类型，可用于长度、数量、序号等场景。
 * 缓存范围至少为 {@code [1, 100]}（参考 {@link Integer} 缓存和 {@link Version} 设计），
 * 可通过 SPI 接口 {@link com.soda.component.support.spi.TypeConfigProvider}
 * 的 {@code positiveIntCacheHigh()} 自定义上限。
 * 超出缓存范围的值创建新实例，不受缓存影响。
 *
 * @see Type
 * @see Version
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public final class PositiveInt implements Type, Comparable<PositiveInt> {

    /** 缓存上限，至少 100。 */
    private static final int CACHE_HIGH = Math.max(100, TypeConfig.PROVIDER.positiveIntCacheHigh());
    private static final PositiveInt[] CACHE = new PositiveInt[CACHE_HIGH];

    static {
        for (int i = 0; i < CACHE_HIGH; ) {
            CACHE[i] = new PositiveInt(++i);
        }
    }

    /** 最短正整数 1。 */
    public static final PositiveInt ONE = CACHE[0];

    private final int value;

    private PositiveInt(int value) {
        ValidateUtils.minValue(1, true, value);
        this.value = value;
    }

    @JsonValue
    public int value() {
        return this.value;
    }

    /** 从可靠输入构造。缓存范围内的值返回缓存实例。 */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PositiveInt of(int value) {
        if (1 <= value && value <= CACHE_HIGH) {
            return CACHE[value - 1];
        }
        return new PositiveInt(value);
    }

    /** 从字符串解析构造。格式同 {@link ParseUtils#parseInt}。 */
    public static PositiveInt parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

    @Override
    public int compareTo(PositiveInt other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        return "PositiveInt[value=" + value + "]";
    }
}
