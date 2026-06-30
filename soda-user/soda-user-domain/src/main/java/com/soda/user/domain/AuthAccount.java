package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soda.component.domain.Entity;
import com.soda.component.support.types.Active;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;

import java.util.function.Predicate;

/**
 * 认证账户抽象基类 — User 聚合下的子实体。
 * <p>
 * 密封类，仅允许 {@link PasswordAuthAccount}、{@link SmsAuthAccount}、{@link EmailAuthAccount}、{@link SocialAuthAccount} 四种子类。
 * 子类通过多态实现不同认证方式的行为差异。
 * <p>
 * <b>新增子类提醒</b>：{@code permits} 子句 + 新增类声明后，在新增类上添加 {@code @JsonTypeName} 注解指定类型标识。
 * Jackson 3 从密封类 {@code permits} 子句自动发现子类，无需 {@code @JsonSubTypes}。
 * <p>
 * Jackson 序列化说明：{@link Entity 基类} 声明了 {@code @JsonAutoDetect(getterVisibility = NONE)}，
 * 因此子类上的 {@code @Getter}（Lombok 生成 getter）不影响 JSON 序列化／反序列化。
 * 反序列化走 {@code @JsonCreator(mode = Mode.PROPERTIES)} 构造器 + {@code @JsonProperty} 参数，
 * 序列化走字段可见性（field visibility ANY）。{@code @Getter} 仅用于 Java 代码层面的快捷访问，非 Jackson 用途。
 *
 * @param <ID> 账户标识符类型，必须是 {@link AuthAccountId} 的子类
 * @see PasswordAuthAccount
 * @see SmsAuthAccount
 * @see EmailAuthAccount
 * @see SocialAuthAccount
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "authAccountType")
@EqualsAndHashCode(callSuper = true)
public abstract sealed class AuthAccount<ID extends AuthAccountId> extends Entity<ID>
        permits PasswordAuthAccount, SmsAuthAccount, EmailAuthAccount, SocialAuthAccount {

    public static final Predicate<AuthAccount<?>> ACTIVE = AuthAccount::isActive;
    private Active active;

    /**
     * 手动设置 / 已有数据恢复。
     */
    protected AuthAccount(ID id, Active active) {
        super(id);
        Assert.notNull(active, "active must not be null");
        this.active = active;
    }

    // ─── construction ───

    public static Predicate<AuthAccount<?>> ofType(AuthAccountType type) {
        return a -> a.getAuthAccountType() == type;
    }

    /**
     * 返回该账户的认证类型 — 委托至 {@link #getId()}.{@link AuthAccountId#authAccountType() authAccountType()}。
     */
    public final AuthAccountType getAuthAccountType() {
        return getId().authAccountType();
    }

    /**
     * 是否激活。
     */
    public boolean isActive() {
        return active.value();
    }

    /**
     * 激活账户。
     */
    public void activate() {
        this.active = Active.TRUE;
    }

    /**
     * 停用账户。
     */
    public void deactivate() {
        this.active = Active.FALSE;
    }
}
