package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.Type;
import com.soda.component.support.types.RandomString;
import com.soda.component.support.util.ValidateUtils;

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
        @JsonProperty("code") String code,
        @JsonProperty("expireAt") Instant expireAt,
        @JsonProperty("used") boolean used
) implements Type {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VerificationCode {
        ValidateUtils.nonBlank(code);
        ValidateUtils.notNull(expireAt);
    }

    /** 是否已过期（基于当前时间）。record 风格命名，避免 Jackson 序列化。 */
    public boolean expired() {
        return Instant.now().isAfter(expireAt);
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
