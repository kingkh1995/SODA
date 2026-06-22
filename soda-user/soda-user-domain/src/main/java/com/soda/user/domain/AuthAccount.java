package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.soda.component.domain.Entity;
import com.soda.component.support.types.Active;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.Getter;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 认证账户抽象基类 — User 聚合下的子实体。
 * <p>
 * 密封类，仅允许 {@link PasswordAuthAccount}、{@link SmsAuthAccount}、{@link EmailAuthAccount}、{@link SocialAuthAccount} 四种子类。
 * 子类通过多态实现不同认证方式的行为差异。
 *
 * @param <ID> 账户标识符类型，必须是 {@link AuthAccountId} 的子类
 * @see PasswordAuthAccount
 * @see SmsAuthAccount
 * @see EmailAuthAccount
 * @see SocialAuthAccount
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "authAccountType"
)
@JsonIgnoreProperties("authAccountType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PasswordAuthAccount.class, name = "P"),
        @JsonSubTypes.Type(value = SmsAuthAccount.class, name = "S"),
        @JsonSubTypes.Type(value = EmailAuthAccount.class, name = "E"),
        @JsonSubTypes.Type(value = SocialAuthAccount.class, name = "O")
})
public abstract sealed class AuthAccount<ID extends AuthAccountId> extends Entity<ID>
        permits PasswordAuthAccount, SmsAuthAccount, EmailAuthAccount, SocialAuthAccount {

    public static final Predicate<AuthAccount<?>> ACTIVE = AuthAccount::isActive;

    public static Predicate<AuthAccount<?>> ofType(AuthAccountType type) {
        return a -> a.getAuthAccountType() == type;
    }

    @Getter
    private Active active;

    // ─── construction ───

    /** 手动设置 / 已有数据恢复。 */
    protected AuthAccount(ID id, Active active) {
        super(id);
        this.active = Objects.requireNonNull(active);
    }

    /** 返回该账户的认证类型 — 委托至 {@link #getId()}.{@link AuthAccountId#authAccountType() authAccountType()}。 */
    public final AuthAccountType getAuthAccountType() {
        return Objects.requireNonNull(getId()).authAccountType();
    }

    /** 是否激活。 */
    public boolean isActive() {
        return active.value();
    }

    /** 激活账户。 */
    public void activate() {
        this.active = Active.TRUE;
    }

    /** 停用账户。 */
    public void deactivate() {
        this.active = Active.FALSE;
    }
}
