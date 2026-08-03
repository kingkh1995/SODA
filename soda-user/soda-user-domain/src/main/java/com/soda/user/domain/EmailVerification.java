package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.PositiveInt;
import com.soda.component.domain.types.UUId;
import com.soda.component.domain.types.VerificationChannel;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Objects;

/**
 * 邮箱验证实体 — 目标为 {@link Email}。
 * <p>
 * 与 {@link SmsVerification} 的唯一差异是 target 类型与 {@code @JsonTypeName} 标识，
 * 其余状态与行为见 {@link Verification}。
 *
 * @see Verification
 * @see SmsVerification
 */
@JsonTypeName("E")
@EqualsAndHashCode(callSuper = true)
public final class EmailVerification extends Verification<Email> {

    // ─── construction ───

    /**
     * 持久化恢复 / JSON 反序列化。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private EmailVerification(
            @JsonProperty("id") UUId id,
            @JsonProperty("scene") VerificationScene scene,
            @JsonProperty("status") VerificationStatus status,
            @JsonProperty("code") VerificationCode code,
            @JsonProperty("policy") VerificationCodePolicy policy,
            @JsonProperty("target") Email target,
            @JsonProperty("userId") LongId userId) {
        super(id, scene, status, code, policy, target, userId);
    }

    // ─── factories ───

    /**
     * 创建邮箱验证实体（创建路径，ID 创建时生成）。
     */
    @Builder(builderClassName = "EmailVerificationCreateBuilder",
            builderMethodName = "createBuilder")
    private static EmailVerification create(
            LongId userId,
            VerificationScene scene,
            Email target,
            VerificationCodePolicy policy,
            RandomStringGenerator generator) {
        var verificationCode = VerificationCode.from(Objects.requireNonNull(generator).generate(PositiveInt.of(Objects.requireNonNull(policy).codeLength())), Instant.now().plus(policy.expiry()));
        return new EmailVerification(UUId.random(), scene, VerificationStatus.P, verificationCode, policy, target, userId);
    }

    /**
     * 从持久化恢复邮箱验证实体 — 全部字段显式传入。
     */
    @Builder(builderClassName = "EmailVerificationRestoreBuilder",
            builderMethodName = "restoreBuilder")
    private static EmailVerification restore(
            UUId id,
            VerificationScene scene,
            VerificationStatus status,
            VerificationCode code,
            VerificationCodePolicy policy,
            Email target,
            LongId userId) {
        return new EmailVerification(id, scene, status, code, policy, target, userId);
    }

    // ─── accessors ───

    /**
     * 验证渠道 — 与 {@code @JsonTypeName} 判别值一致。
     */
    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.E;
    }
}
