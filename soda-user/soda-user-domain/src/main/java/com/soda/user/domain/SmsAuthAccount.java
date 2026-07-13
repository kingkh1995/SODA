package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 短信认证账户实体 — 手机号 + 短信验证码方式的认证。
 * <p>
 * 与 User.mobile 联动：设置 User.mobile 时自动创建，清除时自动删除。
 *
 * @see AuthAccount
 */
@JsonTypeName("S")
@EqualsAndHashCode(callSuper = true)
public final class SmsAuthAccount extends AuthAccount<SmsAuthAccountId> {

    /**
     * 默认短信验证码策略：6 位，5 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_POLICY = VerificationCodePolicy.DEFAULT_SMS;
    private @Nullable VerificationCode verificationCode;

    private @Nullable VerificationCodePolicy verificationCodePolicy;

    // ─── construction ───

    /**
     * 持久化恢复 / JSON 反序列化。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    protected SmsAuthAccount(
            @JsonProperty("id") SmsAuthAccountId id,
            @JsonProperty("active") Active active,
            @JsonProperty("verificationCode") @Nullable VerificationCode verificationCode,
            @JsonProperty("verificationCodePolicy") @Nullable VerificationCodePolicy verificationCodePolicy) {
        super(id, active);
        this.verificationCode = verificationCode;
        this.verificationCodePolicy = verificationCodePolicy;
    }

    // ─── factories ───

    /**
     * 创建新短信账户 — active 默认 TRUE，ID 从 mobile 派生。验证码通过 replaceCode() 后续注入。
     */
    @Builder(builderClassName = "SmsAuthAccountCreateBuilder",
            builderMethodName = "createBuilder")
    public static SmsAuthAccount create(Mobile mobile) {
        return new SmsAuthAccount(
                SmsAuthAccountId.from(mobile),
                Active.TRUE,
                null,
                null
        );
    }

    /**
     * 从持久化恢复短信账户 — 全部字段显式传入。
     */
    @Builder(builderClassName = "SmsAuthAccountRestoreBuilder",
            builderMethodName = "restoreBuilder")
    public static SmsAuthAccount restore(
            SmsAuthAccountId id, Active active,
            @Nullable VerificationCode verificationCode,
            @Nullable VerificationCodePolicy verificationCodePolicy) {
        return new SmsAuthAccount(id, active, verificationCode, verificationCodePolicy);
    }

    // ─── policy ───

    /**
     * 当前生效的策略。
     */
    public Optional<VerificationCode> getVerificationCode() {
        return Optional.ofNullable(verificationCode);
    }

    /**
     * 当前生效的策略。
     */
    public VerificationCodePolicy getVerificationCodePolicy() {
        return verificationCodePolicy != null ? verificationCodePolicy : DEFAULT_POLICY;
    }

    // ─── verification ───

    /**
     * 校验验证码。
     *
     * @param inputCode 待校验的验证码
     * @return true 若校验通过
     */
    public boolean verifyCode(RandomString inputCode) {
        return verificationCode != null && verificationCode.verify(inputCode);
    }

    // ─── code lifecycle ───

    /**
     * 注入验证码（替换已有）。
     *
     * @param code 新验证码，非 null
     * @return true 替换成功；false 新码已过期
     */
    public boolean replaceCode(VerificationCode code) {
        ValidateUtils.notNull(code);
        if (code.expired()) {
            return false;
        }
        this.verificationCode = code;
        return true;
    }

    /**
     * 使用验证码（标记为已使用）。
     * 无验证码时无操作。
     */
    public void useCode() {
        if (verificationCode != null) {
            verificationCode = verificationCode.use();
        }
    }

    // ─── mobile ───

    public Mobile getMobile() {
        return getId().mobile();
    }
}
