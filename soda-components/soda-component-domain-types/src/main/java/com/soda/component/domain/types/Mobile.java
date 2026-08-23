package com.soda.component.domain.types;

import com.soda.component.domain.SensitiveValue;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

/**
 * 手机号 DP —— 中国手机号格式校验，不可变、自校验。
 * <p>
 * {@code maskedValue()} 由 {@link MaskedMobile} 的掩码算法即时生成；{@code toString()} 输出脱敏值；
 * JSON 序列化（{@link #value()}）保持明文。
 *
 * @see SensitiveValue
 * @see MaskedMobile 掩码权威实现
 * @see Ciphertext 可逆加密信封
 */
@EqualsAndHashCode(callSuper = true)
public final class Mobile extends SensitiveValue {

    private static final Pattern PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    public Mobile(String value) {
        super(value);
        ValidateUtils.matches(value(), PATTERN);
    }

    @Override
    public String maskedValue() {
        return MaskedMobile.from(this).value();
    }
}
