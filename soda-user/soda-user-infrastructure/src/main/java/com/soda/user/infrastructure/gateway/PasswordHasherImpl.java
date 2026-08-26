package com.soda.user.infrastructure.gateway;

import com.soda.component.domain.gateway.PasswordHasher;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.SecretValue;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link PasswordHasher} 的 BCrypt 实现。
 * <p>
 * 使用 Spring Security 的 {@link BCryptPasswordEncoder}（{@code spring-security-crypto}
 * 独立依赖，不引入整个 Spring Security）；固定 10 轮成本因子（{@link #needsRehash} 以同值
 * 判定升级）。bcrypt 的 Blowfish 密钥调度最多吸收 72 字节输入、实现普遍静默截断——
 * {@link #hash()} 在入口 fail-fast 拒绝超限凭证；{@link #verify()} 不拦（存量截断哈希靠
 * 截断对称性照常匹配，见 ADR-0033）。调用方应在 {@code hash()}/{@code verify()} 返回后尽快丢弃
 * {@link SecretValue} 引用（接口契约，本实现不保留、不日志）。
 */
@Component
public class PasswordHasherImpl implements PasswordHasher {

    /**
     * 成本因子 —— 显式常量，编码强度与 {@link #needsRehash} 升级判据同源。
     */
    private static final int COST = 10;

    /**
     * BCrypt 强度段提取（{@code $2[abcy]$NN$} 的 NN 为两位成本因子）。
     */
    private static final Pattern BCRYPT_COST = Pattern.compile("^\\$2[abcy]\\$(\\d{2})\\$.*");

    /**
     * bcrypt 输入字节上限 —— 算法定理限制（Blowfish 密钥调度），超出部分被实现静默截断，
     * 导致前 72 字节相同的口令验证等价。仅约束 {@link #hash()} 新增口令，不入端口契约。
     */
    private static final int BCRYPT_MAX_INPUT_BYTES = 72;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COST);

    @Override
    public PasswordHash hash(SecretValue credential) {
        if (credential.rawValue().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_INPUT_BYTES) {
            throw new IllegalArgumentException("credential exceeds BCrypt 72-byte input limit");
        }
        return PasswordHash.of(encoder.encode(credential.rawValue()));
    }

    @Override
    public boolean verify(PasswordHash stored, SecretValue candidate) {
        return encoder.matches(candidate.rawValue(), stored.value());
    }

    @Override
    public boolean needsRehash(PasswordHash stored) {
        Matcher matcher = BCRYPT_COST.matcher(stored.value());
        return matcher.matches() && Integer.parseInt(matcher.group(1)) < COST;
    }
}
