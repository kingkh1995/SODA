package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 验证通道枚举。取值：S（SMS 短信）、E（Email 邮箱）。
 * <p>
 * <b>普通枚举，不挂 Class 引用</b>（映射归基础设施，见 ADR-0016）。角色（见 ADR-0026）：
 * ① 预认证场景命令输入（ULG/UPR/URG）；② {@link VerificationRecipient} 判别属性——
 * <b>channel 只在 VerificationRecipient</b>，subject 不携带通道语义；持久化 channel 独立列 +
 * target 裸值。码形策略按<b>场景</b>而非通道选择（UCC 专属策略双通道统一，不引用通道默认，
 * 见 ADR-0026）。
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
