package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 已脱敏银行卡号 —— 存储格式为 {@code 622588******6789}，保留 BIN 前 6 位和后 4 位、长度不变的掩码唯一权威实现（见 ADR-0032）。
 *
 * @see BankCard 原始值 DP
 * @see Ciphertext 可逆加密信封
 */
public record MaskedBankCard(String value) implements StringLiteralType {

    private static final Pattern PATTERN = Pattern.compile("^\\d{6}\\*{3,13}\\d{4}$");

    public MaskedBankCard {
        ValidateUtils.matches(value, PATTERN);
    }

    public static MaskedBankCard of(String value) {
        return new MaskedBankCard(value);
    }

    public static MaskedBankCard from(BankCard bankCard) {
        return new MaskedBankCard(maskOf(bankCard.value()));
    }

    /**
     * 掩码生产算法：保留 BIN 前 6 位和后 4 位，长度不变。仅服务 {@link #from(BankCard)}。
     */
    private static String maskOf(String rawValue) {
        var len = rawValue.length();
        var prefixLen = Math.min(6, len - 4);
        var maskLen = len - prefixLen - 4;
        return rawValue.substring(0, prefixLen) + "*".repeat(maskLen) + rawValue.substring(len - 4);
    }
}
