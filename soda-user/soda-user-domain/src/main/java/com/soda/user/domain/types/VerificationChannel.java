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
 * 与 {@link com.soda.user.domain.Verification} 子类型一一对应的判别值——领域词汇 + 数据形态
 * （JSON {@code channel} 属性、持久化判别列）。<b>不持有任何 Class 引用</b>：
 * 实例侧编码由子类覆写 {@code Verification#getChannel()} 提供（编译期强制），
 * class→channel 反查归基础设施（ADR-0016 决策 2/3）。
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
