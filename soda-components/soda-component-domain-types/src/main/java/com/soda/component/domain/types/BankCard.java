package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

/**
 * 银行卡号 DP —— 银行卡号格式校验（13-19 位数字），不可变、自校验。
 * <p>
 * {@code maskedValue()} 由 {@link MaskedBankCard} 的掩码算法即时生成。
 *
 * @see SensitiveValue
 * @see MaskedBankCard 掩码权威实现
 * @see Ciphertext 可逆加密信封
 */
@EqualsAndHashCode(callSuper = true)
public final class BankCard extends SensitiveValue {

    private static final Pattern PATTERN = Pattern.compile("^\\d{13,19}$");

    private BankCard(String value) {
        super(value);
        ValidateUtils.matches(value, PATTERN);
    }

    /**
     * 工厂 —— 校验统一在 {@code SensitiveValue} 构造器与 {@code matches}（单一入口点）。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BankCard of(String raw) {
        return new BankCard(raw);
    }

    @Override
    public String maskedValue() {
        return MaskedBankCard.from(this).value();
    }
}
