package com.soda.user.domain.types;

import com.soda.component.domain.Type;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.util.ValidateUtils;

import java.time.Instant;

/**
 * 验证码 DP — code + expireAt + used。
 * <p>
 * 封装验证码的完整生命周期：生成 → 验证 → 使用/过期。
 * 不可变、自校验、可序列化、可比较。
 * <p>
 * 通过 {@link #use()} 返回一个标记为已使用的副本。
 *
 * @see Type
 */
public record VerificationCode(
        String code,
        Instant expireAt,
        boolean used
) implements Type {

    public VerificationCode {
        ValidateUtils.hasText(code);
        ValidateUtils.notNull(expireAt);
    }

    /**
     * 是否已过期（基于当前时间）。委托至 {@link #expiredAt(Instant)}。
     */
    public boolean expired() {
        return expiredAt(Instant.now());
    }

    /**
     * 在指定时刻检查是否已过期（纯函数，无隐式时钟依赖）。
     */
    public boolean expiredAt(Instant instant) {
        return instant.isAfter(expireAt);
    }


    /**
     * 校验输入 code 是否匹配且未过期且未使用。
     *
     * @param inputCode 用户输入的验证码
     * @return true 校验通过；false 不匹配 / 已过期 / 已使用 / 输入为 null
     */
    public boolean verify(RandomString inputCode) {
        return !used && !expired() && inputCode != null && inputCode.matches(code);
    }

    /**
     * 返回一个标记为已使用的新 VerificationCode 实例（不可变风格）。
     *
     * @return 新实例，used = true
     */
    public VerificationCode use() {
        return used() ? this : new VerificationCode(code, expireAt, true);
    }

}
