package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 密码认证账户标识符 DP — 派生自 {@link UserId}。
 * <p>
 * 值 = {@code "P:{userId}"}（如 {@code "P:42"}），统一 {@link AuthAccountId} 格式。
 * <p>
 * payload 即 {@link UserId} 本身，故 {@code of(String)} 与 {@code from(UserId)} 是同一派生，各自直达私有构造器。
 *
 * @see AuthAccountId
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
@Accessors(fluent = true)
public final class PasswordAuthAccountId extends AuthAccountId implements Comparable<PasswordAuthAccountId> {

    private final UserId userId;

    private PasswordAuthAccountId(UserId userId) {
        super(String.valueOf(userId.value()));
        this.userId = userId;
    }

    /**
     * 反序列化入口 — 格式 {@code "P:{userId}"}。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PasswordAuthAccountId of(String value) {
        var suffix = ParseUtils.cutPrefix(value, prefix(AuthAccountType.P));
        return new PasswordAuthAccountId(new UserId(ParseUtils.parseLong(suffix)));
    }

    /**
     * 从 {@link UserId} 构造。
     */
    public static PasswordAuthAccountId from(UserId userId) {
        ValidateUtils.notNull(userId);
        return new PasswordAuthAccountId(userId);
    }

    @Override
    public AuthAccountType accountType() {
        return AuthAccountType.P;
    }

    @Override
    public int compareTo(PasswordAuthAccountId other) {
        return value().compareTo(other.value());
    }
}
