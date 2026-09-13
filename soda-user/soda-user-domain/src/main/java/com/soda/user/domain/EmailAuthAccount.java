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
 * 邮箱认证账户实体 — 以邮箱为认证标识。
 * <p>
 * 与 User.email 联动——字段与账户同步替换，恒一致。
 * <p>
 * 只持有认证标识与 active：验证码状态由独立的 {@link Verification} 聚合管理（见 ADR-0011）；
 * 码形是通道级规则，账户不持有 policy 字段（见 ADR-0018）。
 *
 * @see AuthAccount
 */
@JsonTypeName("E")
@EqualsAndHashCode(callSuper = true)
@Getter
public final class EmailAuthAccount extends AuthAccount<EmailAuthAccountId> {

    /**
     * 通道默认码形常量（预留契约——码形由场景策略决定，非账户数据，见 ADR-0018）。
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

    @Override
    public AuthAccountType getAccountType() {
        return AuthAccountType.E;
    }

    public Email getEmail() {
        return getId().email();
    }
}
