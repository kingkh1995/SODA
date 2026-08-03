package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 验证场景枚举。
 * <p>
 * CC(Credential Change) 换绑联系方式/凭证变更
 * PR(Password Reset) 密码重置
 * LG(Login) 登录
 * RG(Register) 注册
 *
 * @see EnumType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum VerificationScene implements EnumType {

    CC("credential-change"),
    PR("password-reset"),
    LG("login"),
    RG("register");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VerificationScene of(String name) {
        return ParseUtils.parseEnum(VerificationScene.class, name);
    }
}