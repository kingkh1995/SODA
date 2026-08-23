package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 已脱敏身份证号 —— 存储格式为 {@code 110101********1234}，保留前 6 位（地区码）和后 4 位、
 * 校验位 X 大写归一化的掩码唯一权威实现（见 ADR-0032）。
 *
 * @see IdCard 原始值 DP
 * @see Ciphertext 可逆加密信封
 */
public record MaskedIdCard(String value) implements StringLiteralType {

    private static final Pattern PATTERN = Pattern.compile("^\\d{6}\\*{8}[\\dXx]{4}$");

    public MaskedIdCard {
        if (value != null) {
            value = value.toUpperCase(Locale.ROOT);
        }
        ValidateUtils.matches(value, PATTERN);
    }

    public static MaskedIdCard of(String value) {
        return new MaskedIdCard(value);
    }

    public static MaskedIdCard from(IdCard idCard) {
        return new MaskedIdCard(maskOf(idCard.value()));
    }

    /**
     * 掩码生产算法：保留前 6 位（地址码）和后 4 位，中间 8 位替换为 *。仅服务 {@link #from(IdCard)}。
     */
    private static String maskOf(String rawValue) {
        return rawValue.substring(0, 6) + "********" + rawValue.substring(14);
    }
}
