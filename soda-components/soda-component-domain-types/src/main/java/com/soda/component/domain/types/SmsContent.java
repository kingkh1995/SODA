package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.TypeConfig;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 短信内容 DP — 限定长度的纯文本，不可变、自校验。
 *
 * <p>校验规则：非 blank，最长 70 字符（参照主流短信平台单条上限）。
 *
 * @see Type
 */
public record SmsContent(String value) implements StringLiteralType {

    /** 短信内容长度上限（字符）——提供方可经 {@link TypeConfig} 覆盖，70（主流平台单条上限）为不可下探的地板值。 */
    public static final int MAX_LENGTH = Math.max(70, TypeConfig.PROVIDER.smsContentMaxLength());

    public SmsContent {
        ValidateUtils.hasText(value);
        ValidateUtils.maxLength(value, MAX_LENGTH);
    }
}
