package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.Aggregate;
import com.soda.component.domain.gateway.PasswordHasher;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.SecretValue;
import com.soda.component.domain.types.Sex;
import com.soda.component.domain.types.Version;
import com.soda.component.domain.util.ValidateUtils;
import com.soda.user.domain.event.PasswordChangedEvent;
import com.soda.user.domain.event.UserCreatedEvent;
import com.soda.user.domain.event.UserDeregisteredEvent;
import com.soda.user.domain.event.UserStateChangedEvent;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.EmailRecipient;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.SmsRecipient;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.UserVerificationScene;
import com.soda.user.domain.types.Username;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 用户身份聚合根 — 持有一组 AuthAccount 子实体与用户资料属性。
 * <p>
 * 创建时通过 {@link #createBuilder()} 构建，不含 ID（服务端生成）；
 * 持久化恢复通过 {@link #builder()}。
 * <p>
 * 不变量：passwordAccount 为构造期必填独立字段（每 User 恰一个密码账户，ADR-0004 类型化）；
 * accounts 仅存可选账户且同类型至多一条；状态机 E/D/R（R 吸收态终态，见 ADR-0017），
 * 注销后的键释放是基础设施表示决策，领域不感知（见 ADR-0023）。
 *
 * @see Aggregate
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class User extends Aggregate<UserId> {

    private final PasswordAuthAccount passwordAccount;
    private Username username;
    private Nickname nickname;
    private UserState state;
    private @Nullable Mobile mobile;
    private @Nullable Email email;
    private @Nullable Sex sex;
    private @Nullable Avatar avatar;
    private List<AuthAccount<?>> accounts;
    /**
     * 乐观锁版本号（持久化状态）— 创建路径恒为 {@link Version#INITIAL}，恢复路径随持久化数据流转。
     * 递增由基础设施层负责（写入时 {@code WHERE version = ?} 校验后落 version + 1），
     * 领域逻辑不触碰（Vernon IDDD：手动递增会泄漏基础设施关注点到模型）。
     */
    private Version version;

    // ─── 构造器 ───

    /**
     * 全参数恢复构造器 — 持久化恢复与 JSON 反序列化唯一入口（{@link JsonCreator}）。
     * <p>
     * 委托无 id 创建构造器完成字段初始化后补充 id 与 version（构造器参数校验见该构造器）。
     * 序列化走 {@code Entity} 基类字段可见性；恢复路径非空字段（id、version、username、nickname、
     * state、passwordAccount）由 JSON schema 声明（{@code required = true}），缺字段由 Jackson
     * 在协议边界拒绝（框架能力，非领域守卫）；可空字段（mobile / email / sex / avatar）标
     * {@code @Nullable}，accounts 缺省为空列表。
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @Builder
    private User(
            @JsonProperty(value = "id", required = true) UserId id,
            @JsonProperty(value = "version", required = true) Version version,
            @JsonProperty(value = "username", required = true) Username username,
            @JsonProperty(value = "nickname", required = true) Nickname nickname,
            @JsonProperty(value = "state", required = true) UserState state,
            @JsonProperty("mobile") @Nullable Mobile mobile,
            @JsonProperty("email") @Nullable Email email,
            @JsonProperty("sex") @Nullable Sex sex,
            @JsonProperty("avatar") @Nullable Avatar avatar,
            @JsonProperty(value = "passwordAccount", required = true) PasswordAuthAccount passwordAccount,
            @JsonProperty("accounts") @Nullable List<AuthAccount<?>> accounts) {
        this(username, nickname, state, mobile, email, sex, avatar, passwordAccount, accounts);
        assignId(id);
        this.version = version;
    }

    /**
     * 无 id 创建构造器 — 仅创建路径使用，id 由 Repository 在持久化后通过 {@link #assignId} 填补。
     * <p>
     * 字段初始化唯一存在处，全参数恢复构造器委托本构造器。
     * <p>
     * {@code passwordAccount} 为必填字段（ADR-0004：User 必有密码账户，类型化保证，无守卫）；
     * {@code accounts} 仅存放可选账户（Sms / Email / Social），不允许包含密码账户。
     * {@code version} 创建路径恒为 {@link Version#INITIAL}（乐观锁递增归基础设施层）。
     */
    private User(Username username, Nickname nickname, UserState state, @Nullable Mobile mobile, @Nullable Email email, @Nullable Sex sex, @Nullable Avatar avatar, PasswordAuthAccount passwordAccount, @Nullable List<AuthAccount<?>> accounts) {
        ValidateUtils.notNull(username);
        ValidateUtils.notNull(nickname);
        ValidateUtils.notNull(state);
        ValidateUtils.notNull(passwordAccount);
        this.username = username;
        this.nickname = nickname;
        this.state = state;
        this.mobile = mobile;
        this.email = email;
        this.sex = sex;
        this.avatar = avatar;
        this.passwordAccount = passwordAccount;
        this.accounts = new LinkedList<>(Objects.requireNonNullElse(accounts, List.of()));
        this.version = Version.INITIAL;
    }

    // ─── 创建 builder（public，只暴露业务字段）───

    /**
     * 创建新用户。
     * <p>
     * 生成的 User 不含 ID（由 Repository save 后 {@link #assignId} 填补），
     * 必传密码哈希构造 {@link PasswordAuthAccount} 作为独立字段（ADR-0004 类型化）；
     * 传入 mobile / email 时分别追加 {@link SmsAuthAccount} / {@link EmailAuthAccount} 到账户列表。
     * 注册 {@link UserCreatedEvent}（entityId 在 flush 时延迟求值）。
     *
     * @param passwordHash 必传密码哈希，自动创建 {@link PasswordAuthAccount}
     */
    @Builder(builderClassName = "CreateBuilder", builderMethodName = "createBuilder")
    private static User create(Username username, Nickname nickname, @Nullable Mobile mobile, @Nullable Email email, @Nullable Sex sex, @Nullable Avatar avatar, PasswordHash passwordHash) {
        var user = new User(username, nickname, UserState.E, mobile, email, sex, avatar,
                PasswordAuthAccount.createBuilder().passwordHash(passwordHash).build(), null);
        if (mobile != null) {
            user.addAccount(SmsAuthAccount.createBuilder().mobile(mobile).build());
        }
        if (email != null) {
            user.addAccount(EmailAuthAccount.createBuilder().email(email).build());
        }
        user.registerEvent(new UserCreatedEvent(user));
        return user;
    }

    // ─── accessors ───

    /**
     * 返回可选账户列表的不可修改视图（不包含密码账户，密码账户见 {@link #getPasswordAccount()}）。
     */
    public List<AuthAccount<?>> getAccounts() {
        return List.copyOf(accounts);
    }

    public Optional<Mobile> getMobile() {
        return Optional.ofNullable(mobile);
    }

    public Optional<Email> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<Sex> getSex() {
        return Optional.ofNullable(sex);
    }

    public Optional<Avatar> getAvatar() {
        return Optional.ofNullable(avatar);
    }

    // ─── 账户管理 ───

    /**
     * 查找第一个匹配条件的账户。
     */
    protected Optional<AuthAccount<?>> findAccount(Predicate<AuthAccount<?>> filter) {
        return accounts.stream().filter(filter).findFirst();
    }

    /**
     * 添加可选认证账户。
     * <p>
     * 前置：账户须为激活状态、同类型账户不重复；密码账户为独立字段，不允许放入 accounts。
     * 已包含同一实例时 no-op。
     */
    protected void addAccount(AuthAccount<?> account) {
        Assert.isTrue(!(account instanceof PasswordAuthAccount), "Password account must be set separately.");
        if (accounts.contains(account)) {
            return;
        }
        Assert.isTrue(account.isActive(), "Account should be active.");
        Assert.isTrue(findAccount(account::typeEquals).isEmpty(), "Account already exists.");
        this.accounts.add(account);
    }

    /**
     * 移除指定类型的可选账户（该类型至多一条；密码账户为独立字段，不受影响）。
     */
    protected void removeAccount(AuthAccountType accountType) {
        this.accounts.removeIf(account -> Objects.equals(account.getAccountType(), accountType));
    }

    // ─── 属性修改 ───

    public void changeUsername(Username newUsername) {
        mustEnable();
        this.username = newUsername;
    }

    public void changeNickname(Nickname nickname) {
        mustEnable();
        this.nickname = nickname;
    }

    public void changeSex(@Nullable Sex sex) {
        mustEnable();
        this.sex = sex;
    }

    public void changeAvatar(@Nullable Avatar avatar) {
        mustEnable();
        this.avatar = avatar;
    }

    /**
     * 修改密码 — 校验原密码后委托 {@link PasswordAuthAccount#changePassword}，注册 {@link PasswordChangedEvent}。
     * <p>
     * 密码账户为构造期必填字段（ADR-0004 类型化），此处直接委托，无查找。
     * <p>
     * 原密码比对为域守卫（不变量单一来源）：通过 {@link PasswordAuthAccount#verify} 校验
     * 旧凭证与当前哈希，不匹配抛 {@link IllegalArgumentException}，不产生任何变更与事件。
     *
     * @param oldCredential 原密码（校验对应用户当前凭证）
     * @param newCredential 新密码
     * @param hasher        口令哈希器
     */
    public void changePassword(SecretValue oldCredential, SecretValue newCredential, PasswordHasher hasher) {
        mustEnable();
        Assert.isTrue(passwordAccount.verify(oldCredential, hasher), "Invalid old password");
        passwordAccount.changePassword(newCredential, hasher);
        registerEvent(new PasswordChangedEvent(getId()));
    }

    /**
     * 修改手机号（消费 UCC 验证聚合）。
     * <p>
     * 前置：
     * <ul>
     *   <li>source 匹配本用户——subject 裸键串与 userId 相等（按 source 加载即主体匹配，
     *       此处保留为防御纵深，见 ADR-0026）</li>
     *   <li>场景为 UCC——其他场景的验证码不可用于换绑</li>
     *   <li>VERIFIED 状态（USED 为终态，不可重放）</li>
     *   <li>recipient 为 {@link SmsRecipient} 且目标手机号不同于当前值</li>
     * </ul>
     * 后置：mobile 字段与 SmsAuthAccount 同步替换（联动不变量——二者恒一致）。
     *
     * @param verification 已通过验证的验证聚合（recipient 为 {@link SmsRecipient}）
     */
    public void changeMobile(Verification verification) {
        mustEnable();
        Assert.isTrue(Objects.equals(verification.getSource().scene(), UserVerificationScene.UCC.name()),
                "verification scene must be credential change, but was " + verification.getSource().scene());
        // subject 裸键串推导与 UserVerificationFactory.source 一致——防御纵深守卫（find-by-source 已按
        // 构造精确，此处冗余兜底；推导刻意留在聚合侧而非复用工厂，见 ADR-0026 双概念模型）
        Assert.isTrue(Objects.equals(verification.getSource().subject(), Long.toString(getId().value())),
                "verification subject must match user " + getId().value()
                        + ", but was " + verification.getSource().subject());
        Assert.isTrue(verification.isVerified(), "verification must be verified");
        var recipient = verification.getRecipient();
        if (!(recipient instanceof SmsRecipient(Mobile newMobile))) {
            throw new IllegalArgumentException(
                    "verification recipient must be a mobile, but was " + recipient.channel() + ":" + recipient.target());
        }
        Assert.isTrue(!Objects.equals(mobile, newMobile),
                "Cannot change to the same mobile: " + newMobile.value());
        this.mobile = newMobile;
        // 替换 SmsAuthAccount：移除旧账户，添加新账户
        removeAccount(SmsAuthAccountId.ACCOUNT_TYPE);
        addAccount(SmsAuthAccount.createBuilder().mobile(newMobile).build());
    }

    /**
     * 修改邮箱（消费 UCC 验证聚合）。
     * <p>
     * 前置：
     * <ul>
     *   <li>source 匹配本用户——subject 裸键串与 userId 相等（按 source 加载即主体匹配，
     *       此处保留为防御纵深，见 ADR-0026）</li>
     *   <li>场景为 UCC——其他场景的验证码不可用于换绑</li>
     *   <li>VERIFIED 状态（USED 为终态，不可重放）</li>
     *   <li>recipient 为 {@link EmailRecipient} 且目标邮箱不同于当前值</li>
     * </ul>
     * 后置：email 字段与 EmailAuthAccount 同步替换（联动不变量——二者恒一致）。
     *
     * @param verification 已通过验证的验证聚合（recipient 为 {@link EmailRecipient}）
     */
    public void changeEmail(Verification verification) {
        mustEnable();
        Assert.isTrue(Objects.equals(verification.getSource().scene(), UserVerificationScene.UCC.name()),
                "verification scene must be credential change, but was " + verification.getSource().scene());
        // subject 裸键串推导与 UserVerificationFactory.source 一致——防御纵深守卫（见 changeMobile 注释）
        Assert.isTrue(Objects.equals(verification.getSource().subject(), Long.toString(getId().value())),
                "verification subject must match user " + getId().value()
                        + ", but was " + verification.getSource().subject());
        Assert.isTrue(verification.isVerified(), "verification must be verified");
        var recipient = verification.getRecipient();
        if (!(recipient instanceof EmailRecipient(Email newEmail))) {
            throw new IllegalArgumentException(
                    "verification recipient must be an email, but was " + recipient.channel() + ":" + recipient.target());
        }
        Assert.isTrue(!Objects.equals(email, newEmail),
                "Cannot change to the same email: " + newEmail.value());
        this.email = newEmail;
        // 替换 EmailAuthAccount：移除旧账户，添加新账户
        removeAccount(EmailAuthAccountId.ACCOUNT_TYPE);
        addAccount(EmailAuthAccount.createBuilder().email(newEmail).build());
    }

    // ─── 生命周期守卫 ───

    /**
     * 断言启用态（E）——变更方法（changeXxx）与发码前置共用的单一守卫，服务层不重复该规则
     * （防消息/逻辑漂移）。D 与 R 均拒绝：D 仅可 {@link #enable()} / {@link #deregister()}，
     * R 为吸收态无任何操作（见 ADR-0017）。
     */
    public void mustEnable() {
        Assert.isTrue(UserState.E.equals(state), "user must be enabled");
    }

    // ─── 注销 ───

    /**
     * 注销用户 — 严格 transition：前置必须为禁用状态（D）。
     * <p>
     * D → R（Removed / 注销，吸收态终态），注册 {@link UserDeregisteredEvent}。
     * R 持久化为状态列（ADR-0017）；表示（状态列 / 软删 / 删行）由基础设施层决定，
     * 领域不感知擦除。此后任何行为方法调用抛 IAE（{@link #mustEnable()} / R 吸收态检查）。
     */
    public void deregister() {
        Assert.isTrue(UserState.D.equals(state), "only disabled user can be deregistered");
        registerEvent(new UserDeregisteredEvent(getId()));
        this.state = UserState.R;
    }

    // ─── 状态切换 ───

    /**
     * 禁用用户。E→D 时注册 {@link UserStateChangedEvent}；已是 D 则 no-op；R（吸收态）→ IAE。
     */
    public void disable() {
        Assert.isTrue(!UserState.R.equals(state), "user already deregistered");
        if (UserState.D.equals(state)) {
            return;
        }
        var oldState = state;
        this.state = UserState.D;
        registerEvent(new UserStateChangedEvent(getId(), oldState, UserState.D));
    }

    /**
     * 启用用户。D→E 时注册 {@link UserStateChangedEvent}；已是 E 则 no-op；R（吸收态）→ IAE。
     */
    public void enable() {
        Assert.isTrue(!UserState.R.equals(state), "user already deregistered");
        if (UserState.E.equals(state)) {
            return;
        }
        var oldState = state;
        this.state = UserState.E;
        registerEvent(new UserStateChangedEvent(getId(), oldState, UserState.E));
    }
}
