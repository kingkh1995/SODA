package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.support.types.Email;
import com.soda.component.support.util.ValidateUtils;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 邮箱认证账户标识符 DP — 派生自 {@link Email}。
 * <p>
 * 值 = {@code "E:{email}"}（如 {@code "E:user@example.com"}），统一 {@link AuthAccountId} 格式。
 *
 * @see AuthAccountId
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Accessors(fluent = true)
public final class EmailAuthAccountId extends AuthAccountId implements Comparable<EmailAuthAccountId> {

    public static final AuthAccountType ACCOUNT_TYPE = AuthAccountType.E;
    private static final String PREFIX = ACCOUNT_TYPE.name() + AuthAccountId.DELIMITER;

    private final Email email;

    private EmailAuthAccountId(String value, Email email) {
        super(value);
        this.email = email;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    /** 反序列化入口 — 格式 {@code "E:{email}"}。 */
    public static EmailAuthAccountId of(String value) {
        ValidateUtils.hasPrefix(PREFIX, value);
        var suffix = value.substring(PREFIX.length());
        return new EmailAuthAccountId(value, new Email(suffix));
    }

    /**
     * 从 {@link Email} 构造邮箱认证账户标识符。
     */
    public static EmailAuthAccountId from(Email email) {
        ValidateUtils.notNull(email);
        return new EmailAuthAccountId(PREFIX + email.value(), email);
    }

    @Override
    public AuthAccountType authAccountType() {
        return ACCOUNT_TYPE;
    }

    @Override
    public int compareTo(EmailAuthAccountId other) {
        return this.value().compareTo(other.value());
    }
}
