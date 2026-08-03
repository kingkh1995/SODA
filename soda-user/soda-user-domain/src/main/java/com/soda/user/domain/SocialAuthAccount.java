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
 * 社交认证账户实体 — 第三方社交账号（Gitee/DingTalk/WeChat）方式的认证。
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
     * 持久化恢复 / JSON 反序列化。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private SocialAuthAccount(
            @JsonProperty("id") SocialAuthAccountId id,
            @JsonProperty("active") Active active) {
        super(id, active);
    }

    // ─── factories ───

    /**
     * 创建新社交账户 — active 默认 TRUE，ID 从 socialType + openId 派生。
     */
    @Builder(builderClassName = "SocialAuthAccountCreateBuilder",
            builderMethodName = "createBuilder")
    private static SocialAuthAccount create(SocialType socialType, String openId) {
        return new SocialAuthAccount(
                SocialAuthAccountId.from(socialType, openId),
                Active.TRUE
        );
    }

    /**
     * 从持久化恢复社交账户 — 全部字段显式传入。
     */
    @Builder(builderClassName = "SocialAuthAccountRestoreBuilder",
            builderMethodName = "restoreBuilder")
    private static SocialAuthAccount restore(SocialAuthAccountId id, Active active) {
        return new SocialAuthAccount(id, active);
    }

    // ─── accessors ───

    /**
     * 认证类型 — 常量来源为 {@link SocialAuthAccountId#ACCOUNT_TYPE}（与 ID 解耦，无 ID 亦可派发）。
     */
    @Override
    public AuthAccountType getAuthAccountType() {
        return SocialAuthAccountId.ACCOUNT_TYPE;
    }

    /**
     * 社交平台类型（@JsonIgnore：数据在 id 字段中，避免 JSON 属性冲突）。
     */
    public SocialType getSocialType() {
        return requireId().socialType();
    }

    /**
     * 社交平台用户开放 ID（数据在 id 字段中，避免 JSON 属性冲突）。
     */
    public String getOpenId() {
        return requireId().openId();
    }

}
