package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.TypeConfig;
import com.soda.component.support.util.ValidateUtils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;

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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class Version implements Type, Comparable<Version> {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 缓存内部类 — 通过 SPI 确定上限。至少 99。 */
    private static class Cache {
        static final int HIGH = Math.max(99, TypeConfig.PROVIDER.versionCacheHigh());
        static final Version[] INSTANCES = new Version[HIGH + 1];

        static {
            for (int i = 0; i < INSTANCES.length; i++) {
                INSTANCES[i] = new Version(i);
            }
        }
    }

    /** 初始版本号（0）。 */
    public static final Version PRIMARY = Cache.INSTANCES[0];

    @Getter
    @JsonValue
    @EqualsAndHashCode.Include
    private final int value;

    private Version(int value) {
        ValidateUtils.minValue(0, true, value);
        this.value = value;
    }

    /** 从可靠输入构造。缓存范围内的值返回缓存实例。 */
    @JsonCreator
    public static Version of(int value) {
        if (0 <= value && value < Cache.INSTANCES.length) {
            return Cache.INSTANCES[value];
        }
        return new Version(value);
    }

    /** 从不可靠输入构造，null 或非法值时抛出 {@link IllegalArgumentException}。 */
    public static Version valueOf(Object value) {
        return of(ParseUtils.parseInt(value));
    }

    /** 返回递增后的新版本号（不修改自身）。 */
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
