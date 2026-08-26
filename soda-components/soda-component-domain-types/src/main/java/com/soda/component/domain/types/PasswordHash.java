package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.util.TypeConfig;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

/**
 * 口令哈希 —— 哈希族敏感特例（ADR-0033）。
 * <p>
 * PHC 自描述格式（{@code $<id>$[参数]$盐$校验和}），算法标识与成本参数在带内逐行可变，
 * 因此是哈希族中唯一能从字面值验证自身算法的类型。前缀白名单：
 * {@code argon2id|argon2i|argon2d|bcrypt($2[abcy]$)|scrypt|pbkdf2(-sha256/-sha512)?}。
 * <p>
 * <b>本类型的字面值本身即攻击素材</b>（盐 + 校验和可离线枚举弱口令，OWASP Password Storage /
 * CWE-532），因此是三类型中唯一继承 {@link SensitiveValue} 的哈希类——toString 强制遮蔽由基类
 * final 化保证；{@link #maskedValue()} 采用格式感知截断：保留至盐段之前（盐段 = 倒数第二段，
 * argon2id 含完整成本参数；bcrypt 盐与校验和融合形态退化为 7 字符截断），零秘密材料泄露。
 * <p>
 * {@code @JsonValue} 裸值序列化为家族统一语义，但本类型字面值即攻击素材——含本类型的聚合
 * 禁止从 adapter 直接序列化输出（必须经 WebAssembler → DTO 装配），否则日志脱敏体系被整体绕过。
 *
 * @see SensitiveValue
 * @see com.soda.component.domain.gateway.PasswordHasher
 */
@EqualsAndHashCode(callSuper = true)
public final class PasswordHash extends SensitiveValue {

    /**
     * PHC 算法 id 前缀白名单（参数语法不做穷尽校验，交由验证器）。
     */
    private static final Pattern PHC_PREFIX =
            Pattern.compile("^\\$(?:argon2(?:id|i|d)\\$|2[abcy]\\$\\d{2}\\$|scrypt\\$|pbkdf2(?:-sha256|-sha512)?\\$).*$");

    /**
     * 长度上限 —— SPI 可配置（默认 200，对齐 user 表 password_hash 列），下限 128。
     */
    private static final int MAX_LENGTH = Math.max(128, TypeConfig.PROVIDER.passwordHashMaxLength());

    private PasswordHash(String value) {
        ValidateUtils.hasText(value);
        ValidateUtils.maxLength(value, MAX_LENGTH);
        ValidateUtils.matches(value, PHC_PREFIX);
        super(value);
    }

    /**
     * 工厂 —— 校验统一在构造器（单一入口点），此处仅委托。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PasswordHash of(String phcString) {
        return new PasswordHash(phcString);
    }

    /**
     * 格式感知截断 —— 盐段为倒数第二段，保留至其之前的算法 id / 版本 / 成本参数段，追加省略号；
     * bcrypt 等盐与校验和融合的四段形态退化为 7 字符截断。
     */
    @Override
    public String maskedValue() {
        var v = value();
        var segments = v.split("\\$", -1);
        if (segments.length >= 5) {
            int cut = 0;
            for (int i = 0; i < segments.length - 2; i++) {
                cut += segments[i].length() + 1;
            }
            return v.substring(0, cut) + "***";
        }
        return v.substring(0, Math.min(7, v.length())) + "***";
    }
}
