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
 * 格式：{@code "O:{社交类型短名}:{openId}"}（如 {@code "O:GE:open123"}、{@code "O:DT:456"}）。
 * <p>
 * payload 是两段组合（{@link SocialType} 短名 + openId），故 {@code of(String)} 的拆分与
 * {@code from(SocialType, String)} 的装配是同一派生的两个方向，各自直达私有构造器。
 *
 * @see AuthAccountId
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Getter
@Accessors(fluent = true)
public final class SocialAuthAccountId extends AuthAccountId implements Comparable<SocialAuthAccountId> {

    private final SocialType socialType;
    private final String openId;

    private SocialAuthAccountId(SocialType socialType, String openId) {
        super(socialType.name() + AuthAccountId.DELIMITER + openId);
        this.socialType = socialType;
        this.openId = openId;
    }

    /**
     * 反序列化入口 — 格式 {@code "O:{社交类型短名}:{openId}"}。
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SocialAuthAccountId of(String value) {
        var suffix = ParseUtils.cutPrefix(value, prefix(AuthAccountType.O));
        var socialParts = ParseUtils.splitPair(suffix, AuthAccountId.DELIMITER);
        var socialType = ParseUtils.parseEnum(SocialType.class, socialParts[0]);
        var openId = socialParts[1];
        // 校验本入口已解析出的局部值（类字段此刻尚未赋值），基类构造器的 hasText 是兜底而非唯一校验点
        ValidateUtils.hasText(openId);
        return new SocialAuthAccountId(socialType, openId);
    }

    /**
     * 从 {@link SocialType} + openId 构造。
     */
    public static SocialAuthAccountId from(SocialType socialType, String openId) {
        ValidateUtils.notNull(socialType);
        ValidateUtils.hasText(openId);
        return new SocialAuthAccountId(socialType, openId);
    }

    @Override
    public AuthAccountType accountType() {
        return AuthAccountType.O;
    }

    @Override
    public int compareTo(SocialAuthAccountId other) {
        return value().compareTo(other.value());
    }
}
