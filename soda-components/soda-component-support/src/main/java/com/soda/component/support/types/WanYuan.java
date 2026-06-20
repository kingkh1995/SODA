package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 人民币万元 DP — 不可变、自校验、可比较。
 * <p>
 * 内部以万元单位存储，精确到百元（最多两位小数）。
 * 通过 {@link #valueOf(Object)} 以万元值构造，
 * 通过 {@link #fromYuan(BigDecimal)} 从元转换。
 * <p>
 * 参考 kk-ddd 的 {@code MillionYuan} 设计。
 *
 * @see Type
 */
public record WanYuan(@JsonValue BigDecimal value) implements Type, Comparable<WanYuan> {

    @Serial
    private static final long serialVersionUID = 1L;

    public WanYuan {
        ValidateUtils.notNull(value);
        ValidateUtils.minValue(BigDecimal.ZERO, true, value);
        ValidateUtils.maxScale(2, value);
        value = value.stripTrailingZeros();
        if (value.scale() < 0) {
            value = value.setScale(0, java.math.RoundingMode.UNNECESSARY);
        }
    }

    /** 从不可靠输入构造，以万元为单位。null 或非法值时抛出 {@link IllegalArgumentException}。 */
    public static WanYuan valueOf(Object value) {
        return new WanYuan(ParseUtils.parseBigDecimal(value));
    }

    /** 从元（元/分）构造。舍入到百元精度。例如 {@code fromYuan(new BigDecimal("15000"))} → 1.5万元。 */
    public static WanYuan fromYuan(BigDecimal yuan) {
        ValidateUtils.notNull(yuan);
        ValidateUtils.minValue(BigDecimal.ZERO, true, yuan);
        var result = yuan.divide(BigDecimal.valueOf(10000), 2, java.math.RoundingMode.HALF_UP);
        return new WanYuan(result);
    }

    /** 转换为元（乘以 10000）。 */
    public BigDecimal toYuan() {
        return value.multiply(BigDecimal.valueOf(10000));
    }

    @Override
    public int compareTo(WanYuan other) {
        return this.value.compareTo(other.value);
    }
}
