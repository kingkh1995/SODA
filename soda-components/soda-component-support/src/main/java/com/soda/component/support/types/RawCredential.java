package com.soda.component.support.types;

import com.soda.component.domain.Secret;
import com.soda.component.support.util.ValidateUtils;

/**
 * 原始凭证 —— 长期有效凭证（密码、API Key、Token 等）的原始值。
 * <p>
 * 通用载体，不绑定任何特定算法。上游各 DP（如 {@code Password}、{@code ApiKey}）
 * 各自校验规则后通过 {@link #of(CharSequence)} 创建，再传递给 {@link com.soda.component.support.gateway.CredentialHasher}。
 * <p>
 * 安全约定（应用层负责）：
 * <ul>
 *   <li>不写入日志</li>
 *   <li>不保留引用</li>
 *   <li>使用后尽快丢弃</li>
 * </ul>
 * <p>
 * 序列化保护：构造器私有 + 值访问器命名为 {@link #internalValue()} 而非 {@code value()} 或 {@code getValue()}，
 * 避免被 Jackson 等序列化框架自动发现。
 *
 * @see Secret
 * @see com.soda.component.support.gateway.CredentialHasher
 */
public final class RawCredential extends Secret {

    private final String value;
    public RawCredential(String value) {
        ValidateUtils.nonBlank(value);
        this.value = value;
    }

    /**
     * 读取内部值。仅限 {@link com.soda.component.support.gateway.CredentialHasher} 使用。
     * 不叫 {@code value()} 或 {@code getValue()}，避免被序列化框架自动发现。
     *
     * @return 原始凭证字符串
     */
    public String internalValue() {
        return value;
    }
}
