package com.soda.user.infrastructure.convertor;

import com.soda.component.domain.types.Active;
import com.soda.component.domain.types.ConcurrencyVersion;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.types.PasswordHash;
import com.soda.component.domain.types.Sex;
import com.soda.user.domain.AuthAccount;
import com.soda.user.domain.EmailAuthAccount;
import com.soda.user.domain.PasswordAuthAccount;
import com.soda.user.domain.SmsAuthAccount;
import com.soda.user.domain.User;
import com.soda.user.domain.types.AuthAccountType;
import com.soda.user.domain.types.Avatar;
import com.soda.user.domain.types.EmailAuthAccountId;
import com.soda.user.domain.types.Nickname;
import com.soda.user.domain.types.PasswordAuthAccountId;
import com.soda.user.domain.types.SmsAuthAccountId;
import com.soda.user.domain.types.UserId;
import com.soda.user.domain.types.UserState;
import com.soda.user.domain.types.Username;
import com.soda.user.infrastructure.persistence.UserPO;

import java.util.ArrayList;
import java.util.Optional;

/**
 * {@link User} 聚合 ↔ {@link UserPO} 双向转换（COLA 惯例：infrastructure 独立 convertor）。
 * <p>
 * ADR-0004 单表化：账户不落表。恢复时 {@code password_hash} 列 →
 * {@link PasswordAuthAccount}（独立必填字段），{@code mobile}/{@code email} 非空 →
 * 派生 {@link SmsAuthAccount}/{@link EmailAuthAccount}。
 * 登录开关（{@code sms_login_enabled}/{@code email_login_enabled}）表达账户 active
 * （支付宝/阿里云模式：手机号/邮箱仍是账号标识，开关仅表达登录方式可用性）。
 * <p>
 * 注销键释放（ADR-0023）：R 行三键（username/mobile/email）由 {@code UserGatewayImpl.save}
 * 置空——{@code username} 列为空时恢复为领域默认值 {@link Username#REMOVED}（该值只存在于
 * 领域内存，从不落库）；{@code mobile}/{@code email} 经 Optional 链式转换，null → Optional.empty。
 * <p>
 * 审计列（{@code created_date}/{@code last_modified_date}）属基础设施表示，不入领域聚合
 * （ADR-0031）——两方向均不搬运，列值由 Spring Data auditing 全权维护。
 */
public final class UserConvertor {

    private UserConvertor() {
        // 工具类
    }

    /**
     * 持久化行 → 领域聚合（全参数恢复构造器，version 随持久化数据流转）。
     * <p>
     * 可空列（mobile/email/sex/avatar）经 Optional 链式转换，null → 领域 Optional.empty；
     * {@code username} 为空（注销终态键释放）→ {@link Username#REMOVED}（ADR-0023）。
     */
    public static User toDomain(UserPO e) {
        var userId = new UserId(e.getId());
        var passwordAccount = PasswordAuthAccount.builder()
                .id(PasswordAuthAccountId.from(userId))
                .active(Active.TRUE)
                .passwordHash(PasswordHash.of(e.getPasswordHash()))
                .build();
        var accounts = new ArrayList<AuthAccount<?>>();
        if (e.getMobile() != null) {
            accounts.add(SmsAuthAccount.builder()
                    .id(SmsAuthAccountId.from(Mobile.of(e.getMobile())))
                    .active(Active.of(e.isSmsLoginEnabled()))
                    .build());
        }
        if (e.getEmail() != null) {
            accounts.add(EmailAuthAccount.builder()
                    .id(EmailAuthAccountId.from(Email.of(e.getEmail())))
                    .active(Active.of(e.isEmailLoginEnabled()))
                    .build());
        }
        return User.builder()
                .id(userId)
                .version(ConcurrencyVersion.of(e.getVersion()))
                .username(e.getUsername() == null ? Username.REMOVED : new Username(e.getUsername()))
                .nickname(new Nickname(e.getNickname()))
                .state(UserState.of(e.getState()))
                .mobile(Optional.ofNullable(e.getMobile()).map(Mobile::of).orElse(null))
                .email(Optional.ofNullable(e.getEmail()).map(Email::of).orElse(null))
                .sex(Optional.ofNullable(e.getSex()).map(Sex::of).orElse(null))
                .avatar(Optional.ofNullable(e.getAvatar()).map(Avatar::new).orElse(null))
                .passwordAccount(passwordAccount)
                .accounts(accounts)
                .build();
    }

    /**
     * 领域聚合 → 持久化行（全量构造，创建与更新路径共用——id==null → persist（INSERT）；
     * id 有值 → merge 按行存在性路由（UPDATE），乐观锁由 {@code @Version} 校验，见 ADR-0024）。
     * <p>
     * 全量构造：领域对象是行的唯一事实源，逐列赋值（含 null——可空列 mobile/email/sex/avatar
     * 领域为空 → 显式 NULL 清空列）。version 原样带入（乐观锁令牌：领域「递增由基础设施层
     * 负责」契约，JPA {@code @Version} 校验递增）；Sms/Email 登录开关从对应账户的 active
     * 读取。id 可空（服务端生成 id 的创建路径：id==null → isNew=true → persist，
     * insert 后经 {@code assignId} 回填）。
     * <p>
     * 审计列（{@code created_date}/{@code last_modified_date}）不在此构造（null 即可）——
     * 由 Spring Data auditing + {@code updatable=false} 自动处理（见 ADR-0024）：
     * created_date 不进 UPDATE、last_modified_date 由 {@code @PreUpdate} 刷新，merge 的 null
     * 不会覆盖审计列。
     */
    public static UserPO toPersistence(User user) {
        var entity = new UserPO();
        if (user.isIdentified()) {
            entity.setId(user.getId().value());
        }
        entity.setUsername(user.getUsername().value());
        entity.setNickname(user.getNickname().value());
        entity.setState(user.getState().name());
        entity.setPasswordHash(user.getPasswordAccount().getPasswordHash().value());
        entity.setVersion(user.getVersion().value());
        entity.setMobile(user.getMobile().map(Mobile::value).orElse(null));
        entity.setEmail(user.getEmail().map(Email::value).orElse(null));
        entity.setSex(user.getSex().map(Sex::name).orElse(null));
        entity.setAvatar(user.getAvatar().map(Avatar::value).orElse(null));
        entity.setSmsLoginEnabled(activeOf(user, SmsAuthAccountId.ACCOUNT_TYPE));
        entity.setEmailLoginEnabled(activeOf(user, EmailAuthAccountId.ACCOUNT_TYPE));
        return entity;
    }

    /**
     * 从用户聚合中按账户类型取 active（登录开关）；账户不存在（未绑定该登录方式）默认启用。
     */
    private static boolean activeOf(User user, AuthAccountType accountType) {
        return user.getAccounts().stream()
                .filter(a -> a.getAccountType().equals(accountType))
                .findFirst()
                .map(AuthAccount::isActive)
                .orElse(true);
    }
}
