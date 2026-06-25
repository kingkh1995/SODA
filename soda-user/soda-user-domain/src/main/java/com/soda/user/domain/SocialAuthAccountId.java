package com.soda.user.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.support.util.IllegalArgumentExceptions;
import com.soda.component.support.util.ParseUtils;
import com.soda.component.support.util.ValidateUtils;
import com.soda.user.domain.enums.AuthAccountType;
import com.soda.user.domain.enums.SocialType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;


/**
 * 社交认证账户标识符 DP — 派生自 {@link SocialType} + openId。
 * <p>
 * 值 = {@code "O:{socialType}:{openId}"}（如 {@code "O:GE:12345"}），统一 {@link AuthAccountId} 格式。
 *
 * @see AuthAccountId
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Getter
@Accessors(fluent = true)
public final class SocialAuthAccountId extends AuthAccountId implements Comparable<SocialAuthAccountId> {


    public static final AuthAccountType ACCOUNT_TYPE = AuthAccountType.O;
    private static final String PREFIX = ACCOUNT_TYPE.name() + AuthAccountId.DELIMITER;

    private final SocialType socialType;
    private final String openId;

    private SocialAuthAccountId(String value, SocialType socialType, String openId) {
        super(value);
        this.socialType = socialType;
        this.openId = openId;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    /** 反序列化入口 — 格式 {@code "O:{socialType}:{openId}"}。 */
    public static SocialAuthAccountId of(String value) {
        ValidateUtils.hasPrefix(PREFIX, value);
        var suffix = value.substring(PREFIX.length());
        var colonIndex = suffix.indexOf(AuthAccountId.DELIMITER);
        if (colonIndex < 0) {
            throw IllegalArgumentExceptions.forInvalidFormat(value);
        }
        var socialType = ParseUtils.parseEnum(SocialType.class, suffix.substring(0, colonIndex));
        var openId = suffix.substring(colonIndex + 1);
        ValidateUtils.nonBlank(openId);
        return new SocialAuthAccountId(value, socialType, openId);
    }

    /** 从 {@link SocialType} + openId 构造社交认证账户标识符。 */
    public static SocialAuthAccountId from(SocialType socialType, String openId) {
        ValidateUtils.notNull(socialType);
        ValidateUtils.nonBlank(openId);
        return new SocialAuthAccountId(PREFIX + socialType.name() + AuthAccountId.DELIMITER + openId, socialType, openId);
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
