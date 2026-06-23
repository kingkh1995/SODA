package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.support.gateway.PasswordEncoder;
import com.soda.component.support.types.Active;
import com.soda.component.support.types.PasswordHash;
import com.soda.component.support.types.RawPassword;
import lombok.Builder;
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
public final class PasswordAuthAccount extends AuthAccount<PasswordAuthAccountId> {

    private PasswordHash passwordHash;

    // ─── construction ───

    /** 持久化恢复 / JSON 反序列化。 */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    protected PasswordAuthAccount(
            @JsonProperty("id") PasswordAuthAccountId id,
            @JsonProperty("active") Active active,
            @JsonProperty("passwordHash") PasswordHash passwordHash) {
        super(id, active);
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    // ─── factories ───

    /** 创建新密码账户 — active 默认 TRUE，ID 从 userId 派生。 */
    @Builder(builderClassName = "PasswordAuthAccountCreateBuilder",
             builderMethodName = "createBuilder")
    public static PasswordAuthAccount create(UserId userId, PasswordHash passwordHash) {
        return new PasswordAuthAccount(
                PasswordAuthAccountId.from(userId),
                Active.TRUE,
                passwordHash
        );
    }

    /** 从持久化恢复密码账户 — 全部字段显式传入。 */
    @Builder(builderClassName = "PasswordAuthAccountRestoreBuilder",
             builderMethodName = "restoreBuilder")
    public static PasswordAuthAccount restore(PasswordAuthAccountId id, Active active, PasswordHash passwordHash) {
        return new PasswordAuthAccount(id, active, passwordHash);
    }

    // ─── queries ───

    /**
     * 校验原始密码是否匹配当前哈希。
     *
     * @param rawPassword 原始密码
     * @param encoder     密码编码器
     * @return true 若匹配
     */
    public boolean verify(RawPassword rawPassword, PasswordEncoder encoder) {
        Objects.requireNonNull(rawPassword);
        Objects.requireNonNull(encoder);
        return encoder.matches(rawPassword, passwordHash);
    }

    // ─── commands ───

    /**
     * 修改密码。
     *
     * @param rawPassword 新原始密码
     * @param encoder     密码编码器
     */
    public void changePassword(RawPassword rawPassword, PasswordEncoder encoder) {
        Objects.requireNonNull(rawPassword);
        Objects.requireNonNull(encoder);
        this.passwordHash = encoder.encode(rawPassword);
    }
}
