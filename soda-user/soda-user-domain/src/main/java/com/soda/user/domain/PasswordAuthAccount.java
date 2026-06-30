package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.support.gateway.CredentialHasher;
import com.soda.component.support.types.Active;
import com.soda.component.support.types.CredentialHash;
import com.soda.component.support.types.RawCredential;
import lombok.Builder;
import lombok.Getter;
import lombok.EqualsAndHashCode;

import org.springframework.util.Assert;

/**
 * 密码认证账户实体 — 用户名 + 密码方式的认证。
 * <p>
 * 每个 User 有且仅有一个 PasswordAuthAccount，在 User 创建时自动生成。
 *
 * @see AuthAccount
 */
@Getter
@JsonTypeName("P")
@EqualsAndHashCode(callSuper = true)
public final class PasswordAuthAccount extends AuthAccount<PasswordAuthAccountId> {

    private CredentialHash passwordHash;

    // ─── construction ───

    /**
     * 持久化恢复 / JSON 反序列化。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    protected PasswordAuthAccount(
            @JsonProperty("id") PasswordAuthAccountId id,
            @JsonProperty("active") Active active,
            @JsonProperty("passwordHash") CredentialHash passwordHash) {
        super(id, active);
        Assert.notNull(passwordHash, "passwordHash must not be null");
        this.passwordHash = passwordHash;
    }

    // ─── factories ───

    /**
     * 创建新密码账户 — active 默认 TRUE，ID 从 userId 派生。
     */
    @Builder(builderClassName = "PasswordAuthAccountCreateBuilder",
            builderMethodName = "createBuilder")
    public static PasswordAuthAccount create(UserId userId, CredentialHash passwordHash) {
        return new PasswordAuthAccount(
                PasswordAuthAccountId.from(userId),
                Active.TRUE,
                passwordHash
        );
    }

    /**
     * 从持久化恢复密码账户 — 全部字段显式传入。
     */
    @Builder(builderClassName = "PasswordAuthAccountRestoreBuilder",
            builderMethodName = "restoreBuilder")
    public static PasswordAuthAccount restore(PasswordAuthAccountId id, Active active, CredentialHash passwordHash) {
        return new PasswordAuthAccount(id, active, passwordHash);
    }

    // ─── queries ───

    /**
     * 校验原始凭证是否匹配当前哈希。
     *
     * @param credential 原始凭证
     * @param hasher     凭证哈希器
     * @return true 若匹配
     */
    public boolean verify(RawCredential credential, CredentialHasher hasher) {
        Assert.notNull(credential, "credential must not be null");
        Assert.notNull(hasher, "hasher must not be null");
        return hasher.matches(credential, passwordHash);
    }

    // ─── commands ───

    /**
     * 修改密码。
     *
     * @param credential 新原始凭证
     * @param hasher     凭证哈希器
     */
    public void changePassword(RawCredential credential, CredentialHasher hasher) {
        Assert.notNull(credential, "credential must not be null");
        Assert.notNull(hasher, "hasher must not be null");
        this.passwordHash = hasher.hash(credential);
    }
}
