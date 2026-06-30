package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.support.types.Active;
import com.soda.user.domain.enums.SocialType;
import lombok.EqualsAndHashCode;
import lombok.Builder;

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
    protected SocialAuthAccount(
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
    public static SocialAuthAccount create(SocialType socialType, String openId) {
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
    public static SocialAuthAccount restore(SocialAuthAccountId id, Active active) {
        return new SocialAuthAccount(id, active);
    }

    /**
     * 社交平台类型（@JsonIgnore：数据在 id 字段中，避免 JSON 属性冲突）。
     */
    public SocialType getSocialType() {
        return getId().socialType();
    }

    /**
     * 社交平台用户开放 ID（数据在 id 字段中，避免 JSON 属性冲突）。
     */
    public String getOpenId() {
        return getId().openId();
    }

}
