package com.soda.user.domain.types;

import com.soda.component.domain.Type;
import com.soda.component.domain.util.ValidateUtils;

/**
 * 请求源 DP（见 ADR-0026）— {@code {scene, subject}} 两个纯字符串属性。
 * <p>
 * <b>Verification 对 source 的唯一要求是非空</b>：聚合持有但不解析——scene/subject 纯字符串，
 * 零行为耦合（场景语义是调用方词汇，见 {@link UserVerificationScene}）。
 * <p>
 * <b>subject = 槽位占用者（唯一索引的身份维度）</b>：UCC/ULG/UPR = userId 裸键串（UCC 会话注入、
 * ULG/UPR 端点反查推导）；URG = 端点值串（无用户）。裸键无类型前缀——同一 scene 内 subject
 * 语义类型恒定是安全前提（跨场景由 scene 消歧、同场景内类型由场景语义固定，见 ADR-0026）。
 * <p>
 * {@link #compositeKey()} = {@code "scene:subject"} — {@code active_key} 单一事实源
 * （复合键推导是 DP 合法职责——非序列化：串从不解析回 DP，是单向派生的槽位唯一键，
 * 见 ADR-0025）。
 * 不可变、自校验、可比较。
 *
 * @see Type
 */
public record VerificationSource(String scene, String subject) implements Type {

    public VerificationSource {
        ValidateUtils.hasText(scene);
        ValidateUtils.hasText(subject);
    }

    public static VerificationSource of(String scene, String subject) {
        return new VerificationSource(scene, subject);
    }

    /**
     * 规范复合键：{@code "{scene}:{subject}"}（如 {@code UCC:42}）——{@code active_key} 单一事实源。
     */
    public String compositeKey() {
        return scene + ":" + subject;
    }
}
