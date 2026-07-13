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
    private static final int SCALE = Math.clamp(TypeConfig.PROVIDER.wanYuanScale(), 0, 4);

    @EqualsAndHashCode.Include
    private final String value;
    @Getter
    private final BigDecimal bigDecimalValue;

    private WanYuan(BigDecimal raw) {
        ValidateUtils.notNull(raw);
        ValidateUtils.maxScale(raw, SCALE);
        var normalized = raw.setScale(SCALE, RoundingMode.UNNECESSARY);
        this.value = normalized.toPlainString();
        this.bigDecimalValue = normalized;
    }

    /**
     * JSON 反序列化入口。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static WanYuan of(String jsonValue) {
        var bd = ParseUtils.parseBigDecimal(jsonValue);
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

    @JsonValue
    public String value() {
        return this.value;
    }

    /**
     * 转换为元（乘以 10000），结果不保留小数。
     */
    public BigDecimal toYuan() {
        return bigDecimalValue.multiply(WAN).setScale(0, RoundingMode.UNNECESSARY);
    }

    /**
     * 展示文本。格式：{@code 111.11万元}。
     */
    public String toDisplayString() {
        return bigDecimalValue.toPlainString() + "万元";
    }

    @Override
    public String toString() {
        return "WanYuan[value=" + value + "]";
    }
}
