package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.soda.component.domain.Entity;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.types.AuthAccountId;
import com.soda.user.domain.types.AuthAccountType;
import lombok.EqualsAndHashCode;

/**
 * 认证账户抽象基类 — User 聚合下的子实体。
 * <p>
 * 密封类，仅允许 {@link PasswordAuthAccount}、{@link SmsAuthAccount}、{@link EmailAuthAccount}、{@link SocialAuthAccount} 四种子类。
 * 子类通过多态实现不同认证方式的行为差异。
 * <p>
 * ton * <b>新增子类提醒</b>：{@code permits} 子句 + 新增类声明后，在新增类上添加 {@code @JsonTypeName} 注解指定类型标识；
 * 同步补充 {@link AuthAccountType} 枚举常量与一致性测试（ADR-0016）。
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

    private Active active;

    /**
     * 服务端生成 ID：构造时无 ID，后续由 Repository 调用 {@link #assignId(Identifier)} 填补。
     * active 默认 {@link Active#TRUE}。
     */
    protected AuthAccount(Active active) {
        super();
        ValidateUtils.notNull(active);
        this.active = active;
    }

    /**
     * 手动设置 / 已有数据恢复。
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

    /**
     * 是否激活。
     */
    public boolean isActive() {
        return active.value();
    }

    // ─── commands ───

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
