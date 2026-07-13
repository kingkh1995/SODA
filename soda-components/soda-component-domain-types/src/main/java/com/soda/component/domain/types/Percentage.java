package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.TypeConfig;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 百分比 DP — 不可变、自校验。字面值语义，例如 {@code 12.34} 表示 12.34%。
 * <p>
 * 规范值是对 {@link BigDecimal#toPlainString()} 的字符串，作为 {@link JsonValue JSON 序列化} 和
 * {@link Object#equals(Object) equals}/{@link Object#hashCode() hashCode} 的依据。
 * {@link BigDecimal} 作为派生缓存值，不参与序列化和相等性判断。
 * <p>
 * 取值范围 {@code [0, 100]}，最多 2 位小数。需要舍入时使用 {@link #from(BigDecimal, RoundingMode)}。
 *
 * @see Type
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class Percentage implements Type, Comparable<Percentage> {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = Math.clamp(TypeConfig.PROVIDER.percentageScale(), 0, 4);

    @EqualsAndHashCode.Include
    private final String value;
    @Getter
    private final BigDecimal bigDecimalValue;

    private Percentage(BigDecimal raw) {
        ValidateUtils.notNull(raw);
        ValidateUtils.maxScale(raw, SCALE);
        var normalized = raw.setScale(SCALE, RoundingMode.UNNECESSARY);
        ValidateUtils.range(normalized, BigDecimal.ZERO, HUNDRED);
        this.value = normalized.toPlainString();
        this.bigDecimalValue = normalized;
    }

    /**
     * JSON 反序列化入口。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Percentage of(String jsonValue) {
        var bd = ParseUtils.parseBigDecimal(jsonValue);
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

    @JsonValue
    public String value() {
        return this.value;
    }

    /**
     * 转换为小数。例如 12.34% → {@code 0.1234}。
     */
    public BigDecimal toFraction() {
        return bigDecimalValue.divide(HUNDRED, 4, RoundingMode.HALF_UP);
    }

    /**
     * 展示文本。格式：{@code 12.34%}。
     */
    public String toDisplayString() {
        return bigDecimalValue.toPlainString() + "%";
    }

    @Override
    public int compareTo(Percentage other) {
        return this.bigDecimalValue.compareTo(other.bigDecimalValue);
    }

    @Override
    public String toString() {
        return "Percentage[value=" + value + "]";
    }
}
