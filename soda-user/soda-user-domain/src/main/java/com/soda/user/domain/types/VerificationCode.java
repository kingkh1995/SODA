package com.soda.user.domain.types;

import com.soda.component.domain.Type;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.util.ValidateUtils;

import java.time.Instant;

/**
 * 验证码 DP — code + expireAt。
 * <p>
 * 封装验证码的匹配与过期判断；验证码生命周期（待验证/已验证/已使用）
 * 由聚合状态表达，不在 DP 内重复记录使用状态（单一事实源，见 ADR-0011）。
 * 不可变、自校验、可序列化、可比较。
 *
 * @see Type
 * @see RandomString
 */
public record VerificationCode(
        String code,
        Instant expireAt
) implements Type {

    public VerificationCode {
        ValidateUtils.hasText(code);
        ValidateUtils.notNull(expireAt);
    }

    public static VerificationCode from(RandomString randomString, Instant expireAt) {
        return new VerificationCode(randomString.value(), expireAt);
    }

    /**
     * 在指定时刻检查是否已过期（纯函数，无隐式时钟依赖）。
     */
    public boolean expiredAt(Instant instant) {
        return instant.isAfter(expireAt);
    }

    /**
     * 校验输入 code 是否匹配（纯匹配，不涉及过期判断）。
     * <p>
     * 过期判断由调用方在指定时刻通过 {@link #expiredAt(Instant)} 完成，
     * 或由 {@link com.soda.user.domain.Verification} 聚合统一校验。
     *
     * @param inputCode 用户输入的验证码
     * @return true 匹配；false 不匹配
     */
    public boolean matches(RandomString inputCode) {
        return code.equals(inputCode.value());
    }
}