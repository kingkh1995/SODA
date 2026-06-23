package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.SocialType;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 社交认证账户标识符 DP — 派生自 {@link SocialType} + openId。
 * <p>
 * 值 = {@code "O:{socialType}:{openId}"}（如 {@code "O:GE:12345"}），统一 {@link AuthAccountId} 格式。
 *
 * @see AuthAccountId
 */
@Getter
@Accessors(fluent = true)
public final class SocialAuthAccountId extends AuthAccountId implements Comparable<SocialAuthAccountId> {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final AuthAccountType ACCOUNT_TYPE = AuthAccountType.O;
    private static final String PREFIX = ACCOUNT_TYPE.name() + ":";

    private final SocialType socialType;
    private final String openId;

    private SocialAuthAccountId(String value, SocialType socialType, String openId) {
        super(value);
        ValidateUtils.nonBlank(openId);
        this.socialType = socialType;
        this.openId = openId;
    }

    /** 反序列化入口 — 格式 {@code "O:{socialType}:{openId}"}。 */
    @JsonCreator
    public static SocialAuthAccountId of(String value) {
        var suffix = requirePrefixed(value, PREFIX);
        var colonIndex = suffix.indexOf(':');
        if (colonIndex < 0) {
            throw new IllegalArgumentException("invalid social auth account id format: " + value);
        }
        var socialType = ParseUtils.parseEnum(SocialType.class, suffix.substring(0, colonIndex));
        var openId = suffix.substring(colonIndex + 1);
        return new SocialAuthAccountId(value, socialType, openId);
    }

    /** 从 {@link SocialType} + openId 构造社交认证账户标识符。 */
    public static SocialAuthAccountId from(SocialType socialType, String openId) {
        ValidateUtils.notNull(socialType);
        ValidateUtils.nonBlank(openId);
        return new SocialAuthAccountId(PREFIX + socialType.name() + ":" + openId, socialType, openId);
    }

    @Override
    public AuthAccountType authAccountType() {
        return ACCOUNT_TYPE;
    }

    @Override
    public int compareTo(SocialAuthAccountId other) {
        return this.value().compareTo(other.value());
    }
}
