package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.EnumType;
import com.soda.component.domain.util.ParseUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户验证场景枚举（2026-08-16 由 {@code VerificationScene} 更名，见 ADR-0026）。
 * <p>
 * <b>调用方词汇</b>——Verification 不感知（{@link VerificationSource#scene()} 是纯字符串，
 * 场景语义只在此枚举与工厂/AppService 消费）。取值：
 * UCC(User Credential Change) 用户换绑联系方式/凭证变更（subject = 用户）
 * UPR(User Password Reset) 用户找回密码（subject = 用户——端点反查）
 * ULG(User Login) 用户登录验证（subject = 用户——端点反查）
 * URG(User Register) 用户注册验证（subject = 投递端点——用户尚不存在）
 * <p>
 * 扁平助记码（2026-08-15，ADR-0005 修订——领域前缀助记码满足 1-4 字符规则，SocialType 先例，
 * 见 ADR-0025）：{@link #name()} 返回助记码串（= {@link VerificationSource#scene()} 值），
 * 跨领域共享时以前缀隔离（未来 sys_user 场景用 {@code S*}）。
 *
 * @see EnumType
 */
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum UserVerificationScene implements EnumType {

    UCC("user-credential-change"),
    UPR("user-password-reset"),
    ULG("user-login"),
    URG("user-register");

    private final String desc;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserVerificationScene of(String name) {
        return ParseUtils.parseEnum(UserVerificationScene.class, name);
    }
}
