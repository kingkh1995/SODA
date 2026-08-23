package com.soda.component.domain.types;

import com.soda.component.domain.StringLiteralType;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 随机字符串 DP — 不可变、自校验。
 * <p>
 * 由 {@link com.soda.component.domain.gateway.RandomStringGenerator} 生成。
 * 字符集由调用方以 {@link Alphabet} 指定（领域拥有，见 ADR-0018），随机源由基础设施层决定。
 *
 * @see Type
 * @see com.soda.component.domain.gateway.RandomStringGenerator
 */
public record RandomString(String value) implements StringLiteralType {

    public RandomString {
        ValidateUtils.hasText(value);
    }

}
