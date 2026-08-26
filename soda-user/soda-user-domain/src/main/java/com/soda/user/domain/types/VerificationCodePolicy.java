package com.soda.user.domain.types;

import com.soda.component.domain.Type;
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import com.soda.component.domain.util.ValidateUtils;

import java.time.Duration;

/**
 * 验证码策略 DP — 码长 + 有效期 + 字符集。
 * <p>
 * 验证码的唯一策略类型：作为通道常量载体与 {@code Verification} create 构造<b>必传</b>输入
 * （决定码长、过期时间与字符集，效果物化进 {@code VerificationCode}，不落验证实体，见 ADR-0011）。
 * 码形是通道级规则（短信=纯数字、邮箱=字母数字），非账号配置字段——账户实体不持有 policy
 * （见 ADR-0018）。策略决策属调用方<b>场景</b>：聚合不做通道→策略映射；通道默认
 * （{@link #DEFAULT_SMS}/{@link #DEFAULT_EMAIL}）仍是通道级常量，场景专属策略可不依赖默认
 * （UCC 专属：双通道统一纯数字/短时效，见 ADR-0026）。
 * <p>
 * 属性为 DP 字面值语义的嵌套组合（见 ADR-0018）：{@code codeLength} 为 {@link PositiveInt}、
 * {@code codeAlphabet} 为 {@link Alphabet}——字符集不变量由 {@link Alphabet} 构造器校验
 * （单一校验源），create 时零转换直传生成器。
 * <p>
 * 不可变、自校验、可序列化、可比较。
 *
 * @see Type
 */
public record VerificationCodePolicy(
        PositiveInt codeLength,
        Duration expiry,
        Alphabet codeAlphabet
) implements Type {

    /**
     * 默认短信验证码策略：6 位纯数字，5 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_SMS =
            new VerificationCodePolicy(PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);

    /**
     * 默认邮箱验证码策略：8 位去混淆字母数字（{@link Alphabet#UNAMBIGUOUS_ALPHANUMERIC}），30 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_EMAIL =
            new VerificationCodePolicy(PositiveInt.of(8), Duration.ofMinutes(30), Alphabet.UNAMBIGUOUS_ALPHANUMERIC);

    public VerificationCodePolicy {
        ValidateUtils.notNull(codeLength);
        ValidateUtils.range(codeLength.value(), 1, 20);   // PositiveInt 保证 ≥1，此处限业务范围 ≤20
        ValidateUtils.minValue(expiry, Duration.ZERO, false);
        ValidateUtils.notNull(codeAlphabet);
    }

}
