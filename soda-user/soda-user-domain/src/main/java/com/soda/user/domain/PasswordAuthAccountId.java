package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.support.types.LongId;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 密码认证账户标识符 DP — 派生自 {@link UserId}。
 * <p>
 * 值 = {@code "P:{userId}"}（如 {@code "P:42"}），统一 {@link AuthAccountId} 格式。
 *
 * @see AuthAccountId
 */
@Getter
@Accessors(fluent = true)
public final class PasswordAuthAccountId extends AuthAccountId implements Comparable<PasswordAuthAccountId> {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final AuthAccountType ACCOUNT_TYPE = AuthAccountType.P;
    private static final String PREFIX = ACCOUNT_TYPE.name() + ":";

    private final UserId userId;

    private PasswordAuthAccountId(String value, UserId userId) {
        super(value);
        this.userId = userId;
    }

    @JsonCreator
    public PasswordAuthAccountId(String value) {
        super(value);
        ValidateUtils.hasPrefix(PREFIX, value);
        var suffix = value.substring(PREFIX.length());
        this.userId = new UserId(ParseUtils.parseLong(suffix));
    }


    @Override
    public AuthAccountType authAccountType() {
        return ACCOUNT_TYPE;
    }

    /** 从 {@link UserId} 构造密码认证账户标识符。 */
    public static PasswordAuthAccountId from(UserId userId) {
        ValidateUtils.notNull(userId);
        return new PasswordAuthAccountId(PREFIX + userId.value(), userId);
    }

    /** 从不可靠输入构造，格式 {@code "P:{userId}"}。 */
    public static PasswordAuthAccountId valueOf(Object value) {
        return new PasswordAuthAccountId(ParseUtils.parseString(value));
    }

    /** 返回底层 {@link UserId} 的 {@link LongId} 表示。 */
    public LongId toLongId() {
        return LongId.valueOf(userId.value());
    }

    @Override
    public int compareTo(PasswordAuthAccountId other) {
        return this.value().compareTo(other.value());
    }
}
