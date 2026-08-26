package com.soda.user.infrastructure.persistence;

import com.soda.component.infrastructure.persistence.AbstractAuditable;
import com.soda.user.infrastructure.gateway.persistence.UserGatewayImpl;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * {@code user} 表 JPA 实体 — 持久化模型，非领域对象。
 * <p>
 * 与领域 {@code User} 聚合双向转换见 {@link UserGatewayImpl}。Sms/Email 账户不落表，
 * 由 {@code mobile}/{@code email} 列派生（ADR-0004 单表化）；登录开关
 * （{@code sms_login_enabled}/{@code email_login_enabled}）表达账户 active
 * （支付宝/阿里云模式：手机号/邮箱仍是账号标识，开关仅表达登录方式可用性）。
 * <p>
 * {@code version} 用 JPA {@code @Version}：乐观锁校验 + 自动递增，正合领域层
 * 「递增由基础设施层负责」的注释契约（IDDD ConcurrencySafeEntity 同款）。
 * 审计字段 {@code created_date}/{@code last_modified_date} 由 Spring Data auditing 维护
 * （{@code @CreatedDate}/{@code @LastModifiedDate} + {@code AuditingEntityListener}，
 * 见 AbstractAuditable；UTC 字面值，无 DB 默认值）。
 */
@Entity
@Table(
        name = "`user`",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_mobile", columnNames = "mobile"),
                @UniqueConstraint(name = "uk_email", columnNames = "email")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class UserPO extends AbstractAuditable<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名（全局唯一，可空——注销终态 R 行键释放后置 NULL，恢复时领域补
     * {@code Username.REMOVED} 默认值，ADR-0023）。
     */
    @Column(length = 30)
    private @Nullable String username;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(length = 20)
    private @Nullable String mobile;

    @Column(length = 100)
    private @Nullable String email;

    @Column(length = 1)
    private @Nullable String sex;

    @Column(length = 500)
    private @Nullable String avatar;

    @Column(nullable = false, length = 1)
    private String state;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "sms_login_enabled", nullable = false)
    private boolean smsLoginEnabled = true;

    @Column(name = "email_login_enabled", nullable = false)
    private boolean emailLoginEnabled = true;

    /**
     * 乐观锁版本（JPA {@code @Version} 自动校验递增）— 领域 User 携带版本令牌
     * （{@code User.version}，加载后跨读改写间隙保持），convertor 原样带入，
     * merge 以 {@code WHERE version = 领域版本} 检测并发覆盖（User 独有的承重乐观锁；
     * Verification 无领域版本令牌，不加 version——见 ADR-0024）。
     */
    @Version
    @Column(nullable = false)
    private Integer version;
}
