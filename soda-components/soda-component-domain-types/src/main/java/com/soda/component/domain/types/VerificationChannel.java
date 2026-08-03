package com.soda.component.domain.types;

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