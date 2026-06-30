package com.soda.component.support.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.support.util.ValidateUtils;

/**
 * 随机字符串 DP — 不可变、自校验。
 * <p>
 * 由 {@link com.soda.component.support.gateway.RandomStringGenerator} 生成。
 * 字符集和随机源由基础设施层决定，领域层不关心。
 *
 * @see Type
 * @see com.soda.component.support.gateway.RandomStringGenerator
 */
public record RandomString(String value) implements Type {

    @JsonValue
    public String value() { return this.value; }

    public RandomString {
        ValidateUtils.hasText(value);
    }

    /**
     * 校验原始值是否与给定字符串匹配。
     *
     * @param rawValue 待匹配的字符串
     * @return true 若匹配
     */
    public boolean matches(String rawValue) {
        return value.equals(rawValue);
    }
}
