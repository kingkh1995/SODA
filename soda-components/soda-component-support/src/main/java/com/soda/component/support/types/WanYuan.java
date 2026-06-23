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
 * 紧凑构造器为主入口（以万元 {@link BigDecimal} 值），
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

    /** 从字符串解析构造。格式同 {@link ParseUtils#parseBigDecimal}。 */
    public static WanYuan parse(String s) {
        return new WanYuan(ParseUtils.parseBigDecimal(s));
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
