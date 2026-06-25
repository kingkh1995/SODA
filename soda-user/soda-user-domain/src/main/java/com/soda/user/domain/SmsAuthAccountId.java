package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.support.types.Mobile;
import com.soda.component.support.util.ValidateUtils;
import com.soda.user.domain.enums.AuthAccountType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 短信认证账户标识符 DP — 派生自 {@link Mobile}。
 * <p>
 * 值 = {@code "S:{mobile}"}（如 {@code "S:13800138000"}），统一 {@link AuthAccountId} 格式。
 *
 * @see AuthAccountId
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Accessors(fluent = true)
public final class SmsAuthAccountId extends AuthAccountId implements Comparable<SmsAuthAccountId> {

    public static final AuthAccountType ACCOUNT_TYPE = AuthAccountType.S;
    private static final String PREFIX = ACCOUNT_TYPE.name() + AuthAccountId.DELIMITER;

    private final Mobile mobile;

    private SmsAuthAccountId(String value, Mobile mobile) {
        super(value);
        this.mobile = mobile;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    /** 反序列化入口 — 格式 {@code "S:{mobile}"}。 */
    public static SmsAuthAccountId of(String value) {
        ValidateUtils.hasPrefix(PREFIX, value);
        var suffix = value.substring(PREFIX.length());
        return new SmsAuthAccountId(value, new Mobile(suffix));
    }

    /** 从 {@link Mobile} 构造短信认证账户标识符。 */
    public static SmsAuthAccountId from(Mobile mobile) {
        ValidateUtils.notNull(mobile);
        return new SmsAuthAccountId(PREFIX + mobile.value(), mobile);
    }

    @Override
    public AuthAccountType authAccountType() {
        return ACCOUNT_TYPE;
    }

    @Override
    public int compareTo(SmsAuthAccountId other) {
        return this.value().compareTo(other.value());
    }
}
