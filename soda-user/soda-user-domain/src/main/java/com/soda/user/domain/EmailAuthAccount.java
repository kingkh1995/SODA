package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 邮箱认证账户实体 — 邮箱 + 验证码方式的认证。
 * <p>
 * 与 User.email 联动：设置 User.email 时自动创建，清除时自动删除。
 * <p>
 * 只保留认证标识（邮箱）与 active——验证码的发送/校验状态已迁移至独立的
 * {@link Verification} 实体（ADR-0011）；码形策略是通道级规则，账户不持有 policy 字段
 * （2026-08-09 移除，见 ADR-0018）。
 *
 * @see AuthAccount
 */
@JsonTypeName("E")
@EqualsAndHashCode(callSuper = true)
@Getter
public final class EmailAuthAccount extends AuthAccount<EmailAuthAccountId> {

    /**
     * 默认邮箱验证码策略：8 位字母数字，30 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_POLICY = VerificationCodePolicy.DEFAULT_EMAIL;

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
            @JsonProperty(value = "active", required = true) Active active) {
        super(id, active);
    }

    // ─── factories ───

    /**
     * 创建新邮箱账户 — active 默认 TRUE，ID 从 email 派生。
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static EmailAuthAccount create(Email email) {
        return new EmailAuthAccount(EmailAuthAccountId.from(email), Active.TRUE);
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
