package com.soda.user.application.factory;

import com.soda.component.domain.gateway.RandomStringGenerator;
import com.soda.component.domain.types.Alphabet;
import com.soda.component.domain.types.PositiveInt;
import com.soda.user.domain.Verification;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserVerificationScene;
import com.soda.user.domain.types.VerificationCodePolicy;
import com.soda.user.domain.types.VerificationRecipient;
import com.soda.user.domain.types.VerificationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 用户侧验证构造工厂（2026-08-16，见 ADR-0026）— <b>调用方词汇 → 领域构造</b>的薄映射层。
 * <p>
 * 职责：
 * <ul>
 *   <li>{@link #newCredentialChangeSource(UserId)} — UCC source 构造（scene=UCC 方法内写死、
 *       userId→裸键串；subject = 槽位占用者，见 ADR-0026）——<b>AppService 侧唯一映射点</b>，
 *       槽位预检/消费反查与构造方法共用，杜绝服务侧两处映射漂移</li>
 *   <li><b>按场景的构造方法</b>（如 {@link #newCredentialChangeVerification(UserId, VerificationRecipient)}）——
 *       scene 在方法内写死（方法名即场景），策略按场景选择（<b>不引用通道默认
 *       DEFAULT_SMS/DEFAULT_EMAIL</b>——策略决策属调用方场景，2026-08-16 会话修订，见 ADR-0026）</li>
 * </ul>
 * 边界：<b>纯构造</b>——业务前置（跨实例查询）与持久化/事件发布由 AppService 执行。
 * 当前仅 UCC；ULG/UPR/URG（未来票）落地时按各自场景加构造方法。
 */
@Component
@RequiredArgsConstructor
public class UserVerificationFactory {

    /**
     * UCC（User Credential Change，换绑联系方式/凭证变更）专属码形策略（2026-08-16 会话修订，
     * 见 ADR-0026）：换绑场景<b>无论通道（手机/邮箱）统一纯数字、短时效</b>——6 位数字 / 5 分钟
     * 过期。不引用通道默认（{@link VerificationCodePolicy#DEFAULT_SMS}/
     * {@link VerificationCodePolicy#DEFAULT_EMAIL}）——策略按场景（用例）而非通道选择。
     */
    private static final VerificationCodePolicy UCC_CODE_POLICY = new VerificationCodePolicy(
            PositiveInt.of(6), Duration.ofMinutes(5), Alphabet.DIGITS);

    private final RandomStringGenerator randomStringGenerator;

    /**
     * UCC source 构造 — scene=UCC 方法内写死（方法名即场景）；槽位预检/消费反查（AppService）
     * 与构造方法共用的唯一映射点。
     */
    public VerificationSource newCredentialChangeSource(UserId userId) {
        return VerificationSource.of(UserVerificationScene.UCC.name(), Long.toString(userId.value()));
    }

    /**
     * 构造 UCC（User Credential Change，换绑联系方式/凭证变更）INITIALIZED 验证聚合 —
     * scene=UCC 方法内写死（方法名即场景，见 ADR-0026）；策略 = UCC 专属
     * （纯数字、短时效、双通道统一）；码生成 + 事件注册经领域静态工厂单构造路径。
     */
    public Verification newCredentialChangeVerification(UserId userId, VerificationRecipient<?> recipient) {
        return Verification.createBuilder()
                .source(newCredentialChangeSource(userId))
                .recipient(recipient)
                .generator(randomStringGenerator)
                .policy(UCC_CODE_POLICY)
                .build();
    }
}
