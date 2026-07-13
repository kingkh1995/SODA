package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.RawCredential;

/**
 * 凭证哈希器 Gateway —— 对原始凭证进行哈希编码及校验的抽象。
 * <p>
 * 算法无关，实现层可对接 BCrypt、Argon2、SCrypt 等。参考 Spring Security {@code PasswordEncoder}
 * 的设计，但输入类型固定为 {@link RawCredential}（而非 {@code CharSequence}），
 * 利用类型系统确保不会被非凭证字符串误调用。
 * <p>
 * 使用约定：调用方应在 {@code hash()} / {@code matches()} 返回后尽快丢弃 {@link RawCredential} 引用，
 * 不保留、不日志。
 *
 * @see Gateway
 * @see CredentialHash
 */
public interface CredentialHasher extends Gateway {

    /**
     * 对原始凭证进行哈希。
     */
    CredentialHash hash(RawCredential credential);

    /**
     * 校验原始凭证是否与哈希值匹配。
     */
    boolean matches(RawCredential credential, CredentialHash hash);
}
