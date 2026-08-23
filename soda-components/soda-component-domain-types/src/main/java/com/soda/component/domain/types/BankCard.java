package com.soda.component.domain.types;

import com.soda.component.domain.SensitiveValue;
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

    public BankCard(String value) {
        super(value);
        ValidateUtils.matches(value(), PATTERN);
    }

    @Override
    public String maskedValue() {
        return MaskedBankCard.from(this).value();
    }
}
