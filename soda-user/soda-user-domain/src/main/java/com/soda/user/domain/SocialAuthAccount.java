package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.types.Active;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SocialAuthAccountId;
import com.soda.user.domain.types.SocialType;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * 社交认证账户实体 — 第三方社交账号映射。
 * <p>
 * 纯标识映射，无密码验证。SocialType + openId 编码在 {@link SocialAuthAccountId} 中。
 *
 * @see AuthAccount
 */
@JsonTypeName("O")
@EqualsAndHashCode(callSuper = true)
public final class SocialAuthAccount extends AuthAccount<SocialAuthAccountId> {
    // ─── construction ───

    /**
     * 全参数恢复构造器 — 持久化恢复与 JSON 反序列化唯一入口（{@link JsonCreator}）。
     * <p>
     * id 非空由 {@link AuthAccount}/{@link Entity} 构造器链路保证。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @Builder
    private SocialAuthAccount(
            @JsonProperty(value = "id", required = true) SocialAuthAccountId id,
            @JsonProperty(value = "active", required = true) Active active) {
        super(id, active);
    }

    // ─── factories ───

    /**
     * 创建新社交账户 — active 默认 TRUE，ID 从 socialType + openId 派生。
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static SocialAuthAccount create(SocialType socialType, String openId) {
        return new SocialAuthAccount(
                SocialAuthAccountId.from(socialType, openId),
                Active.TRUE
        );
    }

    // ─── accessors ───

    @Override
    public AuthAccountType getAccountType() {
        return AuthAccountType.O;
    }

    public SocialType getSocialType() {
        return getId().socialType();
    }

    public String getOpenId() {
        return getId().openId();
    }
}
