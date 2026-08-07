package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * 邮箱认证账户实体 — 邮箱 + 验证码方式的认证。
 * <p>
 * 与 User.email 联动：设置 User.email 时自动创建，清除时自动删除。
 * 只保留认证标识（邮箱）和策略配置（{@link VerificationCodePolicy}），
 * 验证码的发送/校验状态已迁移至独立的 {@link Verification} 实体。
 *
 * @see AuthAccount
 */
@JsonTypeName("E")
@EqualsAndHashCode(callSuper = true)
@Getter
public final class EmailAuthAccount extends AuthAccount<EmailAuthAccountId> {

    /**
     * 默认邮箱验证码策略：8 位，30 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_POLICY = VerificationCodePolicy.DEFAULT_EMAIL;

    /**
     * 当前生效的策略。
     */
    private VerificationCodePolicy verificationCodePolicy;

    // ─── construction ───

    /**
     * 全参数恢复构造器 — 持久化恢复与 JSON 反序列化唯一入口（{@link JsonCreator}）。
     * <p>
     * id 非空由 {@link AuthAccount}/{@link Entity} 构造器链路保证。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @Builder
    private EmailAuthAccount(
            @JsonProperty(value = "id", required = true) EmailAuthAccountId id,
            @JsonProperty(value = "active", required = true) Active active,
            @JsonProperty(value = "verificationCodePolicy", required = true) VerificationCodePolicy verificationCodePolicy) {
        super(id, active);
        ValidateUtils.notNull(verificationCodePolicy);
        this.verificationCodePolicy = verificationCodePolicy;
    }

    // ─── factories ───

    /**
     * 创建新邮箱账户 — active 默认 TRUE，ID 从 email 派生。
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static EmailAuthAccount create(Email email, @Nullable VerificationCodePolicy verificationCodePolicy) {
        return new EmailAuthAccount(
                EmailAuthAccountId.from(email),
                Active.TRUE, Objects.requireNonNullElse(verificationCodePolicy, DEFAULT_POLICY)
        );
    }

    // ─── accessors ───

    /**
     * 认证类型 — 常量来源为 {@link EmailAuthAccountId#ACCOUNT_TYPE}（与 ID 解耦，无 ID 亦可派发）。
     */
    @Override
    public AuthAccountType getAccountType() {
        return EmailAuthAccountId.ACCOUNT_TYPE;
    }

    /**
     * 从 ID 中提取邮箱（@JsonIgnore：数据在 id 字段中，避免 JSON 属性冲突）。
     */
    public Email getEmail() {
        return getId().email();
    }
}
