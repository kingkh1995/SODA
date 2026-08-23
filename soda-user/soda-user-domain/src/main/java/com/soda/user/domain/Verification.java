package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.Aggregate;
import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.RandomString;
import com.soda.component.domain.types.Uuid;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.event.VerificationCreatedEvent;
import com.soda.user.domain.types.VerificationCode;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationRecipient;
import com.soda.user.domain.types.VerificationSource;
import com.soda.user.domain.types.VerificationState;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * 验证聚合根 — 单类（2026-08-15 多态塌缩，见 ADR-0025；2026-08-12 由「Entity 非聚合根」
 * 重分类为聚合根，见 ADR-0021）。
 * <p>
 * <b>双概念模型</b>（2026-08-16，见 ADR-0026）：只感知 {@code source}（{@link VerificationSource}
 * ——<b>不透明</b>，scene/subject 纯字符串，非空即足，零行为耦合）与 {@code recipient}
 * （{@link VerificationRecipient}——泛型多态密封，channel + 类型化地址，<b>通道判别栖身于此</b>）。
 * <p>
 * <b>载体化</b>（见 ADR-0026）：领域内无任何 channel 行为——{@code verify}/{@code use}/
 * {@code isExpiredAt} 全通道同构；策略（{@link VerificationCodePolicy}）由调用方（工厂）显式传入，
 * 聚合不做通道→策略映射；send 投递是投递侧职责（监听器按 recipient 类型匹配 sender）。
 * 主体裁定：发送验证码的主体是用户（UCC/ULG/UPR；URG 无用户例外），Verification 是协助方聚合
 * （码生命周期载体），「UserVerification」是用例概念（非类，见 ADR-0026）。
 * <p>
 * 封装验证码的完整生命周期与业务不变量：
 * <ul>
 *   <li>请求源 ({@link VerificationSource}，<b>恒非空</b>——非空即 Verification 对 source 的唯一要求)</li>
 *   <li>状态 ({@link VerificationState})</li>
 *   <li>验证码 ({@link VerificationCode}) — 内含过期时间</li>
 *   <li>投递端点 ({@link VerificationRecipient}，泛型多态密封，构造边界校验)</li>
 * </ul>
 * <p>
 * 策略 ({@link VerificationCodePolicy}) 是创建时的<b>必传</b>输入（默认值由调用方工厂按
 * <b>场景方法</b>选择——UCC 专属策略：纯数字、短时效、双通道统一，见 ADR-0026；决定码长、
 * 过期时间与字符集），效果物化进 {@link VerificationCode}，不作为聚合属性持久化。
 * <p>
 * 状态跃迁：INITIALIZED → PENDING（{@link #markSent()}，投递成功由监听器调用）→ VERIFIED
 * → USED，或任一步过期。VERIFIED 为内存瞬态（消费流 verify→use 同事务完成，永不落库，
 * 见 {@code CredentialChangeDomainService}；{@link VerificationState} 终态化）。采用可变命令风格，
 * 与 {@link AuthAccount} 一致。
 * <p>
 * 投递：物理发送由投递侧监听器（AFTER_COMMIT）按 {@code recipient} 模式匹配 sender 执行
 * （{@code SmsRecipient}→SmsSender、{@code EmailRecipient}→EmailSender），成功后调用
 * {@link #markSent()} 并落库 PENDING（「PENDING 蕴含已送达」不变量，见 ADR-0011）。
 *
 * @see VerificationSource
 * @see VerificationRecipient
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public final class Verification extends Aggregate<Uuid> {

    private VerificationSource source;
    private VerificationState state;
    private VerificationCode code;
    private VerificationRecipient<?> recipient;

    // ─── construction ───

    /**
     * 全参数恢复构造器 — 持久化恢复（convertor）与 JSON 反序列化唯一入口。
     * <p>
     * source 恒必填（Verification 对 source 的唯一要求：非空——不解析 scene/subject 语义）。
     * 恢复路径非空字段（id、source、state、code、recipient）标 {@code required = true}——
     * 缺字段在 Jackson 边界拒绝（framework-conventions「JSON 序列化契约」+ ADR-0015 双屏障）。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @Builder
    private Verification(
            @JsonProperty(value = "id", required = true) Uuid id,
            @JsonProperty(value = "source", required = true) VerificationSource source,
            @JsonProperty(value = "state", required = true) VerificationState state,
            @JsonProperty(value = "code", required = true) VerificationCode code,
            @JsonProperty(value = "recipient", required = true) VerificationRecipient<?> recipient) {
        super(id);
        ValidateUtils.notNull(source);
        ValidateUtils.notNull(state);
        ValidateUtils.notNull(code);
        ValidateUtils.notNull(recipient);
        this.source = source;
        this.state = state;
        this.code = code;
        this.recipient = recipient;
    }

    // ─── 创建 builder（public，只暴露业务字段）───

    /**
     * 创建验证实体（创建路径，ID 创建时生成）。
     * <p>
     * {@code policy} 为<b>必传</b>参数（聚合不做通道→策略映射——策略决策属调用方<b>场景</b>，
     * 默认值由 {@code UserVerificationFactory} 按场景方法选择（UCC 专属：纯数字、短时效、
     * 双通道统一，见 ADR-0026）），效果物化进 {@link VerificationCode}，不落验证实体。{@code source} 恒必填：UCC/ULG/UPR
     * 的 subject = userId 裸键串（UCC 会话注入、ULG/UPR 端点反查），URG = 端点值串（见 ADR-0026）。
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static Verification create(VerificationSource source,
                                       VerificationRecipient<?> recipient,
                                       RandomStringGenerator generator,
                                       VerificationCodePolicy policy) {
        ValidateUtils.notNull(generator);
        ValidateUtils.notNull(policy);
        var verificationCode = VerificationCode.from(
                generator.generate(policy.codeLength(), policy.codeAlphabet()),
                Instant.now().plus(policy.expiry()));
        var verification = new Verification(
                Uuid.random(), source, VerificationState.I, verificationCode, recipient);
        verification.registerEvent(new VerificationCreatedEvent(verification));
        return verification;
    }

    // ─── state checks ───

    /**
     * 判断是否处于初始化状态（已创建、验证码尚未发送）。
     */
    public boolean isInitialized() {
        return VerificationState.I.equals(state);
    }

    /**
     * 判断是否处于待验证状态。
     */
    public boolean isPending() {
        return VerificationState.P.equals(state);
    }

    /**
     * 判断当前是否处于已验证状态——仅 VERIFIED：V 为内存瞬态（verify 后 use 前窗口成立）；
     * USED 不是「已验证」，U 是持久化终态（见 {@link VerificationState}）。
     */
    public boolean isVerified() {
        return VerificationState.V.equals(state);
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
        this.state = VerificationState.V;
    }

    /**
     * 标记为已发送 — 状态从 INITIALIZED 变为 PENDING。
     * <p>
     * 由投递侧监听器在 sender 投递成功后调用（sender 分派按 {@code recipient} 模式匹配，
     * 见 ADR-0026）；仅允许从 INITIALIZED 状态转移。
     *
     * @throws IllegalArgumentException 当前状态不是 INITIALIZED（业务状态前置）
     */
    public void markSent() {
        Assert.isTrue(VerificationState.I.equals(state), "verification must be initialized before sending");
        this.state = VerificationState.P;
    }

    /**
     * 标记为已使用。
     * <p>
     * 仅允许从 VERIFIED 状态转为 USED。
     *
     * @throws IllegalArgumentException 当前状态不是 VERIFIED（业务参数校验）
     */
    public void use() {
        Assert.isTrue(VerificationState.V.equals(state), "verification must be verified");
        this.state = VerificationState.U;
    }
}
