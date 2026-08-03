package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 随机字符串 DP — 不可变、自校验。
 * <p>
 * 由 {@link com.soda.component.domain.gateway.RandomStringGenerator} 生成。
 * 字符集和随机源由基础设施层决定，领域层不关心。
 *
 * @see Type
 * @see com.soda.component.domain.gateway.RandomStringGenerator
 */
public record RandomString(String value) implements Type {

    public RandomString {
        ValidateUtils.hasText(value);
    }

    @JsonValue
    public String value() {
        return this.value;
    }
}
