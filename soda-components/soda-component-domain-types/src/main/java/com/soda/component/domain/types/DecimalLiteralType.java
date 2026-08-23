package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 小数字面量基类（见 ADR-0031 及其修订注记）—
 * 「wire≠semantic」类型：规范值为 {@link String}（{@code value}，{@code @JsonValue}
 * 继承自家族契约 {@link StringLiteralType}，防 JSON 数字精度丢失），派生 {@link BigDecimal}
 * 缓存（{@code decimalValue}），不参与序列化/{@code equals}/{@code hashCode}。
 * 契约与缓存不变量同置此类——无独立子契约接口（全仓零多态调用点，不过度设计）。
 * <p>
 * Lombok 端到端生成（零手写 equals/hashCode/accessor）：
 * <ul>
 *   <li>{@link EqualsAndHashCode}（{@code onlyExplicitlyIncluded}，canEqual=instanceof 本类；
 *       子类 {@code callSuper=true} 且 canEqual 收窄为 instanceof 子类——
 *       使 {@code WanYuan("1.00")≠Percentage("1.00")}，见 {@link DecimalLiteralTypeTest}）</li>
 *   <li>{@link Getter}+{@link Accessors}（fluent）生成 {@code value()}/{@code decimalValue()} 访问器</li>
 * </ul>
 * 构造期规范化：null 校验 → maxScale → {@code setScale(UNNECESSARY)} → {@link #validate(BigDecimal)} 钩子。
 * 子类提供 {@code SCALE}（构造器参）+ {@link #validate(BigDecimal)} 不变量钩子（默认空）+ 工厂/单位换算。
 * <p>
 * 沿用「abstract 基类承载家族不变量」模式（原以 AbstractEncryptedValue 为先例，现见
 * {@link com.soda.component.domain.SensitiveValue}，ADR-0033）。
 *
 * @see StringLiteralType
 * @see WanYuan
 * @see Percentage
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public abstract class DecimalLiteralType implements StringLiteralType {

    @EqualsAndHashCode.Include
    @Getter
    private final String value;

    @Getter
    private final BigDecimal decimalValue;

    /**
     * @param raw   原始 BigDecimal（构造前）
     * @param scale 规范小数位（0–4，子类提供 {@code SCALE}）
     */
    protected DecimalLiteralType(BigDecimal raw, int scale) {
        ValidateUtils.notNull(raw);
        ValidateUtils.maxScale(raw, scale);
        var normalized = raw.setScale(scale, RoundingMode.UNNECESSARY);
        validate(normalized);
        this.value = normalized.toPlainString();
        this.decimalValue = normalized;
    }

    /**
     * 子类不变量钩子（默认空）。在 {@code setScale} 后、赋值前调用，如 {@code Percentage} 覆写加 {@code [0,100]} range。
     */
    protected void validate(BigDecimal normalized) {
        // 默认无额外约束
    }
}
