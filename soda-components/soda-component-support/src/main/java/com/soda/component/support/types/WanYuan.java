package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 人民币万元 DP — 不可变、自校验。
 * <p>
 * 规范值是对 {@link BigDecimal#toPlainString()} 的字符串，作为 {@link JsonValue JSON 序列化} 和
 * {@link Object#equals(Object) equals}/{@link Object#hashCode() hashCode} 的依据。
 * {@link BigDecimal} 作为派生缓存值，不参与序列化和相等性判断。
 * <p>
 * 通过 {@link #fromYuan(BigDecimal)} 从元转换，精度到百元（最多 2 位小数），可为负。
 *
 * @see Type
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class WanYuan implements Type {

    private static final BigDecimal WAN = BigDecimal.valueOf(10000);

    @EqualsAndHashCode.Include
    private final String value;
    @JsonValue
    public String value() {
        return this.value;
    }

    @Getter
    private final BigDecimal bigDecimalValue;

    private WanYuan(String value, BigDecimal bigDecimalValue) {
        this.value = value;
        this.bigDecimalValue = bigDecimalValue;
    }

    /**
     * JSON 反序列化入口 — 通过 {@link ParseUtils#parseBigDecimal} 解析，确保始终输出
     * {@link BigDecimal#toPlainString()} 格式的字符串规范值。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WanYuan of(String jsonValue) {
        var bd = ParseUtils.parseBigDecimal(jsonValue);
        ValidateUtils.maxScale(2, bd);
        bd = bd.setScale(2, java.math.RoundingMode.UNNECESSARY);
        return new WanYuan(bd.toPlainString(), bd);
    }

    /** 从字符串解析构造。格式同 {@link ParseUtils#parseBigDecimal}。 */
    public static WanYuan parse(String s) {
        return of(s);
    }

    /** 从元（元/分）构造，默认 {@link java.math.RoundingMode#HALF_UP HALF_UP} 舍入。例如 {@code fromYuan(new BigDecimal("15000"))} → 1.5万元。 */
    public static WanYuan fromYuan(BigDecimal yuan) {
        return fromYuan(yuan, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 从元（元/分）构造，指定舍入模式。
     *
     * @param yuan         元值
     * @param roundingMode 舍入模式（除以 10000 时使用）
     * @return WanYuan 实例
     */
    public static WanYuan fromYuan(BigDecimal yuan, java.math.RoundingMode roundingMode) {
        ValidateUtils.notNull(yuan);
        ValidateUtils.notNull(roundingMode);
        var result = yuan.divide(WAN, 2, roundingMode);
        return of(result.toPlainString());
    }

    /** 转换为元（乘以 10000），结果不保留小数。 */
    public BigDecimal toYuan() {
        return bigDecimalValue.multiply(WAN).setScale(0, java.math.RoundingMode.UNNECESSARY);
    }

    /** 中文展示文本。格式：{@code 111.11万元}。 */
    public String toPlainText() {
        return bigDecimalValue.toPlainString() + "万元";
    }

    @Override
    public String toString() {
        return "WanYuan[value=" + value + "]";
    }
}
