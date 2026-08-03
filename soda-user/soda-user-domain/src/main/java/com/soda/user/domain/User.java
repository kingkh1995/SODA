package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.Aggregate;
import com.soda.component.domain.gateway.CredentialHasher;
import com.soda.component.domain.types.CredentialHash;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.RawCredential;
import com.soda.component.domain.types.Sex;
import com.soda.user.domain.event.PasswordChangedEvent;
import com.soda.user.domain.event.UserCreatedEvent;
import com.soda.user.domain.event.UserStateChangedEvent;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.domain.types.VerificationStatus;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 用户聚合根 — 核心业务实体，管理用户身份信息和认证账户集合。
 * <p>
 * 创建时通过 {@link #createBuilder()} 构建，不含 ID（服务端生成）；
 * 持久化恢复通过 {@link #restoreBuilder()}。
 *
 * @see Aggregate
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class User extends Aggregate<UserId> {

    private Username username;
    private Nickname nickname;
    private UserState state;
    private @Nullable Mobile mobile;
    private @Nullable Email email;
    private @Nullable Sex sex;
    private @Nullable Avatar avatar;
    private List<AuthAccount<?>> accounts;

    // ─── 唯一构造器（@JsonCreator + 创建/恢复共用）───

    /**
     * @param id 服务端分配后的 ID；null 表示尚未持久化（创建路径）
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    private User(@JsonProperty("id") @Nullable UserId id, @JsonProperty("username") Username username, @JsonProperty("nickname") Nickname nickname, @JsonProperty("state") UserState state, @JsonProperty("mobile") @Nullable Mobile mobile, @JsonProperty("email") @Nullable Email email, @JsonProperty("sex") @Nullable Sex sex, @JsonProperty("avatar") @Nullable Avatar avatar, @JsonProperty("accounts") @Nullable List<AuthAccount<?>> accounts) {
        super();
        if (id != null) {
            assignId(id);
        }
        this.username = Objects.requireNonNull(username);
        this.nickname = Objects.requireNonNull(nickname);
        this.state = Objects.requireNonNull(state);
        this.mobile = mobile;
        this.email = email;
        this.sex = sex;
        this.avatar = avatar;
        this.accounts = new LinkedList<>(Objects.requireNonNullElse(accounts, List.of()));
    }

    // ─── 创建 builder（public，只暴露业务字段）───

    /**
     * 创建新用户。
     * <p>
     * 生成的 User 不含 ID（由 Repository save 后 {@link #assignId} 填补），
     * 必传密码创建 {@link PasswordAuthAccount}；传入 mobile / email 时分别追加
     * {@link SmsAuthAccount} / {@link EmailAuthAccount} 到账户列表。
     * 注册 {@link UserCreatedEvent}（entityId 在 flush 时延迟求值）。
     *
     * @param passwordHash 必传密码哈希，自动创建 {@link PasswordAuthAccount}
     */
    @Builder(builderClassName = "UserCreationBuilder", builderMethodName = "createBuilder")
    private static User create(Username username, Nickname nickname, @Nullable Mobile mobile, @Nullable Email email, @Nullable Sex sex, @Nullable Avatar avatar, CredentialHash passwordHash) {
        var user = new User(null, username, nickname, UserState.E, mobile, email, sex, avatar, null);
        user.addAccount(PasswordAuthAccount.createBuilder().passwordHash(passwordHash).build());
        if (mobile != null) {
            user.addAccount(SmsAuthAccount.createBuilder().mobile(mobile).build());
        }
        if (email != null) {
            user.addAccount(EmailAuthAccount.createBuilder().email(email).build());
        }
        user.registerEvent(new UserCreatedEvent(user));
        return user;
    }

    // ─── 恢复 builder（public，暴露全部持久化字段）───

    /**
     * 从持久化数据恢复 User，不触发事件。
     */
    @Builder(builderClassName = "UserRestoreBuilder", builderMethodName = "restoreBuilder")
    private static User restore(UserId id, Username username, Nickname nickname, UserState state, @Nullable Mobile mobile, @Nullable Email email, @Nullable Sex sex, @Nullable Avatar avatar, @Nullable List<AuthAccount<?>> accounts) {
        Objects.requireNonNull(id);
        return new User(id, username, nickname, state, mobile, email, sex, avatar, accounts);
    }

    // ─── accessors ───

    /**
     * 返回账户列表的不可修改视图。
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
     * 添加认证账户到用户聚合。
     */
    protected void addAccount(AuthAccount<?> account) {
        Objects.requireNonNull(account);
        if (accounts.contains(account)) {
            return;
        }
        Assert.isTrue(account.isActive(), "Account should be active.");
        Assert.isTrue(findAccount(account::typeEquals).isEmpty(), "Account already exists.");
        this.accounts.add(account);
    }

    protected void removeAccount(AuthAccountType accountType) {
        Objects.requireNonNull(accountType);
        this.accounts.removeIf(account -> Objects.equals(account.getAuthAccountType(), accountType));
    }

    // ─── 属性修改 ───

    /**
     * 修改用户名。
     */
    public void changeUsername(Username newUsername) {
        this.username = Objects.requireNonNull(newUsername);
    }

    /**
     * 修改昵称。
     */
    public void changeNickname(Nickname nickname) {
        this.nickname = Objects.requireNonNull(nickname);
    }

    /**
     * 修改性别。
     */
    public void changeSex(@Nullable Sex sex) {
        this.sex = sex;
    }

    /**
     * 修改头像。
     */
    public void changeAvatar(@Nullable Avatar avatar) {
        this.avatar = avatar;
    }

    /**
     * 修改密码 — 委托到 {@link PasswordAuthAccount#changePassword}，注册 {@link PasswordChangedEvent}。
     *
     * @param credential 原始密码
     * @param hasher     凭证哈希器
     * @throws NoSuchElementException 当用户没有激活的密码账户时
     */
    public void changePassword(RawCredential credential, CredentialHasher hasher) {
        var account = findAccount(existed -> AuthAccountType.P.equals(existed.getAuthAccountType()) && existed.isActive()).map(PasswordAuthAccount.class::cast).orElseThrow();
        account.changePassword(credential, hasher);
        registerEvent(new PasswordChangedEvent(requireId()));
    }

    /**
     * 修改手机号（需要短信验证）。
     * <p>
     * 业务规则：
     * <ul>
     *   <li>验证必须处于 VERIFIED 状态（USED 为终态，不可重放）</li>
     *   <li>不能修改为相同的手机号</li>
     *   <li>更新 User.mobile 字段</li>
     *   <li>替换旧 SmsAuthAccount 为新账户</li>
     * </ul>
     *
     * @param verification 已通过的短信验证聚合（外部聚合，只读守卫）
     */
    public void changeMobile(SmsVerification verification) {
        Objects.requireNonNull(verification);
        Assert.isTrue(Objects.equals(verification.getUserId(), requireId().toLongId()), "verification userId must match this user");
        Assert.isTrue(Objects.equals(verification.getStatus(), VerificationStatus.V), "verification must be verified");
        var newMobile = verification.getTarget();
        Assert.isTrue(!Objects.equals(this.mobile, newMobile), "cannot change to the same mobile");
        this.mobile = newMobile;
        // 替换 SmsAuthAccount：移除旧账户，添加新账户
        this.removeAccount(SmsAuthAccountId.ACCOUNT_TYPE);
        this.addAccount(SmsAuthAccount.createBuilder().mobile(newMobile).build());
    }

    /**
     * 修改邮箱（需要邮箱验证）。
     * <p>
     * 业务规则：
     * <ul>
     *   <li>验证必须处于 VERIFIED 状态（USED 为终态，不可重放）</li>
     *   <li>不能修改为相同的邮箱</li>
     *   <li>更新 User.email 字段</li>
     *   <li>替换旧 EmailAuthAccount 为新账户</li>
     * </ul>
     *
     * @param verification 已通过的邮箱验证聚合（外部聚合，只读守卫）
     */
    public void changeEmail(EmailVerification verification) {
        Objects.requireNonNull(verification);
        Assert.isTrue(Objects.equals(verification.getUserId(), requireId().toLongId()), "verification userId must match this user");
        Assert.isTrue(Objects.equals(verification.getStatus(), VerificationStatus.V), "verification must be verified");
        var newEmail = verification.getTarget();
        Assert.isTrue(!Objects.equals(this.email, newEmail), "cannot change to the same email");
        this.email = newEmail;
        // 替换 EmailAuthAccount：移除旧账户，添加新账户
        this.removeAccount(EmailAuthAccountId.ACCOUNT_TYPE);
        this.addAccount(EmailAuthAccount.createBuilder().email(newEmail).build());
    }

    // ─── 状态切换 ───

    /**
     * 禁用用户。E→D 时注册 {@link UserStateChangedEvent}；已是 D 则 no-op。
     */
    public void disable() {
        if (UserState.D.equals(this.state)) {
            return;
        }
        var oldState = this.state;
        this.state = UserState.D;
        registerEvent(new UserStateChangedEvent(requireId(), oldState, UserState.D));
    }

    /**
     * 启用用户。D→E 时注册 {@link UserStateChangedEvent}；已是 E 则 no-op。
     */
    public void enable() {
        if (UserState.E.equals(this.state)) {
            return;
        }
        var oldState = this.state;
        this.state = UserState.E;
        registerEvent(new UserStateChangedEvent(requireId(), oldState, UserState.E));
    }
}
