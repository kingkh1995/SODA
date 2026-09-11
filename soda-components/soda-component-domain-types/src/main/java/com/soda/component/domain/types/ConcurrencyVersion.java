package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.IntLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.TypeConfig;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;


/**
 * 乐观锁版本号 DP（并发令牌）— 不可变、自校验、可比较、带缓存。
 * <p>
 * 命名明示并发语义：与 {@code SoftwareVersion}（软件版本号）区分（见 ADR-0020）；
 * 对应 IDDD {@code ConcurrencySafeEntity.concurrencyVersion}。
 * <p>
 * 缓存范围至少 [0, 99]，通过 SPI 接口 {@link com.soda.component.domain.util.TypeConfigProvider}
 * 的 {@code versionCacheHigh()} 自定义上限（参考 {@link Integer} 缓存设计）。
 *
 * @see IntLiteralType
 * @see TypeConfig
 */
@EqualsAndHashCode
public final class ConcurrencyVersion implements IntLiteralType, Comparable<ConcurrencyVersion> {

    private static final int CACHE_HIGH = Math.max(99, TypeConfig.PROVIDER.versionCacheHigh());

    private static final ArrayTypeCache<ConcurrencyVersion> CACHE =
            new ArrayTypeCache<>(0, CACHE_HIGH, ConcurrencyVersion::new);

    /**
     * 初始版本号（0）。
     */
    public static final ConcurrencyVersion INITIAL = CACHE.get(0);  // 0 始终在缓存范围

    private final int value;

    private ConcurrencyVersion(int value) {
        ValidateUtils.minValue(value, 0, true);
        this.value = value;
    }

    /**
     * 从可靠输入构造。缓存范围内的值返回缓存实例。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ConcurrencyVersion of(Integer value) {
        ValidateUtils.notNull(value);
        var cached = CACHE.get(value);
        return cached != null ? cached : new ConcurrencyVersion(value);
    }

    /**
     * 从字符串解析构造。格式同 {@link ParseUtils#parseInt}。
     */
    public static ConcurrencyVersion parse(String s) {
        return of(ParseUtils.parseInt(s));
    }

    /**
     * 规范值访问器 — {@code @JsonValue} 继承自 {@link IntLiteralType}（ADR-0028）。
     */
    public int value() {
        return value;
    }

    /**
     * 返回递增后的新版本号（不修改自身）。
     */
    public ConcurrencyVersion next() {
        return of(value + 1);
    }

    @Override
    public int compareTo(ConcurrencyVersion other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return "ConcurrencyVersion[value=" + value + "]";
    }
}
