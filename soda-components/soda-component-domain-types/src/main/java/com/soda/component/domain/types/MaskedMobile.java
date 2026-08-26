package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 已脱敏手机号 —— 存储格式为 {@code 138****8000}（保留前 3 位和后 4 位），掩码唯一权威实现（见 ADR-0032）。
 *
 * @see Mobile 原始值 DP
 * @see Ciphertext 可逆加密信封
 */
public record MaskedMobile(String value) implements StringLiteralType {

    private static final Pattern PATTERN = Pattern.compile("^1[3-9]\\d\\*{4}\\d{4}$");

    public MaskedMobile {
        ValidateUtils.matches(value, PATTERN);
    }

    /**
     * 由原始值 DP 派生脱敏值（掩码算法唯一公开通道）。
     */
    public static MaskedMobile from(Mobile mobile) {
        return new MaskedMobile(maskOf(mobile.value()));
    }

    /**
     * 掩码生产算法：保留前 3 位和后 4 位。仅服务 {@link #from(Mobile)}。
     */
    private static String maskOf(String rawValue) {
        return rawValue.substring(0, 3) + "****" + rawValue.substring(7);
    }
}