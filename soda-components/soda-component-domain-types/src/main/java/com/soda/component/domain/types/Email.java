package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 电子邮箱 DP —— 格式校验（{@code local@domain}），不可变、自校验。
 * <p>
 * 值归一化为小写；{@code localPart()} 与 {@code domain()} 为派生字段（Lombok 流式访问器）；
 * {@code maskedValue()} 由 {@link MaskedEmail} 的掩码算法即时生成。
 *
 * @see SensitiveValue
 * @see MaskedEmail 掩码权威实现
 * @see Ciphertext 可逆加密信封
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Accessors(fluent = true)
public final class Email extends SensitiveValue {

    private static final Pattern PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @Getter
    private final String localPart;

    @Getter
    private final String domain;

    private Email(String value) {
        super(value == null ? null : value.toLowerCase(Locale.ROOT));
        ValidateUtils.matches(value(), PATTERN);
        var v = value();
        var at = v.indexOf('@');
        this.localPart = v.substring(0, at);
        this.domain = v.substring(at + 1);
    }

    @Override
    public String maskedValue() {
        return MaskedEmail.from(this).value();
    }

    /**
     * 工厂 —— 校验统一在 {@code SensitiveValue} 构造器与 {@code matches}（单一入口点）。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Email of(String raw) {
        return new Email(raw);
    }
}