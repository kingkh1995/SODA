package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.soda.component.domain.gateway.PasswordHasher;
import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.SecretValue;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.PasswordAuthAccountId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 密码认证账户实体 — 用户名 + 密码方式的认证。
 * <p>
 * 每个 User 有且仅有一个 PasswordAuthAccount，在 User 创建时自动生成。
 *
 * @see AuthAccount
 */
@Getter
@JsonTypeName("P")
@EqualsAndHashCode(callSuper = true)
public final class PasswordAuthAccount extends AuthAccount<PasswordAuthAccountId> {

    private PasswordHash passwordHash;

    // ─── construction ───

    /**
     * 全参数恢复构造器 — 持久化恢复与 JSON 反序列化唯一入口（{@link JsonCreator}）。
     * <p>
     * 委托无 id 创建构造器完成字段初始化后补充 id（构造器参数校验见该构造器）。
     * 恢复路径非空字段（id、active、passwordHash）由 JSON schema 声明（{@code required = true}），
     * 缺字段由 Jackson 在协议边界拒绝（框架能力，非领域守卫）。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @Builder
    private PasswordAuthAccount(
            @JsonProperty(value = "id", required = true) PasswordAuthAccountId id,
            @JsonProperty(value = "active", required = true) Active active,
            @JsonProperty(value = "passwordHash", required = true) PasswordHash passwordHash) {
        this(active, passwordHash);
        assignId(id);
    }

    /**
     * 无 id 创建构造器 — 仅创建路径使用，active 由调用方传入，id 由 Gateway 在持久化后通过 {@link #assignId} 填补。
     * <p>
     * 字段初始化唯一存在处，全参数恢复构造器委托本构造器。
     */
    private PasswordAuthAccount(Active active, PasswordHash passwordHash) {
        super(active);
        ValidateUtils.notNull(passwordHash);
        // 恒启用不变量（ADR-0004）：密码账户不允许禁用——创建路径强制 TRUE、恢复路径拒绝 FALSE
        ValidateUtils.equals(active, Active.TRUE);
        this.passwordHash = passwordHash;
    }

    // ─── factories ───

    /**
     * 创建新密码账户 — active 默认 TRUE，ID 由 Gateway 在持久化后通过 {@link #assignId} 补充。
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static PasswordAuthAccount create(PasswordHash passwordHash) {
        return new PasswordAuthAccount(Active.TRUE, passwordHash);
    }

    // ─── accessors ───

    /**
     * 认证类型 — 常量来源为 {@link PasswordAuthAccountId#ACCOUNT_TYPE}（与 ID 解耦，无 ID 亦可派发）。
     */
    @Override
    public AuthAccountType getAccountType() {
        return PasswordAuthAccountId.ACCOUNT_TYPE;
    }

    // ─── queries ───

    /**
     * 校验原始凭证是否匹配当前哈希。
     *
     * @param credential 原始凭证
     * @param hasher     凭证哈希器
     * @return true 若匹配
     */
    public boolean verify(SecretValue credential, PasswordHasher hasher) {
        return hasher.verify(passwordHash, credential);
    }

    // ─── commands ───

    /**
     * 修改密码。
     *
     * @param credential 新原始凭证
     * @param hasher     凭证哈希器
     * @implNote 不在此处注册 PasswordChangedEvent（泛型 ID 不匹配，事件需 {@code UserId}）；
     * 事件由 {@code User.changePassword} 在调用本方法后注册。
     */
    public void changePassword(SecretValue credential, PasswordHasher hasher) {
        this.passwordHash = hasher.hash(credential);
    }

    /**
     * 登录透明升级 —— 验证候选凭证，通过且存储哈希低于当前配置时以候选凭证重哈希
     * （ADR-0033 注记 7；ULG 登录路径的落点）。
     * 错误候选返回 false 且不动 {@code passwordHash}（安全不变量：绝不用未经
     * 验证的凭证覆盖哈希）；调用方负责持久化以使新哈希落库（与 changePassword 同约定）。
     *
     * @return 候选凭证是否匹配
     */
    public boolean verifyAndRehash(SecretValue credential, PasswordHasher hasher) {
        if (!hasher.verify(passwordHash, credential)) {
            return false;
        }
        if (hasher.needsRehash(passwordHash)) {
            this.passwordHash = hasher.hash(credential);
        }
        return true;
    }

    /**
     * 停用账户 — 拒绝：密码账户**恒启用**（设计不变量，ADR-0004——无解绑流程，
     * 持久化无 active 列，恢复恒 {@code Active.TRUE}）。不允许设置为禁用态。
     * 防御编程：调用方按契约不得调用，异常类型 + 栈帧即语义，不携消息。
     *
     * @throws UnsupportedOperationException 恒抛（该操作对密码账户不支持）
     */
    @Override
    public void deactivate() {
        throw new UnsupportedOperationException();
    }

}
