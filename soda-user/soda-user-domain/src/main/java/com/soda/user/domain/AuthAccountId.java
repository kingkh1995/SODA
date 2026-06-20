package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Identifier;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import com.soda.component.support.util.ValidateUtils;

import java.io.Serial;

/**
 * 认证账户标识符密封基类 — 所有 AuthAccountId 统一为 {@link Identifier}{@code <String>}。
 * <p>
 * 序列化格式：{@code "{AuthAccountType短名}:{业务键}"}（如 {@code "P:42"}、{@code "S:13800138000"}）。
 * 反序列化由各子类的 {@code valueOf(Object)} 完成，Jackson 需声明具体子类类型。
 *
 * @see PasswordAuthAccountId
 * @see SmsAuthAccountId
 * @see EmailAuthAccountId
 * @see SocialAuthAccountId
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
@Accessors(fluent = true)
public abstract sealed class AuthAccountId implements Identifier<String>
        permits PasswordAuthAccountId, SmsAuthAccountId, EmailAuthAccountId, SocialAuthAccountId {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonValue
    @EqualsAndHashCode.Include
    private final String value;

    protected AuthAccountId(String value) {
        ValidateUtils.nonBlank(value);
        this.value = value;
    }

    @Override
    public final String identifier() {
        return value;
    }

    public abstract AuthAccountType authAccountType();


    @Override
    public String toString() {
        return getClass().getSimpleName() + "[value=" + value + "]";
    }
}
