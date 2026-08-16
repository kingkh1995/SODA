package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 验证渠道枚举。
 * <p>
 * S(SMS) 短信
 * E(Email) 邮箱
 * <p>
 * **普通枚举，不挂 Class 引用**（映射归基础设施，见 ADR-0016）。角色（2026-08-16，见 ADR-0026）：
 * ① 预认证场景命令输入（ULG/UPR/URG 未来票）；② {@link VerificationRecipient} 判别属性
 * （channel + target 双属性多属性 DP——<b>channel 只在 VerificationRecipient</b>，subject 不携带；
 * 持久化 channel 独立列 + target 裸值，2026-08-16 修订）；③ 码形策略选择（{@code UserVerificationFactory} 按 {@code recipient.channel()}
 * 内联选 {@code DEFAULT_SMS}/{@code DEFAULT_EMAIL}）。
 *
 * @see EnumType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum VerificationChannel implements EnumType {

    S("sms"),
    E("email");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VerificationChannel of(String name) {
        return ParseUtils.parseEnum(VerificationChannel.class, name);
    }
}
