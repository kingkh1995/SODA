package com.soda.component.domain.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Type;
import com.soda.component.domain.util.TypeConfig;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 凭证哈希 DP —— 原始凭证经 {@link com.soda.component.domain.gateway.CredentialHasher} 哈希后的值。
 * <p>
 * 算法无关，不绑定任何具体哈希算法（BCrypt、Argon2、SCrypt 等），算法选择由基础设施层决定。
 * <p>
 * 不可变、自校验（非 blank）、可比较。与 {@link RawCredential} 不同，哈希值不敏感，可安全序列化。
 *
 * @see Type
 * @see com.soda.component.domain.gateway.CredentialHasher
 * @see RawCredential
 */
public record CredentialHash(String value) implements Type {

    private static final int MAX_LENGTH = Math.max(128, TypeConfig.PROVIDER.credentialHashMaxLength());

    public CredentialHash {
        ValidateUtils.hasText(value);
        ValidateUtils.maxLength(value, MAX_LENGTH);
    }

    @JsonValue
    public String value() {
        return this.value;
    }
}
