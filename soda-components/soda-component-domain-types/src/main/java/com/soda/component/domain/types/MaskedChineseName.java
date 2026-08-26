package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.util.ValidateUtils;

import java.util.regex.Pattern;

/**
 * 已脱敏中文姓名 —— 存储格式为 {@code 张*}（首字保留 + 恰好 1 个 {@code *} 替代其余，与原始名长度无关），掩码唯一权威实现（见 ADR-0032）。
 * <p>
 * <b>脱敏规则</b>：原始名 ≥ 2 字走脱敏；1 字名无隐私价值，调用方直接展示 {@link ChineseName#value()} 即可，<b>不</b>走本 DP（PATTERN 拒绝 0 {@code *}）。
 *
 * @see ChineseName 原始值 DP
 * @see Ciphertext 可逆加密信封
 */
public record MaskedChineseName(String value) implements StringLiteralType {

    /**
     * 中文字符 + 恰好 1 个 `*`（与 maskOf 输出对齐：无论原名长度一律张* / 李* / 欧*）
     */
    private static final Pattern PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]\\*$");

    public MaskedChineseName {
        ValidateUtils.matches(value, PATTERN);
    }

    /**
     * 由原始值 DP 派生脱敏值（掩码算法唯一公开通道）。
     */
    public static MaskedChineseName from(ChineseName chineseName) {
        return new MaskedChineseName(maskOf(chineseName.value()));
    }

    /**
     * 掩码生产算法：保留首字 + 恰好 1 个 `*`（不论原名多长，与字面长度脱敏）。仅服务 {@link #from(ChineseName)}。
     */
    private static String maskOf(String rawValue) {
        return rawValue.substring(0, 1) + "*";
    }
}