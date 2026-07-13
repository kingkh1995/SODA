package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 邮箱认证账户实体 — 邮箱 + 验证码方式的认证。
 * <p>
 * 与 User.email 联动：设置 User.email 时自动创建，清除时自动删除。
 * 验证码通过 {@link #replaceCode(VerificationCode)} 注入，外部调用方负责实际发送。
 *
 * @see AuthAccount
 */
@JsonTypeName("E")
@EqualsAndHashCode(callSuper = true)
public final class EmailAuthAccount extends AuthAccount<EmailAuthAccountId> {

    /**
     * 默认邮箱验证码策略：8 位，30 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_POLICY = VerificationCodePolicy.DEFAULT_EMAIL;

    private @Nullable VerificationCode verificationCode;

    private @Nullable VerificationCodePolicy verificationCodePolicy;

    // ─── construction ───

    /**
     * 持久化恢复 / JSON 反序列化。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    protected EmailAuthAccount(
            @JsonProperty("id") EmailAuthAccountId id,
            @JsonProperty("active") Active active,
            @JsonProperty("verificationCode") @Nullable VerificationCode verificationCode,
            @JsonProperty("verificationCodePolicy") @Nullable VerificationCodePolicy verificationCodePolicy) {
        super(id, active);
        this.verificationCode = verificationCode;
        this.verificationCodePolicy = verificationCodePolicy;
    }

    // ─── factories ───

    /**
     * 创建新邮箱账户 — active 默认 TRUE，ID 从 email 派生。验证码通过 replaceCode() 后续注入。
     */
    @Builder(builderClassName = "EmailAuthAccountCreateBuilder",
            builderMethodName = "createBuilder")
    public static EmailAuthAccount create(Email email) {
        return new EmailAuthAccount(
                EmailAuthAccountId.from(email),
                Active.TRUE,
                null,
                null
        );
    }

    /**
     * 从持久化恢复邮箱账户 — 全部字段显式传入。
     */
    @Builder(builderClassName = "EmailAuthAccountRestoreBuilder",
            builderMethodName = "restoreBuilder")
    public static EmailAuthAccount restore(
            EmailAuthAccountId id, Active active,
            @Nullable VerificationCode verificationCode,
            @Nullable VerificationCodePolicy verificationCodePolicy) {
        return new EmailAuthAccount(id, active, verificationCode, verificationCodePolicy);
    }

    // ─── queries ───

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

    // ─── email ───

    /**
     * 从 ID 中提取邮箱（@JsonIgnore：数据在 id 字段中，避免 JSON 属性冲突）。
     */
    public Email getEmail() {
        return getId().email();
    }
}
