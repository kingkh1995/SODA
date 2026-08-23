package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 已脱敏邮箱 —— 存储格式为 {@code t***@example.com}，用户名保留首字符的掩码唯一权威实现（见 ADR-0032）。
 *
 * @see Email 原始值 DP
 * @see Ciphertext 可逆加密信封
 */
public record MaskedEmail(String value) implements StringLiteralType {

    private static final Pattern PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]\\*{3}@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public MaskedEmail {
        ValidateUtils.matches(value, PATTERN);
    }

    public static MaskedEmail of(String value) {
        return new MaskedEmail(value);
    }

    public static MaskedEmail from(Email email) {
        return new MaskedEmail(maskOf(email.value()));
    }

    /**
     * 掩码生产算法：用户名保留首字符。仅服务 {@link #from(Email)}。
     */
    private static String maskOf(String rawValue) {
        var at = rawValue.indexOf('@');
        var local = rawValue.substring(0, at);
        var domain = rawValue.substring(at);
        return (local.isEmpty() ? "***" : local.charAt(0) + "***") + domain;
    }
}
