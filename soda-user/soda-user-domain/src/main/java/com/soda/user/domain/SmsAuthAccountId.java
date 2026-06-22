package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.support.types.Mobile;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 短信认证账户标识符 DP — 派生自 {@link Mobile}。
 * <p>
 * 值 = {@code "S:{mobile}"}（如 {@code "S:13800138000"}），统一 {@link AuthAccountId} 格式。
 *
 * @see AuthAccountId
 */
@Getter
@Accessors(fluent = true)
public final class SmsAuthAccountId extends AuthAccountId implements Comparable<SmsAuthAccountId> {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final AuthAccountType ACCOUNT_TYPE = AuthAccountType.S;
    private static final String PREFIX = ACCOUNT_TYPE.name() + ":";

    private final Mobile mobile;

    private SmsAuthAccountId(String value, Mobile mobile) {
        super(value);
        this.mobile = mobile;
    }

    @JsonCreator
    public SmsAuthAccountId(String value) {
        super(value);
        ValidateUtils.hasPrefix(PREFIX, value);
        var suffix = value.substring(PREFIX.length());
        this.mobile = new Mobile(suffix);
    }

    @Override
    public AuthAccountType authAccountType() {
        return ACCOUNT_TYPE;
    }

    /** 从 {@link Mobile} 构造短信认证账户标识符。 */
    public static SmsAuthAccountId from(Mobile mobile) {
        ValidateUtils.notNull(mobile);
        return new SmsAuthAccountId(PREFIX + mobile.value(), mobile);
    }

    /** 从不可靠输入构造，格式 {@code "S:{mobile}"}。 */
    public static SmsAuthAccountId valueOf(Object value) {
        return new SmsAuthAccountId(ParseUtils.parseString(value));
    }

    @Override
    public int compareTo(SmsAuthAccountId other) {
        return this.value().compareTo(other.value());
    }
}
