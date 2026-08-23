package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.TypeConfig;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 百分比 DP — 不可变、自校验，extends {@link DecimalLiteralType}（缓存不变量见基类，ADR-0031）。
 * 字面值语义，例如 {@code 12.34} 表示 12.34%。
 * <p>
 * 规范值为 {@link BigDecimal#toPlainString()} 的 String（{@code @JsonValue}，继承自
 * {@link StringLiteralType}，见 ADR-0028），{@link BigDecimal} 派生缓存。
 * <p>
 * 取值范围 {@code [0, 100]}（见 {@link #validate(BigDecimal)}），最多 2 位小数。
 * 需要舍入时使用 {@link #from(BigDecimal, RoundingMode)}。
 *
 * @see Type
 * @see DecimalLiteralType
 */
@EqualsAndHashCode(callSuper = true)
public final class Percentage extends DecimalLiteralType implements Comparable<Percentage> {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = Math.clamp(TypeConfig.PROVIDER.percentageScale(), 0, 4);

    private Percentage(BigDecimal raw) {
        super(raw, SCALE);
    }

    /**
     * JSON 反序列化入口。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Percentage of(String value) {
        var bd = ParseUtils.parseBigDecimal(value);
        return new Percentage(bd);
    }

    /**
     * 从 {@link BigDecimal} 构造。
     */
    public static Percentage from(BigDecimal value) {
        ValidateUtils.notNull(value);
        return new Percentage(value);
    }

    /**
     * 从 {@link BigDecimal} 构造，指定舍入模式。
     */
    public static Percentage from(BigDecimal value, RoundingMode roundingMode) {
        ValidateUtils.notNull(value);
        ValidateUtils.notNull(roundingMode);
        var rounded = value.setScale(SCALE, roundingMode);
        return new Percentage(rounded);
    }

    /**
     * 不变量：{@code [0, 100]} range 校验（在 {@code setScale} 后、赋值前）。
     */
    @Override
    protected void validate(BigDecimal normalized) {
        ValidateUtils.range(normalized, BigDecimal.ZERO, HUNDRED);
    }

    /**
     * 转换为小数。例如 12.34% → {@code 0.1234}。
     */
    public BigDecimal toFraction() {
        return decimalValue().divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    /**
     * 展示文本。格式：{@code 12.34%}。
     */
    public String toDisplayString() {
        return decimalValue().toPlainString() + "%";
    }

    @Override
    public int compareTo(Percentage other) {
        return decimalValue().compareTo(other.decimalValue());
    }

    @Override
    public String toString() {
        return "Percentage[value=" + value() + "]";
    }
}
