package com.soda.component.support.types;

import com.soda.component.support.util.ValidateUtils;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * DP 实例缓存 — 数组 O(1) 索引查找，无 auto-boxing。
 * <p>
 * 用于 int 范围映射的 DP（如 {@link Version}、{@link PositiveInt}），
 * 在类初始化时预分配数组，工厂方法优先返回缓存命中。
 *
 * @param <V> DP 类型
 */
public final class ArrayTypeCache<V> {

    private final V[] cache;
    private final int offset;

    /**
     * 构造缓存，预创建 {@code [low, high]} 范围内的实例。
     *
     * @param low     范围下界（含）
     * @param high    范围上界（含）
     * @param factory 实例工厂
     * @throws IllegalArgumentException if {@code low > high}
     */
    @SuppressWarnings("unchecked")
    public ArrayTypeCache(int low, int high, IntFunction<V> factory) {
        ValidateUtils.notNull(factory);
        ValidateUtils.minValue(high, low, true);
        var size = high - low + 1;
        this.offset = low;
        this.cache = (V[]) new Object[size];
        for (int i = 0; i < size; i++) {
            cache[i] = factory.apply(low + i);
        }
    }

    /**
     * 按值查找缓存实例。
     *
     * @param value 要查找的值
     * @return 命中返回缓存实例，未命中返回 {@code null}
     */
    public @Nullable V get(int value) {
        var idx = value - offset;
        return 0 <= idx && idx < cache.length ? cache[idx] : null;
    }

    /**
     * 缓存范围下界（含）。
     */
    public int low() {
        return offset;
    }

    /**
     * 缓存范围上界（含）。
     */
    public int high() {
        return offset + cache.length - 1;
    }
}
