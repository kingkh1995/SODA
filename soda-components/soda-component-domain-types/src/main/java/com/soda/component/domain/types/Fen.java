package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 分 DP — 通用金额值对象，以分记，int 存储。不可变、自校验、可比较。
 * <p>
 * 1 元 = 100 分，例如 {@code new Fen(1500)} 表示 15.00 元。
 * 值域覆盖整个 int 范围，即 [-21,474,836.47, 21,474,836.47] 元；
 * 负值合法，用于退款、冲正等负向金额场景。超出该范围的金额请使用 {@link WanYuan}。
 * <p>
 * 校验规则：int 本身保证不溢出，整个值域均合法，无需额外约束。
 * {@link #ZERO} 是便捷常量，非缓存契约——不承诺单例身份（缓存不可观测，禁止依赖 {@code ==}）。
 *
 * @see Type
 * @see WanYuan
 */
public record Fen(int value) implements Type, Comparable<Fen> {

    /**
     * 0 分常量 — 领域意义上的便捷引用。
     */
    public static final Fen ZERO = new Fen(0);

    private static final BigDecimal MIN_FEN = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal MAX_FEN = BigDecimal.valueOf(Integer.MAX_VALUE);

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Fen {
    }

    /**
     * 从字符串解析构造。格式同 {@link ParseUtils#parseInt}。
     */
    public static Fen parse(String s) {
        return new Fen(ParseUtils.parseInt(s));
    }

    /**
     * 从元构造，默认 {@link RoundingMode#HALF_UP HALF_UP} 舍入。例如 {@code fromYuan(new BigDecimal("15.5"))} → 1550 分。
     */
    public static Fen fromYuan(BigDecimal yuan) {
        return fromYuan(yuan, RoundingMode.HALF_UP);
    }

    /**
     * 从元构造，指定舍入模式。例如 {@code fromYuan(new BigDecimal("1.005"), RoundingMode.HALF_UP)} → 101 分。
     *
     * @throws IllegalArgumentException 元超出 int 分范围（约 ±2147 万元）时抛出
     */
    public static Fen fromYuan(BigDecimal yuan, RoundingMode roundingMode) {
        ValidateUtils.notNull(yuan);
        ValidateUtils.notNull(roundingMode);
        var fen = yuan.movePointRight(2).setScale(0, roundingMode);
        ValidateUtils.range(fen, MIN_FEN, MAX_FEN);
        return new Fen(fen.intValueExact());
    }

    /**
     * Jackson 3 序列化出口 — 必须为 public 方法（record component 上无效）。
     */
    @JsonValue
    @Override
    public int value() {
        return value;
    }

    /**
     * 转换为元（分 → 元，精确无舍入）。例如 1500 分 → {@code 15.00}。
     */
    public BigDecimal toYuan() {
        return BigDecimal.valueOf(value, 2);
    }

    /**
     * 展示文本。格式：{@code 15.00元}。
     */
    public String toDisplayString() {
        return toYuan().toPlainString() + "元";
    }

    @Override
    public int compareTo(Fen other) {
        return Integer.compare(value, other.value);
    }
}
