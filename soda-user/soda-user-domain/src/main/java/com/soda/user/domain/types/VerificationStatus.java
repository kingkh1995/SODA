package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 验证状态枚举。
 * <p>
 * P(Pending) 待验证
 * V(Verified) 已验证
 * U(Used) 已使用（终态；过期是派生判断，不落状态，见 ADR-0011）
 *
 * @see EnumType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum VerificationStatus implements EnumType {

    P("pending"),
    V("verified"),
    U("used");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VerificationStatus of(String name) {
        return ParseUtils.parseEnum(VerificationStatus.class, name);
    }
}