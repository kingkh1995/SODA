package com.soda.component.support.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.support.types.PasswordHash;
import com.soda.component.support.types.RawPassword;

/**
 * 密码编码器 Gateway — 对密码进行哈希编码及校验的抽象。
 * <p>
 * 通用契约，实现层对接 Spring Security 的 {@code PasswordEncoder}（BCrypt 等算法）。
 *
 * @see Gateway
 */
public interface PasswordEncoder extends Gateway {

    /** 对原始密码进行编码（哈希）。 */
    PasswordHash encode(RawPassword rawPassword);

    /** 校验原始密码是否与编码后的密码匹配。 */
    boolean matches(RawPassword rawPassword, PasswordHash hash);
}
