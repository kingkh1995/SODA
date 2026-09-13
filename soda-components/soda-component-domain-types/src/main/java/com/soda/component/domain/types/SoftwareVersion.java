package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

/**
 * 软件版本号 DP — 不可变、自校验、可比较。
 * <p>
 * 三段式纯数字（major.minor.patch），每段 {@code [0, 999]}，规范值带小写 {@code v} 前缀
 * （如 {@code "v2.1.3"}）。前导 0 归一化：{@code v2.001.003} 与 {@code v2.1.3} 等价。
 * base-1000 打包 int：{@code v2.1.3} ↔ {@code 2001003}，打包序与版本序单调一致。
 * 非 SemVer：无 pre-release / build 元数据后缀（见 ADR-0020）。
 * <p>
 * 两条入口形态各自直达唯一构造器：数值形态（{@link #from} / {@link #fromPackedInt}）零转换；
 * 线形态（{@link #of}）先守 {@link #FORMAT} 格式契约再拆段——无字符串回炉。
 *
 * @see StringLiteralType
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class SoftwareVersion implements StringLiteralType, Comparable<SoftwareVersion> {

    /**
     * 单段上限。
     */
    private static final int MAX_SEGMENT = 999;

    /**
     * 打包基数（每段 3 位十进制）。
     */
    private static final int BASE = 1_000;

    /**
     * 打包 int 上限（由单段上限派生）。
     */
    private static final int MAX_PACKED =
            MAX_SEGMENT * BASE * BASE + MAX_SEGMENT * BASE + MAX_SEGMENT;

    /**
     * 格式：v（大小写不敏感）+ 三段各 1-3 位数字。段上限 999 由位数天然约束。
     */
    private static final Pattern FORMAT = Pattern.compile("[vV]\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    /**
     * 规范值：小写 v + 去前导 0 的三段，如 "v2.1.3"。相等性唯一依据。
     */
    @EqualsAndHashCode.Include
    private final String value;

    /**
     * 派生字段，不参与相等性。
     */
    @Getter
    private final int major;
    @Getter
    private final int minor;
    @Getter
    private final int patch;

    /**
     * 唯一构造器 = 唯一校验点 —— 段范围校验 ＋ 规范串渲染；非法输入不可表示。
     * <p>
     * 规范串由三段渲染而来（前导 0 归一化即渲染），故 {@link #value} 与三段不可能失配；
     * 线形态的格式守在其转换入口（见 {@link #of}）。
     */
    private SoftwareVersion(int major, int minor, int patch) {
        ValidateUtils.range(major, 0, MAX_SEGMENT);
        ValidateUtils.range(minor, 0, MAX_SEGMENT);
        ValidateUtils.range(patch, 0, MAX_SEGMENT);
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.value = "v" + major + "." + minor + "." + patch;
    }

    /**
     * 从字符串解析。格式 {@code "v2.1.3"}（前缀 v 大小写不敏感），每段 {@code [0, 999]}，
     * 前导 0 自动归一化。非法格式或越界时抛 IAE。
     * <p>
     * {@link #FORMAT} 是线形态的输入契约：三段各 1-3 位数字 → 值天然 ≤ 999，故此处只守格式，
     * 转换后直达唯一构造器。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SoftwareVersion of(String value) {
        ValidateUtils.matches(value, FORMAT);
        var parts = value.substring(1).split("\\.");
        return new SoftwareVersion(
                ParseUtils.parseInt(parts[0]),
                ParseUtils.parseInt(parts[1]),
                ParseUtils.parseInt(parts[2]));
    }

    /**
     * 从三段数值转换构造，每段 {@code [0, 999]}，越界抛 IAE。
     */
    public static SoftwareVersion from(int major, int minor, int patch) {
        return new SoftwareVersion(major, minor, patch);
    }

    /**
     * 从 base-1000 打包 int 还原。{@code v2.1.3} ↔ {@code 2001003}，越界或负数抛 IAE。
     * <p>
     * 入参域的 {@code range} 是转换前置守卫：打包值域与段值域互为双射，此处的守卫指名输入域
     * （负数或超过 {@link #MAX_PACKED}），拆位后仍由构造器承担段范围校验。
     */
    public static SoftwareVersion fromPackedInt(int packed) {
        ValidateUtils.range(packed, 0, MAX_PACKED);
        return new SoftwareVersion(packed / (BASE * BASE), packed / BASE % BASE, packed % BASE);
    }

    /**
     * base-1000 打包 int。{@code v2.1.3} → {@code 2001003}；打包序与版本序单调一致。
     */
    public int toPackedInt() {
        return major * BASE * BASE + minor * BASE + patch;
    }

    /**
     * 规范值访问器 — {@code @JsonValue} 继承自 {@link StringLiteralType}（ADR-0028）。
     */
    public String value() {
        return value;
    }

    /**
     * 递增 patch 段（其余不变），返回新实例。patch 已到 999 时抛 IAE，不进位。
     */
    public SoftwareVersion nextPatch() {
        return new SoftwareVersion(major, minor, patch + 1);
    }

    /**
     * 递增 minor 段并清零 patch，返回新实例。minor 已到 999 时抛 IAE，不进位。
     */
    public SoftwareVersion nextMinor() {
        return new SoftwareVersion(major, minor + 1, 0);
    }

    /**
     * 递增 major 段并清零 minor/patch，返回新实例。major 已到 999 时抛 IAE，不进位。
     */
    public SoftwareVersion nextMajor() {
        return new SoftwareVersion(major + 1, 0, 0);
    }

    @Override
    public int compareTo(SoftwareVersion other) {
        var cmp = Integer.compare(major, other.major);
        if (cmp != 0) {
            return cmp;
        }
        cmp = Integer.compare(minor, other.minor);
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return "SoftwareVersion[value=" + value + "]";
    }
}
