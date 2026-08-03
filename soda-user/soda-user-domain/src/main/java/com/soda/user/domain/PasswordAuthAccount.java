package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.RawCredential;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.PasswordAuthAccountId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

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
    private PasswordAuthAccount(
            @JsonProperty("id") PasswordAuthAccountId id,
            @JsonProperty("active") Active active,
            @JsonProperty("passwordHash") CredentialHash passwordHash) {
        super(id, active);
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    /**
     * 创建新密码账户（无 ID，由 Gateway 后续填补）。
     */
    private PasswordAuthAccount(CredentialHash passwordHash) {
        super(Active.TRUE);
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    // ─── factories ───

    /**
     * 创建新密码账户 — active 默认 TRUE，ID 由 Gateway 在持久化后通过 {@link #assignId} 补充。
     */
    @Builder(builderClassName = "PasswordAuthAccountCreateBuilder",
            builderMethodName = "createBuilder")
    private static PasswordAuthAccount create(CredentialHash passwordHash) {
        return new PasswordAuthAccount(passwordHash);
    }

    /**
     * 从持久化恢复密码账户 — 全部字段显式传入。
     */
    @Builder(builderClassName = "PasswordAuthAccountRestoreBuilder",
            builderMethodName = "restoreBuilder")
    private static PasswordAuthAccount restore(PasswordAuthAccountId id, Active active, CredentialHash passwordHash) {
        return new PasswordAuthAccount(id, active, passwordHash);
    }

    // ─── accessors ───

    /**
     * 认证类型 — 常量来源为 {@link PasswordAuthAccountId#ACCOUNT_TYPE}（与 ID 解耦，无 ID 亦可派发）。
     */
    @Override
    public AuthAccountType getAuthAccountType() {
        return PasswordAuthAccountId.ACCOUNT_TYPE;
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
        Objects.requireNonNull(credential);
        Objects.requireNonNull(hasher);
        return hasher.matches(credential, passwordHash);
    }

    // ─── commands ───

    /**
     * 修改密码。
     *
     * @param credential 新原始凭证
     * @param hasher     凭证哈希器
     * @implNote 不在此处注册 PasswordChangedEvent（泛型 ID 不匹配，事件需 {@code UserId}）；
     * 事件由 {@code User.changePassword} 在调用本方法后注册。
     */
    public void changePassword(RawCredential credential, CredentialHasher hasher) {
        Objects.requireNonNull(credential);
        Objects.requireNonNull(hasher);
        this.passwordHash = hasher.hash(credential);
    }

}
