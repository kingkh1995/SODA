package com.soda.user.domain.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.soda.component.domain.util.ParseUtils;
import com.soda.component.domain.util.ValidateUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 社交认证账户标识符 DP — 派生自 {@link SocialType} + openId。
 * <p>
 * 格式：{@code "O:{社交类型短名}:{openId}"}（如 {@code "O:W:open123"}、{@code "O:A:456"}）。
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
    public static SocialAuthAccountId of(String value) {
        var suffix = ParseUtils.cutPrefix(value, PREFIX);
        var socialParts = ParseUtils.splitPair(suffix, AuthAccountId.DELIMITER);
        var socialType = ParseUtils.parseEnum(SocialType.class, socialParts[0]);
        var openId = socialParts[1];
        ValidateUtils.hasText(openId);
        return new SocialAuthAccountId(value, socialType, openId);
    }

    /**
     * 从 {@link SocialType} + openId 构造社交认证账户标识符。
     */
    public static SocialAuthAccountId from(SocialType socialType, String openId) {
        ValidateUtils.notNull(socialType);
        ValidateUtils.hasText(openId);
        return new SocialAuthAccountId(
                PREFIX + socialType.name() + AuthAccountId.DELIMITER + openId,
                socialType,
                openId);
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
