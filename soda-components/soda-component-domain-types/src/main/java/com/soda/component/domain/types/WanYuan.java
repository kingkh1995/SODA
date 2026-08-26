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
 * 人民币万元 DP — 不可变、自校验，extends {@link DecimalLiteralType}（缓存不变量见基类，ADR-0031）。
 * <p>
 * 规范值为 {@link BigDecimal#toPlainString()} 的 String（{@code @JsonValue}，继承自
 * {@link StringLiteralType}，见 ADR-0028），{@link BigDecimal} 派生缓存。
 * <p>
 * 通过 {@link #fromYuan(BigDecimal)} 从元转换，精度到百元（最多 2 位小数，SPI 可调），可为负。
 * 超出 {@link Fen} 值域（约 ±2147 万元）的金额用本 DP。
 *
 * @see StringLiteralType
 * @see DecimalLiteralType
 */
@EqualsAndHashCode(callSuper = true)
public final class WanYuan extends DecimalLiteralType implements Comparable<WanYuan> {

    private static final BigDecimal WAN = BigDecimal.valueOf(10000);
    private static final int SCALE = Math.clamp(TypeConfig.PROVIDER.wanYuanScale(), 0, 4);

    private WanYuan(BigDecimal raw) {
        super(raw, SCALE);
    }

    /**
     * JSON 反序列化入口。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WanYuan of(String value) {
        var bd = ParseUtils.parseBigDecimal(value);
        return new WanYuan(bd);
    }

    /**
     * 从 {@link BigDecimal} 构造。
     */
    public static WanYuan from(BigDecimal value) {
        ValidateUtils.notNull(value);
        return new WanYuan(value);
    }

    /**
     * 从元（元/分）构造，默认 {@link RoundingMode#HALF_UP HALF_UP} 舍入。例如 {@code fromYuan(new BigDecimal("15000"))} → 1.5万元。
     */
    public static WanYuan fromYuan(BigDecimal yuan) {
        return fromYuan(yuan, RoundingMode.HALF_UP);
    }

    public static WanYuan fromYuan(BigDecimal yuan, RoundingMode roundingMode) {
        ValidateUtils.notNull(yuan);
        ValidateUtils.notNull(roundingMode);
        var result = yuan.divide(WAN, SCALE, roundingMode);
        return new WanYuan(result);
    }

    /**
     * 转换为元（乘以 10000），结果不保留小数。
     */
    public BigDecimal toYuan() {
        return decimalValue().multiply(WAN).setScale(0, RoundingMode.UNNECESSARY);
    }

    /**
     * 展示文本。格式：{@code 111.11万元}。
     */
    public String toDisplayString() {
        return decimalValue().toPlainString() + "万元";
    }

    /**
     * 数值序比较（按十进制值）。
     */
    @Override
    public int compareTo(WanYuan other) {
        return decimalValue().compareTo(other.decimalValue());
    }

    @Override
    public String toString() {
        return "WanYuan[value=" + value() + "]";
    }
}
