package com.soda.user.infrastructure.gateway;

import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.RawCredential;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * {@link CredentialHasher} 的 BCrypt 实现。
 * <p>
 * 使用 Spring Security 的 {@link BCryptPasswordEncoder}（{@code spring-security-crypto}
 * 独立依赖，不引入整个 Spring Security）；版本 10 轮数默认。调用方应在
 * {@code hash()}/{@code matches()} 返回后尽快丢弃 {@link RawCredential} 引用
 * （接口契约，本实现不保留、不日志）。
 */
@Component
public class CredentialHasherImpl implements CredentialHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public CredentialHash hash(RawCredential credential) {
        return new CredentialHash(encoder.encode(credential.rawValue()));
    }

    @Override
    public boolean matches(RawCredential credential, CredentialHash hash) {
        return encoder.matches(credential.rawValue(), hash.value());
    }
}
