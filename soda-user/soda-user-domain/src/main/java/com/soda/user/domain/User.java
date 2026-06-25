package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soda.component.domain.Aggregate;
import com.soda.component.support.gateway.CredentialHasher;
import com.soda.component.support.types.Email;
import com.soda.component.support.types.Mobile;
import com.soda.component.support.types.RawCredential;
import com.soda.component.support.types.RandomString;
import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.Sex;
import com.soda.user.domain.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
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
 * @see AuthAccount
 */
@Getter
public class User extends Aggregate<UserId> {

    @Getter private Username username;
    @Getter private Nickname nickname;
    @Getter private UserStatus status;
    private @Nullable Mobile mobile;
    private @Nullable Email email;
    private @Nullable Sex sex;
    private @Nullable Avatar avatar;
    private @Nullable List<AuthAccount<?>> accounts;

    // ─── 唯一构造器（@JsonCreator + 创建/恢复共用）───

    /** @param id 服务端分配后的 ID；null 表示尚未持久化（创建路径） */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    protected User(
            @JsonProperty("id") @Nullable UserId id,
            @JsonProperty("username") Username username,
            @JsonProperty("nickname") Nickname nickname,
            @JsonProperty("status") UserStatus status,
            @JsonProperty("mobile") @Nullable Mobile mobile,
            @JsonProperty("email") @Nullable Email email,
            @JsonProperty("sex") @Nullable Sex sex,
            @JsonProperty("avatar") @Nullable Avatar avatar,
            @JsonProperty("accounts") @Nullable List<AuthAccount<?>> accounts) {
        super();
        if (id != null) {
            assignId(id);
        }
        this.username = Objects.requireNonNull(username);
        this.nickname = Objects.requireNonNull(nickname);
        this.status = Objects.requireNonNull(status);
        this.mobile = mobile;
        this.email = email;
        this.sex = sex;
        this.avatar = avatar;
        this.accounts = accounts;
    }

    // ─── 创建 builder（public，只暴露业务字段）───

    /**
     * 创建新用户。
     * <p>
     * 生成的 User 不含 ID（由 Repository save 后 {@link #assignId} 填补），
     * 不包含任何 Account。注册 {@link UserCreatedEvent}（entityId 在 flush 时延迟求值）。
     */
    @Builder(builderClassName = "UserCreationBuilder", builderMethodName = "createBuilder")
    public static User create(Username username, Nickname nickname,
                              @Nullable Mobile mobile, @Nullable Email email,
                              @Nullable Sex sex, @Nullable Avatar avatar) {
        var user = new User(null, username, nickname, UserStatus.E, mobile, email, sex, avatar, null);
        user.registerEvent(new UserCreatedEvent(user));
        return user;
    }

    // ─── 恢复 builder（public，暴露全部持久化字段）───

    /** 从持久化数据恢复 User，不触发事件。 */
    @Builder(builderClassName = "UserRestoreBuilder", builderMethodName = "restoreBuilder")
    public static User restore(UserId id, Username username, Nickname nickname,
                               UserStatus status,
                               @Nullable Mobile mobile, @Nullable Email email,
                               @Nullable Sex sex, @Nullable Avatar avatar,
                               List<AuthAccount<?>> accounts) {
        return new User(id, username, nickname, status, mobile, email, sex, avatar,
                accounts != null ? new LinkedList<>(accounts) : null);
    }

    // ─── accessors ───

    /** 返回账户列表的不可修改视图。 */
    public List<AuthAccount<?>> getAccounts() {
        return accounts != null ? List.copyOf(accounts) : List.of();
    }

    public Optional<Mobile> getMobile() { return Optional.ofNullable(mobile); }
    public Optional<Email> getEmail() { return Optional.ofNullable(email); }
    public Optional<Sex> getSex() { return Optional.ofNullable(sex); }
    public Optional<Avatar> getAvatar() { return Optional.ofNullable(avatar); }

    /** 查找第一个匹配条件的账户。 */
    public Optional<AuthAccount<?>> findAccount(Predicate<AuthAccount<?>> filter) {
        if (accounts == null) {
            return Optional.empty();
        }
        return accounts.stream()
                .filter(filter)
                .findFirst();
    }

    // ─── queries ───

    /**
     * 认证 — 根据账户类型分发到对应子类验证。
     *
     * @param type            认证类型
     * @param rawCredential   原始凭证串（密码 / 验证码）
     * @param credentialHasher 凭证哈希器（仅用于 PasswordAuthAccount 验证）
     * @return 验证成功返回 true
     */
    public boolean authenticate(AuthAccountType type, String rawCredential, CredentialHasher credentialHasher) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(rawCredential);
        Objects.requireNonNull(credentialHasher);
        return findAccount(AuthAccount.ofType(type).and(AuthAccount.ACTIVE))
                .map(account -> switch (account) {
                    case PasswordAuthAccount pa -> pa.verify(new RawCredential(rawCredential), credentialHasher);
                    case SmsAuthAccount sa -> sa.verifyCode(new RandomString(rawCredential));
                    case EmailAuthAccount ea -> ea.verifyCode(new RandomString(rawCredential));
                    case SocialAuthAccount sa -> false;
                    default -> false;
                })
                .orElse(false);
}
}
