package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.Mobile;
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
 * 短信验证实体 — 目标为 {@link Mobile}。
 * <p>
 * 与 {@link EmailVerification} 的唯一差异是 target 类型与 {@code @JsonTypeName} 标识，
 * 其余状态与行为见 {@link Verification}。
 *
 * @see Verification
 * @see EmailVerification
 */
@JsonTypeName("S")
@EqualsAndHashCode(callSuper = true)
public final class SmsVerification extends Verification<Mobile> {

    // ─── construction ───

    /**
     * 持久化恢复 / JSON 反序列化。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    protected SmsVerification(
            @JsonProperty("id") UUId id,
            @JsonProperty("scene") VerificationScene scene,
            @JsonProperty("status") VerificationStatus status,
            @JsonProperty("code") VerificationCode code,
            @JsonProperty("policy") VerificationCodePolicy policy,
            @JsonProperty("target") Mobile target,
            @JsonProperty("userId") LongId userId) {
        super(id, scene, status, code, policy, target, userId);
    }

    // ─── factories ───

    /**
     * 创建短信验证实体（创建路径，ID 创建时生成）。
     */
    @Builder(builderClassName = "SmsVerificationCreateBuilder",
            builderMethodName = "createBuilder")
    private static SmsVerification create(
            LongId userId,
            VerificationScene scene,
            Mobile target,
            VerificationCodePolicy policy,
            RandomStringGenerator generator) {
        var verificationCode = VerificationCode.from(Objects.requireNonNull(generator).generate(PositiveInt.of(Objects.requireNonNull(policy).codeLength())), Instant.now().plus(policy.expiry()));
        return new SmsVerification(UUId.random(), scene, VerificationStatus.P, verificationCode, policy, target, userId);
    }

    /**
     * 从持久化恢复短信验证实体 — 全部字段显式传入。
     */
    @Builder(builderClassName = "SmsVerificationRestoreBuilder",
            builderMethodName = "restoreBuilder")
    private static SmsVerification restore(
            UUId id,
            VerificationScene scene,
            VerificationStatus status,
            VerificationCode code,
            VerificationCodePolicy policy,
            Mobile target,
            LongId userId) {
        return new SmsVerification(id, scene, status, code, policy, target, userId);
    }

    // ─── accessors ───

    /**
     * 验证渠道 — 与 {@code @JsonTypeName} 判别值一致。
     */
    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.S;
    }
}
