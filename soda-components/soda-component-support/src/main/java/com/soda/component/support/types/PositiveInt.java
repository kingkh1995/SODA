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
 * 缓存范围至少为 {@code [1, 100]}（参考 {@link Integer} 缓存和 {@link ArrayTypeCache} 设计），
 * 可通过 SPI 接口 {@link com.soda.component.support.spi.TypeConfigProvider}
 * 的 {@code positiveIntCacheHigh()} 自定义上限。
 * 超出缓存范围的值创建新实例，不受缓存影响。
 *
 * @see Type
 * @see ArrayTypeCache
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public final class PositiveInt implements Type, Comparable<PositiveInt> {

    private static final int CACHE_HIGH = Math.max(100, TypeConfig.PROVIDER.positiveIntCacheHigh());
    private static final ArrayTypeCache<PositiveInt> CACHE =
            new ArrayTypeCache<>(1, CACHE_HIGH, PositiveInt::new);

    public static final PositiveInt ONE = CACHE.get(1);  // 1 始终在缓存范围

    private final int value;

    private PositiveInt(int value) {
        ValidateUtils.minValue(value, 1, true);
        this.value = value;
    }

    /**
     * 从可靠输入构造。缓存范围内的值返回缓存实例。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PositiveInt of(int value) {
        var cached = CACHE.get(value);
        return cached != null ? cached : new PositiveInt(value);
    }

    /**
     * 从字符串解析构造。格式同 {@link ParseUtils#parseInt}。
     */
    public static PositiveInt parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

    @JsonValue
    public int value() {
        return this.value;
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
