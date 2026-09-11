package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

/**
 * 中文姓名 DP —— 仅接受 2-20 个中文字符（契约即中文名；护照英文名不属于本类型），不可变、自校验。
 * <p>
 * {@code maskedValue()} 由 {@link MaskedChineseName} 的统一留首字掩码即时生成。
 *
 * @see SensitiveValue
 * @see MaskedChineseName 掩码权威实现
 * @see Ciphertext 可逆加密信封
 */
@EqualsAndHashCode(callSuper = true)
public final class ChineseName extends SensitiveValue {

    private static final Pattern PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]{2,20}$");

    private ChineseName(String value) {
        super(value);
        ValidateUtils.matches(value, PATTERN);
    }

    /**
     * 工厂 —— 校验统一在 {@code SensitiveValue} 构造器与 {@code matches}（单一入口点）。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ChineseName of(String raw) {
        return new ChineseName(raw);
    }

    @Override
    public String maskedValue() {
        return MaskedChineseName.from(this).value();
    }
}
