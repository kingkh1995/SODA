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
 * 乐观锁版本号 DP — 不可变、自校验、可比较、带缓存。
 * <p>
 * 缓存范围至少 [0, 99]，通过 SPI 接口 {@link com.soda.component.support.spi.TypeConfigProvider}
 * 的 {@code versionCacheHigh()} 自定义上限（参考 {@link Integer} 缓存设计）。
 * 超出缓存范围的版本号创建新实例，不受缓存影响。
 * <p>
 * 参考 kk-ddd 的 {@code Version} 设计。
 *
 * @see Type
 * @see TypeConfig
 */
@EqualsAndHashCode
@Accessors(fluent = true)
public final class Version implements Type, Comparable<Version> {

    private static final int CACHE_HIGH = Math.max(99, TypeConfig.PROVIDER.versionCacheHigh());
    private static final ArrayTypeCache<Version> CACHE =
            new ArrayTypeCache<>(0, CACHE_HIGH, Version::new);

    public static final Version PRIMARY = CACHE.get(0);  // 0 始终在缓存范围

    private final int value;

    private Version(int value) {
        ValidateUtils.minValue(0, true, value);
        this.value = value;
    }

    /**
     * 从可靠输入构造。缓存范围内的值返回缓存实例。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Version of(int value) {
        var cached = CACHE.get(value);
        return cached != null ? cached : new Version(value);
    }

    /**
     * 从字符串解析构造。格式同 {@link ParseUtils#parseInt}。
     */
    public static Version parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

    @JsonValue
    public int value() {
        return this.value;
    }

    /**
     * 返回递增后的新版本号（不修改自身）。
     */
    public Version next() {
        return of(value + 1);
    }

    @Override
    public int compareTo(Version other) {
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        return "Version[value=" + value + "]";
    }
}
