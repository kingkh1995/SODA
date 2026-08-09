package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Mobile;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.VerificationCodePolicy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 短信认证账户实体 — 手机号 + 短信验证码方式的认证。
 * <p>
 * 与 User.mobile 联动：设置 User.mobile 时自动创建，清除时自动删除。
 * <p>
 * 只保留认证标识（手机号）与 active——验证码的发送/校验状态已迁移至独立的
 * {@link Verification} 实体（ADR-0011）；码形策略是通道级规则，账户不持有 policy 字段
 * （2026-08-09 移除，见 ADR-0018）。
 *
 * @see AuthAccount
 */
@JsonTypeName("S")
@EqualsAndHashCode(callSuper = true)
@Getter
public final class SmsAuthAccount extends AuthAccount<SmsAuthAccountId> {

    /**
     * 默认短信验证码策略：6 位纯数字，5 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_POLICY = VerificationCodePolicy.DEFAULT_SMS;

    // ─── construction ───

    /**
     * 全参数恢复构造器 — 持久化恢复与 JSON 反序列化唯一入口（{@link JsonCreator}）。
     * <p>
     * id 非空由 {@link AuthAccount}/{@link Entity} 构造器链路保证。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @Builder
    private SmsAuthAccount(
            @JsonProperty(value = "id", required = true) SmsAuthAccountId id,
            @JsonProperty(value = "active", required = true) Active active) {
        super(id, active);
    }

    // ─── factories ───

    /**
     * 创建新短信账户 — active 默认 TRUE，ID 从 mobile 派生。
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static SmsAuthAccount create(Mobile mobile) {
        return new SmsAuthAccount(SmsAuthAccountId.from(mobile), Active.TRUE);
    }

    // ─── accessors ───

    /**
     * 认证类型 — 常量来源为 {@link SmsAuthAccountId#ACCOUNT_TYPE}（与 ID 解耦，无 ID 亦可派发）。
     */
    @Override
    public AuthAccountType getAccountType() {
        return SmsAuthAccountId.ACCOUNT_TYPE;
    }

    public Mobile getMobile() {
        return getId().mobile();
    }
}
