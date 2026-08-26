package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 已脱敏电子邮箱 —— 存储格式为 {@code t***@example.com}（用户名保留首字符，其余替换为 *），掩码唯一权威实现（见 ADR-0032）。
 * <p>
 * 用户名首字符必须为字母或数字（脱敏语义：保留首字符作为识别锚点，掩盖其余部分；纯 `*` 用户名首字符也泄露完，违背脱敏语义）。
 *
 * @see Email 原始值 DP
 * @see Ciphertext 可逆加密信封
 */
public record MaskedEmail(String value) implements StringLiteralType {

    /**
     * 字母/数字首字符 + ≥1 个 `*` + `@` + 域名段。首字符锚定是脱敏语义的最小披露单元。
     */
    private static final Pattern PATTERN =
            Pattern.compile("^[a-zA-Z0-9]\\*+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    public MaskedEmail {
        ValidateUtils.matches(value, PATTERN);
    }
    /**
     * 由原始值 DP 派生脱敏值（掩码算法唯一公开通道）。
     */
    public static MaskedEmail from(Email email) {
        return new MaskedEmail(maskOf(email.value()));
    }

    /**
     * 掩码生产算法：用户名保留首字符，其余替换为 *。仅服务 {@link #from(Email)}。
     */
    private static String maskOf(String rawValue) {
        var at = rawValue.indexOf('@');
        var local = rawValue.substring(0, at);
        var domain = rawValue.substring(at);
        return (local.isEmpty() ? "***" : local.charAt(0) + "***") + domain;
    }
}