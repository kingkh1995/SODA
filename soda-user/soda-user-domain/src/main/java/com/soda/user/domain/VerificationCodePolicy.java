package com.soda.user.domain;

import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

import java.time.Duration;

/**
 * 验证码策略 DP — code 长度 + 有效期。
 * <p>
 * 解析链：per-account 覆盖 → ServiceLoader SPI → 子类静态 {@code DEFAULT_POLICY}。
 * <p>
 * 不可变、自校验、可序列化、可比较。
 *
 * @see Type
 */
public record VerificationCodePolicy(
        int codeLength,
        Duration expiry
) implements Type {


    /**
     * 默认短信验证码策略：6 位，5 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_SMS = new VerificationCodePolicy(6, Duration.ofMinutes(5));

    /**
     * 默认邮箱验证码策略：8 位，30 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_EMAIL = new VerificationCodePolicy(8, Duration.ofMinutes(30));

    public VerificationCodePolicy {
        ValidateUtils.range(codeLength, 1, 20);
        ValidateUtils.minValue(expiry, Duration.ZERO, false);
    }

}
