package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 身份证号 DP —— 中国身份证号格式校验 + 末位 X 大写归一化，不可变、自校验。
 * <p>
 * {@code maskedValue()} 由 {@link MaskedIdCard} 的掩码算法即时生成。
 *
 * @see SensitiveValue
 * @see MaskedIdCard 掩码权威实现
 * @see Ciphertext 可逆加密信封
 */
@EqualsAndHashCode(callSuper = true)
public final class IdCard extends SensitiveValue {

    private static final Pattern PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    private IdCard(String value) {
        super(value == null ? null : value.toUpperCase(Locale.ROOT));
        ValidateUtils.matches(value(), PATTERN);
    }

    @Override
    public String maskedValue() {
        return MaskedIdCard.from(this).value();
    }

    /**
     * 工厂 —— 校验统一在 {@code SensitiveValue} 构造器与 {@code matches}（单一入口点）。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static IdCard of(String raw) {
        return new IdCard(raw);
    }
}