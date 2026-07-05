package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.TypeConfig;
import com.soda.component.support.util.ValidateUtils;

/**
 * 短信内容 DP — 限定长度的纯文本，不可变、自校验、可比较。
 * <p>
 * 校验规则：非 blank，最长 70 字符（参照主流短信平台单条上限）。
 *
 * @see Type
 */
public record SmsContent(String value) implements Type {

    private static final int MAX_LENGTH = Math.max(70, TypeConfig.PROVIDER.smsContentMaxLength());

    public SmsContent {
        ValidateUtils.hasText(value);
        ValidateUtils.maxLength(value, MAX_LENGTH);
    }

    @JsonValue
    public String value() {
        return this.value;
    }
}
