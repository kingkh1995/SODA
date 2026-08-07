package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soda.component.domain.Entity;
import com.soda.component.domain.Type;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.LongId;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.UUId;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.types.VerificationChannel;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationScene;
import com.soda.user.domain.types.VerificationStatus;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * 验证抽象基类 — 独立验证实体。
 * <p>
 * 密封类，仅允许 {@link SmsVerification}、{@link EmailVerification} 两个子类。
 * 两个子类的唯一差异是验证目标（{@code target}）类型：{@link SmsVerification} 目标为
 * {@link Mobile}，{@link EmailVerification} 目标为 {@link Email}。因此 {@code target}
 * 以类型参数 {@code T extends Type} 收敛到本基类，子类仅保留类型差异与
 * {@code @JsonTypeName} 标识。
 * <p>
 * 封装验证码的完整生命周期与业务不变量：
 * <ul>
 *   <li>场景 ({@link VerificationScene})</li>
 *   <li>状态 ({@link VerificationStatus})</li>
 *   <li>验证码 ({@link VerificationCode}) — 内含过期时间</li>
 *   <li>目标 ({@code T}) 与关联用户 ({@link LongId})</li>
 * </ul>
 * <p>
 * 策略 ({@link VerificationCodePolicy}) 仅是创建时的输入参数（create 构造末位可空参数，
 * 缺省取子类静态 {@code DEFAULT_POLICY}；决定码长与过期时间），效果已物化进
 * {@link VerificationCode}，不作为聚合属性持久化。
 * <p>
 * <b>新增子类提醒</b>：{@code permits} 子句 + 新增类声明后，在新增类上添加 {@code @JsonTypeName} 注解指定类型标识；
 * 同步补充 {@link VerificationChannel} 枚举常量与一致性测试（ADR-0016）。
 * Jackson 3 从密封类 {@code permits} 子句自动发现子类，无需 {@code @JsonSubTypes}。
 * <p>
 * Jackson 序列化说明：{@link Entity 基类} 声明了 {@code @JsonAutoDetect(getterVisibility = NONE)}，
 * 因此序列化走字段可见性（field visibility ANY），反序列化走 {@code @JsonCreator(mode = Mode.PROPERTIES)} 构造器
 * + {@code @JsonProperty} 参数。
 * <p>
 * 状态跃迁：INITIALIZED → PENDING（发送验证码后）→ VERIFIED → USED，或任一步过期。
 * 采用可变命令风格，与 {@link AuthAccount} 一致。
 *
 * @param <T> 验证目标类型（{@link Mobile} 或 {@link Email}）
 * @see SmsVerification
 * @see EmailVerification
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "channel")
@EqualsAndHashCode(callSuper = true)
@Getter
public abstract sealed class Verification<T extends Type> extends Entity<UUId>
        permits SmsVerification, EmailVerification {

    private VerificationScene scene;
    private VerificationStatus status;
    private VerificationCode code;
    private T target;
    private LongId userId;

    // ─── construction ───

    /**
     * 手动设置 / 已有数据恢复（reconstitution）。
     */
    protected Verification(UUId id,
                           VerificationScene scene,
                           VerificationStatus status,
                           VerificationCode code,
                           T target,
                           LongId userId) {
        super(id);
        ValidateUtils.notNull(scene);
        ValidateUtils.notNull(status);
        ValidateUtils.notNull(code);
        ValidateUtils.notNull(target);
        ValidateUtils.notNull(userId);
        this.scene = scene;
        this.status = status;
        this.code = code;
        this.target = target;
        this.userId = userId;
    }

    // ─── queries ───

    /**
     * 验证渠道 — 与子类 {@code @JsonTypeName} 判别值一致，构成 JSON 的 {@code channel} 属性。
     * 与 {@link AuthAccount#getAccountType()} 同构。
     */
    public abstract VerificationChannel getChannel();

    // ─── state checks ───

    /**
     * 判断是否处于初始化状态（已创建、验证码尚未发送）。
     */
    public boolean isInitialized() {
        return VerificationStatus.I.equals(status);
    }

    /**
     * 判断是否处于待验证状态。
     */
    public boolean isPending() {
        return VerificationStatus.P.equals(status);
    }

    /**
     * 判断是否已验证通过（含已使用状态）。
     */
    public boolean isVerified() {
        return VerificationStatus.V.equals(status) || VerificationStatus.U.equals(status);
    }

    /**
     * 在指定时刻判断是否已过期（纯函数，无隐式时钟依赖）。
     */
    public boolean isExpiredAt(Instant at) {
        return code.expiredAt(at);
    }

    // ─── commands (mutable) ───

    /**
     * 在指定时刻验证输入码（同时判断过期与匹配）。
     * <p>
     * 业务规则：
     * <ul>
     *   <li>仅待验证状态 (PENDING) 可调用</li>
     *   <li>验证码在 {@code at} 时刻未过期</li>
     *   <li>输入码匹配</li>
     * </ul>
     * 验证通过 → 状态变更为 VERIFIED；失败 → 抛异常。
     *
     * @param at        校验时刻（应用层传入当前时间，测试可注入固定时刻）
     * @param inputCode 用户输入的验证码
     * @throws IllegalArgumentException 状态非待验证、已过期、验证码不匹配（业务参数校验）
     */
    public void verify(Instant at, RandomString inputCode) {
        Assert.isTrue(isPending(), "verification must be pending");
        Assert.isTrue(!isExpiredAt(at), "verification code must not be expired");
        Assert.isTrue(code.matches(inputCode), "Invalid verification code");
        this.status = VerificationStatus.V;
    }

    /**
     * 标记为已发送 — 状态从 INITIALIZED 变为 PENDING。
     * <p>
     * 由子类 {@code send(sender)} 在发送动作成功完成后调用；仅允许从 INITIALIZED 状态转移。
     *
     * @throws IllegalArgumentException 当前状态不是 INITIALIZED（业务状态前置）
     */
    protected void markPending() {
        Assert.isTrue(VerificationStatus.I.equals(status), "verification must be initialized before sending");
        this.status = VerificationStatus.P;
    }

    /**
     * 标记为已使用。
     * <p>
     * 仅允许从 VERIFIED 状态转为 USED。
     *
     * @throws IllegalArgumentException 当前状态不是 VERIFIED（业务参数校验）
     */
    public void use() {
        Assert.isTrue(VerificationStatus.V.equals(status), "verification must be verified");
        this.status = VerificationStatus.U;
    }
}
