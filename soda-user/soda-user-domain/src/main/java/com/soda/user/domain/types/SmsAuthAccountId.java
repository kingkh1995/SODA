package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.types.Mobile;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;
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
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
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

    /**
     * 反序列化入口 — 格式 {@code "S:{mobile}"}。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SmsAuthAccountId of(String value) {
        var suffix = ParseUtils.cutPrefix(value, PREFIX);
        return new SmsAuthAccountId(value, Mobile.of(suffix));
    }

    public static SmsAuthAccountId from(Mobile mobile) {
        ValidateUtils.notNull(mobile);
        return new SmsAuthAccountId(PREFIX + mobile.value(), mobile);
    }

    @Override
    public int compareTo(SmsAuthAccountId other) {
        return value().compareTo(other.value());
    }
}
