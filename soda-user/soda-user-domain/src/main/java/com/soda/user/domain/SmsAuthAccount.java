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
 * 短信认证账户实体 — 以手机号为认证标识。
 * <p>
 * 与 User.mobile 联动——字段与账户同步替换，恒一致。
 * <p>
 * 只持有认证标识与 active：验证码状态由独立的 {@link Verification} 聚合管理（见 ADR-0011）；
 * 码形是通道级规则，账户不持有 policy 字段（见 ADR-0018）。
 *
 * @see AuthAccount
 */
@JsonTypeName("S")
@EqualsAndHashCode(callSuper = true)
@Getter
public final class SmsAuthAccount extends AuthAccount<SmsAuthAccountId> {

    /**
     * 通道默认码形常量（预留契约——码形由场景策略决定，非账户数据，见 ADR-0018）。
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

    @Override
    public AuthAccountType getAccountType() {
        return SmsAuthAccountId.ACCOUNT_TYPE;
    }

    public Mobile getMobile() {
        return getId().mobile();
    }
}
