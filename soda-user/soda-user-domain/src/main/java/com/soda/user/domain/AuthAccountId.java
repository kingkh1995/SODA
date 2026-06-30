package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.soda.component.domain.Identifier;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 认证账户标识符密封基类 — 所有 AuthAccountId 统一为 {@link Identifier}{@code <String>}。
 * <p>
 * 序列化格式：{@code "{AuthAccountType短名}:{业务键}"}（如 {@code "P:42"}、{@code "S:13800138000"}）。
 * 反序列化由各子类的 {@code of(String)} 完成，Jackson 需声明具体子类类型。
 * <p>
 * 子类可通过 {@link #of(String)} 传入带前缀的字符串完成构造。
 *
 * @see PasswordAuthAccountId
 * @see SmsAuthAccountId
 * @see EmailAuthAccountId
 * @see SocialAuthAccountId
 */
@EqualsAndHashCode
@Getter
@Accessors(fluent = true)
public abstract sealed class AuthAccountId implements Identifier<String>
        permits PasswordAuthAccountId, SmsAuthAccountId, EmailAuthAccountId, SocialAuthAccountId {

    /**
     * 认证账户标识符各部分之间的分隔符。
     */
    protected static final String DELIMITER = ":";

    @JsonValue
    private final String value;

    protected AuthAccountId(String value) {
        ValidateUtils.hasText(value);
        this.value = value;
    }

    /**
     * 统一反序列化入口 — 根据前缀路由到对应子类。
     *
     * @param value 格式 {@code "{AuthAccountType短名}:{业务键}"}（如 {@code "P:42"}）
     * @return 对应子类的 AuthAccountId 实例
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static AuthAccountId of(String value) {
        var index = ParseUtils.indexOf(value, DELIMITER);
        var accountType = ParseUtils.parseEnum(AuthAccountType.class, value.substring(0, index));
        return switch (accountType) {
            case P -> PasswordAuthAccountId.of(value);
            case S -> SmsAuthAccountId.of(value);
            case E -> EmailAuthAccountId.of(value);
            case O -> SocialAuthAccountId.of(value);
        };
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
