package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.gateway.SmsSender;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.SmsContent;
import com.soda.component.domain.types.UUId;
import com.soda.user.domain.types.VerificationChannel;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

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

    /**
     * 默认短信验证码策略：6 位，5 分钟过期。
     */
    public static final VerificationCodePolicy DEFAULT_POLICY = VerificationCodePolicy.DEFAULT_SMS;

    // ─── construction ───

    /**
     * 全参数恢复构造器 — 持久化恢复与 JSON 反序列化唯一入口（{@link JsonCreator}）。
     * <p>
     * id 必填由 JSON schema 声明（{@code required = true}），缺 id 由 Jackson 在协议边界拒绝。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @Builder
    private SmsVerification(
            @JsonProperty(value = "id", required = true) UUId id,
            @JsonProperty(value = "scene", required = true) VerificationScene scene,
            @JsonProperty(value = "status", required = true) VerificationStatus status,
            @JsonProperty(value = "code", required = true) VerificationCode code,
            @JsonProperty(value = "target", required = true) Mobile target,
            @JsonProperty(value = "userId", required = true) LongId userId) {
        super(id, scene, status, code, target, userId);
    }

    // ─── factories ───

    /**
     * 创建短信验证实体（创建路径，ID 创建时生成）。
     * <p>
     * {@code policy} 可不传（默认 {@link #DEFAULT_POLICY}），决定码长、过期时间与字符集，
     * 效果物化进 {@link VerificationCode}，不落验证实体。
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static SmsVerification create(
            LongId userId,
            VerificationScene scene,
            Mobile target,
            RandomStringGenerator generator,
            @Nullable VerificationCodePolicy policy) {
        var effectivePolicy = Objects.requireNonNullElse(policy, DEFAULT_POLICY);
        var verificationCode = VerificationCode.from(
                generator.generate(effectivePolicy.codeLength(), effectivePolicy.codeAlphabet()),
                Instant.now().plus(effectivePolicy.expiry()));
        return new SmsVerification(UUId.random(), scene, VerificationStatus.I, verificationCode, target, userId);
    }

    // ─── accessors ───

    /**
     * 验证渠道 — 与 {@code @JsonTypeName} 判别值一致。
     */
    @Override
    public VerificationChannel getChannel() {
        return VerificationChannel.S;
    }

    // ─── commands ───

    /**
     * 通过 {@link SmsSender} 发送验证码短信；发送成功后状态从 INITIALIZED 变为 PENDING。
     * <p>
     * sender 以参数注入（领域层不持有 gateway 端口）；发送失败（异常）时状态保持 INITIALIZED。
     *
     * @throws IllegalArgumentException 状态非 INITIALIZED（业务状态前置）
     */
    public void send(SmsSender sender) {
        Assert.isTrue(isInitialized(), "verification must be initialized before sending");
        sender.send(getTarget(), new SmsContent("您的验证码: " + getCode().code().value()));
        markPending();
    }
}
