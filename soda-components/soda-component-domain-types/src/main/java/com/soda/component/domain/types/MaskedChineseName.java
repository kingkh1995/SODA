package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 已脱敏中文姓名 —— 存储格式为 {@code 张*}：保留首字、其余替换为星号的均匀掩码唯一权威实现
 * （主流规则，无复姓特例，见 ADR-0032 修订注记 4/5）。
 *
 * @see ChineseName 原始值 DP
 * @see Ciphertext 可逆加密信封
 */
public record MaskedChineseName(String value) implements StringLiteralType {

    private static final Pattern PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]\\*+$");


    public MaskedChineseName {
        ValidateUtils.matches(value, PATTERN);
    }

    public static MaskedChineseName of(String value) {
        return new MaskedChineseName(value);
    }

    public static MaskedChineseName from(ChineseName chineseName) {
        return new MaskedChineseName(maskOf(chineseName.value()));
    }
    /**
     * 掩码生产算法：保留首字，其余替换为 *。仅服务 {@link #from(ChineseName)}。
     */
    private static String maskOf(String rawValue) {
        return rawValue.substring(0, 1) + "*".repeat(rawValue.length() - 1);
    }
}
