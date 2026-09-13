package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.types.Email;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 邮箱认证账户标识符 DP — 派生自 {@link Email}。
 * <p>
 * 值 = {@code "E:{email}"}（如 {@code "E:user@example.com"}），统一 {@link AuthAccountId} 格式。
 * <p>
 * payload 即 {@link Email} 本身（构造期已归一化为小写），故 {@code of(String)} 与 {@code from(Email)} 是同一派生，
 * 各自直达私有构造器——线形态入参的大小写写法不影响规范串。
 *
 * @see AuthAccountId
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
@Accessors(fluent = true)
public final class EmailAuthAccountId extends AuthAccountId implements Comparable<EmailAuthAccountId> {

    private final Email email;

    private EmailAuthAccountId(Email email) {
        super(email.value());
        this.email = email;
    }

    /**
     * 反序列化入口 — 格式 {@code "E:{email}"}。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EmailAuthAccountId of(String value) {
        var suffix = ParseUtils.cutPrefix(value, prefix(AuthAccountType.E));
        return new EmailAuthAccountId(Email.of(suffix));
    }

    /**
     * 从 {@link Email} 构造。
     */
    public static EmailAuthAccountId from(Email email) {
        ValidateUtils.notNull(email);
        return new EmailAuthAccountId(email);
    }

    @Override
    public AuthAccountType accountType() {
        return AuthAccountType.E;
    }

    @Override
    public int compareTo(EmailAuthAccountId other) {
        return value().compareTo(other.value());
    }
}
