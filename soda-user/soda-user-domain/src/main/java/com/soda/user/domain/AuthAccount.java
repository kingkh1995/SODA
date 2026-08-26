package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soda.component.domain.Entity;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.types.AuthAccountId;
import com.soda.user.domain.types.AuthAccountType;
import lombok.EqualsAndHashCode;

/**
 * 认证账户抽象基类 — User 聚合下的子实体，一种认证方式一个子类。
 * <p>
 * <b>新增子类提醒</b>：{@code permits} 子句放行后，在新增类上标注 {@code @JsonTypeName}
 * 指定类型标识，并同步补充 {@link AuthAccountType} 枚举常量与一致性测试（ADR-0016）。
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

    private Active active;

    /**
     * 创建路径构造器 — 无 ID，由 Repository 持久化后经 {@link #assignId} 填补。
     */
    protected AuthAccount(Active active) {
        super();
        ValidateUtils.notNull(active);
        this.active = active;
    }

    /**
     * 恢复路径构造器 — 显式携带已生成 ID。
     */
    protected AuthAccount(ID id, Active active) {
        super(id);
        ValidateUtils.notNull(active);
        this.active = active;
    }

    // ─── factories ───

    /**
     * 返回该账户的认证类型 — 常量来源为各 ID 子类的 {@code ACCOUNT_TYPE}，与 ID 解耦（无 ID 亦可派发）。
     */
    public abstract AuthAccountType getAccountType();

    public boolean typeEquals(AuthAccount<?> other) {
        return other.getAccountType().equals(getAccountType());
    }

    public boolean isActive() {
        return active.value();
    }

    // ─── commands ───

    public void activate() {
        this.active = Active.TRUE;
    }

    public void deactivate() {
        this.active = Active.FALSE;
    }
}
