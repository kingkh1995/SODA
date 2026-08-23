package com.soda.component.domain.gateway;

import com.soda.component.domain.Gateway;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.SecretValue;

/**
 * 口令哈希器 Gateway —— 口令验证场景（ADR-0033）。
 * <p>
 * 慢 KDF（bcrypt/argon2id 等）与成本参数由基础设施配置决定，领域层零算法词汇。
 * 输入统一为 {@link SecretValue} 瞬态秘密载体（不落 StringLiteralType、防序列化）。
 * 实现不得保留、缓存或日志 {@link SecretValue} 明文——仅限当前调用栈内使用，返回后即弃。
 *
 * @see Gateway
 * @see PasswordHash
 * @see SecretValue
 */
public interface PasswordHasher extends Gateway {

    /**
     * 对原始口令做慢 KDF 哈希（注册 / 改密 / 登录透明升级路径）。
     */
    PasswordHash hash(SecretValue credential);

    /**
     * 校验候选口令是否与存储哈希匹配（登录路径，恒定时间比较由实现保证）。
     */
    boolean verify(PasswordHash stored, SecretValue candidate);

    /**
     * 判断存储哈希是否应按当前配置重编码（登录透明升级——verify 通过后消费，
     * 为真则以候选凭证 {@link #hash} 重哈希并由调用方持久化）。
     * 纯判断、无 IO；非本实现产出的哈希形态返回 false（不评估、不阻断验证路径）。
     */
    boolean needsRehash(PasswordHash stored);
}
